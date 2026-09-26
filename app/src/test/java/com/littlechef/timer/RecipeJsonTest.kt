package com.littlechef.timer

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeJsonTest {

    private val recipes = listOf(
        Recipe(id = "1", title = "Huevo duro", emoji = "🥚", durationSeconds = 600),
        Recipe(id = "2", title = "Arroz", emoji = "🍚", note = "2 tazas de agua por una de arroz.", durationSeconds = 1200),
    )

    @Test
    fun roundTripKeepsEveryField() {
        assertEquals(recipes, recipesFromJson(recipesToJson(recipes)))
    }

    @Test
    fun emptyListRoundTrips() {
        assertEquals(emptyList<Recipe>(), recipesFromJson(recipesToJson(emptyList())))
    }

    @Test
    fun missingIdAndNoteAreFilledIn() {
        val parsed = recipesFromJson("""[{"title":"Té","emoji":"🍵","durationSeconds":240}]""").single()
        assertTrue(parsed.id.isNotBlank())
        assertEquals("", parsed.note)
    }

    @Test
    fun fieldsAreTrimmedAndNoteIsCapped() {
        val longNote = "a".repeat(MAX_NOTE_LENGTH + 50)
        val parsed = recipesFromJson(
            """[{"title":"  Pasta ","emoji":" 🍝 ","note":"$longNote","durationSeconds":600}]"""
        ).single()
        assertEquals("Pasta", parsed.title)
        assertEquals("🍝", parsed.emoji)
        assertEquals(MAX_NOTE_LENGTH, parsed.note.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTitleIsRejected() {
        recipesFromJson("""[{"title":"  ","emoji":"🥚","durationSeconds":60}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationIsRejected() {
        recipesFromJson("""[{"title":"Huevo","emoji":"🥚","durationSeconds":0}]""")
    }

    @Test(expected = JSONException::class)
    fun missingDurationIsRejected() {
        recipesFromJson("""[{"title":"Huevo","emoji":"🥚"}]""")
    }

    @Test(expected = JSONException::class)
    fun nonArrayIsRejected() {
        recipesFromJson("""{"title":"Huevo"}""")
    }
}
