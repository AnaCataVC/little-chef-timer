package com.littlechef.timer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

const val MAX_NOTE_LENGTH = 200

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Returns a context whose resources use the language chosen in the app, if any. */
fun Context.withAppLanguage(): Context {
    val tag = RecipeStore(this).language ?: return this
    val config = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
    return createConfigurationContext(config)
}

data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String,
    val note: String = "",
    val durationSeconds: Int,
)

/** A timer is either running (endAtMillis set) or paused (remainingMillis set). */
data class TimerState(
    val recipeId: String,
    val totalMillis: Long,
    val endAtMillis: Long? = null,
    val remainingMillis: Long? = null,
) {
    val isPaused get() = endAtMillis == null
    fun remaining(now: Long) = remainingMillis ?: ((endAtMillis ?: now) - now).coerceAtLeast(0)
}

private fun exampleRecipes(context: Context) = listOf(
    Recipe(title = context.getString(R.string.example_egg), emoji = "🥚", durationSeconds = 10 * 60),
    Recipe(
        title = context.getString(R.string.example_rice), emoji = "🍚",
        note = context.getString(R.string.example_rice_note), durationSeconds = 20 * 60,
    ),
)

fun recipesToJson(recipes: List<Recipe>): String = JSONArray().apply {
    recipes.forEach {
        put(
            JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("emoji", it.emoji)
                .put("note", it.note)
                .put("durationSeconds", it.durationSeconds)
        )
    }
}.toString(2)

/** Parses and validates a recipe list; throws JSONException or IllegalArgumentException on bad input. */
fun recipesFromJson(json: String): List<Recipe> {
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val recipe = Recipe(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = obj.getString("title").trim(),
            emoji = obj.getString("emoji").trim(),
            note = obj.optString("note").trim().take(MAX_NOTE_LENGTH),
            durationSeconds = obj.getInt("durationSeconds"),
        )
        require(recipe.title.isNotEmpty() && recipe.emoji.isNotEmpty() && recipe.durationSeconds > 0) {
            "Invalid recipe at position ${i + 1}"
        }
        recipe
    }
}

class RecipeStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("little_chef", Context.MODE_PRIVATE)

    fun loadRecipes(): List<Recipe> {
        val json = prefs.getString(KEY_RECIPES, null) ?: return exampleRecipes(context).also(::saveRecipes)
        return recipesFromJson(json)
    }

    fun saveRecipes(recipes: List<Recipe>) = prefs.edit().putString(KEY_RECIPES, recipesToJson(recipes)).apply()

    fun loadTimer(): TimerState? {
        val recipeId = prefs.getString(KEY_TIMER_RECIPE, null) ?: return null
        return TimerState(
            recipeId = recipeId,
            totalMillis = prefs.getLong(KEY_TIMER_TOTAL, 0),
            endAtMillis = prefs.getLong(KEY_TIMER_END, -1).takeIf { it >= 0 },
            remainingMillis = prefs.getLong(KEY_TIMER_REMAINING, -1).takeIf { it >= 0 },
        )
    }

    fun saveTimer(timer: TimerState?) = prefs.edit().apply {
        if (timer == null) {
            remove(KEY_TIMER_RECIPE); remove(KEY_TIMER_TOTAL); remove(KEY_TIMER_END); remove(KEY_TIMER_REMAINING)
        } else {
            putString(KEY_TIMER_RECIPE, timer.recipeId)
            putLong(KEY_TIMER_TOTAL, timer.totalMillis)
            putLong(KEY_TIMER_END, timer.endAtMillis ?: -1)
            putLong(KEY_TIMER_REMAINING, timer.remainingMillis ?: -1)
        }
    }.apply()

    var alarmSoundUri: String?
        get() = prefs.getString(KEY_SOUND, null)
        set(value) = prefs.edit().putString(KEY_SOUND, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.entries.find { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    /** BCP 47 tag ("en", "es"), or null to follow the phone's language. */
    var language: String?
        get() = prefs.getString(KEY_LANGUAGE, null)
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun registerTimerListener(listener: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_TIMER_RECIPE || key == KEY_TIMER_END || key == KEY_TIMER_REMAINING || key == KEY_TIMER_TOTAL) {
                listener()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        return prefListener
    }

    fun unregisterTimerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_RECIPES = "recipes"
        const val KEY_TIMER_RECIPE = "timer_recipe"
        const val KEY_TIMER_TOTAL = "timer_total"
        const val KEY_TIMER_END = "timer_end"
        const val KEY_TIMER_REMAINING = "timer_remaining"
        const val KEY_SOUND = "alarm_sound"
    }
}
