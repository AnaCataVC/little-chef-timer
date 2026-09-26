package com.littlechef.timer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val lightChefColors = lightColorScheme(
    primary = Color(0xFF7B61D9),
    secondary = Color(0xFFF08BC0),
    background = Color(0xFFF7F4FF),
    surface = Color(0xFFFFFFFF),
)

private val darkChefColors = darkColorScheme(
    primary = Color(0xFFC9B8FF),
    secondary = Color(0xFFF7A8D2),
    background = Color(0xFF1B1726),
    surface = Color(0xFF252033),
)

private val foodEmojis = listOf(
    "🥚", "🍚", "🍝", "🍜", "🥔", "🥕", "🥦", "🌽", "🍅", "🧅",
    "🍗", "🥩", "🐟", "🍤", "🍕", "🍞", "🥐", "🧁", "🍰", "🍪",
    "🍫", "🥧", "🍵", "☕", "🫖", "🍲", "🥘", "🍳", "🥞", "🍿",
)

// Language names are shown in their own language, so they are not translated resources.
private val languages = listOf("en" to "English", "es" to "Español")

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        val store = RecipeStore(this)
        setContent {
            var themeMode by remember { mutableStateOf(store.themeMode) }
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MaterialTheme(colorScheme = if (dark) darkChefColors else lightChefColors) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(
                        store = store,
                        themeMode = themeMode,
                        onThemeChange = { store.themeMode = it; themeMode = it },
                        onLanguageChange = { store.language = it; recreate() },
                    )
                }
            }
        }
    }
}

@Composable
private fun App(
    store: RecipeStore,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    var recipes by remember { mutableStateOf(store.loadRecipes()) }
    var timer by remember { mutableStateOf(store.loadTimer()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // The store is the source of truth: listen reactively to store changes (e.g. dismissed from notification).
    DisposableEffect(store) {
        val listener = store.registerTimerListener {
            timer = store.loadTimer()
        }
        onDispose {
            store.unregisterTimerListener(listener)
        }
    }

    // Keep UI countdown time up-to-date while a timer is active
    LaunchedEffect(timer?.isPaused) {
        while (timer != null && !timer!!.isPaused) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    fun updateTimer(newTimer: TimerState?) {
        store.saveTimer(newTimer)
        timer = newTimer
    }

    fun updateRecipes(newRecipes: List<Recipe>) {
        store.saveRecipes(newRecipes)
        recipes = newRecipes
    }

    val activeTimer = timer
    val activeRecipe = activeTimer?.let { t -> recipes.find { it.id == t.recipeId } }
    if (activeTimer != null && activeRecipe != null) {
        TimerScreen(
            recipe = activeRecipe,
            timer = activeTimer,
            now = now,
            onPauseResume = {
                if (activeTimer.isPaused) {
                    val endAt = System.currentTimeMillis() + activeTimer.remaining(now)
                    TimerAlarm.schedule(context, endAt, activeRecipe.title)
                    updateTimer(activeTimer.copy(endAtMillis = endAt, remainingMillis = null))
                } else {
                    TimerAlarm.cancel(context)
                    updateTimer(activeTimer.copy(endAtMillis = null, remainingMillis = activeTimer.remaining(System.currentTimeMillis())))
                }
            },
            onStop = {
                TimerAlarm.cancel(context)
                TimerAlarm.stopRinging(context)
                timer = null
            },
        )
    } else {
        RecipeListScreen(
            store = store,
            recipes = recipes,
            themeMode = themeMode,
            onThemeChange = onThemeChange,
            onLanguageChange = onLanguageChange,
            onRecipesChange = ::updateRecipes,
            onStart = { recipe ->
                val total = recipe.durationSeconds * 1000L
                val endAt = System.currentTimeMillis() + total
                TimerAlarm.schedule(context, endAt, recipe.title)
                updateTimer(TimerState(recipe.id, total, endAtMillis = endAt))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeListScreen(
    store: RecipeStore,
    recipes: List<Recipe>,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String?) -> Unit,
    onRecipesChange: (List<Recipe>) -> Unit,
    onStart: (Recipe) -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Recipe?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Recipe?>(null) }
    var pendingImport by remember { mutableStateOf<List<Recipe>?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var choosingTheme by remember { mutableStateOf(false) }
    var choosingLanguage by remember { mutableStateOf(false) }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            store.alarmSoundUri = uri?.toString()
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(recipesToJson(recipes).toByteArray()) }
            Toast.makeText(context, R.string.backup_exported, Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                pendingImport = recipesFromJson(json)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.invalid_backup, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${stringResource(R.string.app_name)} 👩‍🍳") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.options)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_sound)) }, onClick = {
                            menuOpen = false
                            soundPicker.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, store.alarmSoundUri?.let(Uri::parse))
                            )
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_theme)) }, onClick = {
                            menuOpen = false
                            choosingTheme = true
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_language)) }, onClick = {
                            menuOpen = false
                            choosingLanguage = true
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_export)) }, onClick = {
                            menuOpen = false
                            exporter.launch("little-chef-backup.json")
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_import)) }, onClick = {
                            menuOpen = false
                            importer.launch(arrayOf("application/json", "text/plain"))
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, stringResource(R.string.new_recipe)) }
        },
    ) { padding ->
        if (recipes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_list))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onStart = { onStart(recipe) },
                    onEdit = { editing = recipe },
                    onDelete = { deleting = recipe },
                )
            }
        }
    }

    if (creating || editing != null) {
        RecipeDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { saved ->
                onRecipesChange(
                    if (editing != null) recipes.map { if (it.id == saved.id) saved else it } else recipes + saved
                )
                creating = false
                editing = null
            },
        )
    }

    deleting?.let { recipe ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_title, "${recipe.emoji} ${recipe.title}")) },
            confirmButton = {
                TextButton(onClick = { onRecipesChange(recipes - recipe); deleting = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_title, imported.size)) },
            text = { Text(stringResource(R.string.import_text)) },
            confirmButton = {
                TextButton(onClick = { onRecipesChange(imported); pendingImport = null }) { Text(stringResource(R.string.import_action)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (choosingTheme) {
        ChoiceDialog(
            title = stringResource(R.string.menu_theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
            ),
            selected = themeMode,
            onSelect = { onThemeChange(it); choosingTheme = false },
            onDismiss = { choosingTheme = false },
        )
    }

    if (choosingLanguage) {
        ChoiceDialog(
            title = stringResource(R.string.menu_language),
            options = listOf<Pair<String?, String>>(null to stringResource(R.string.language_system)) + languages,
            selected = store.language,
            onSelect = { choosingLanguage = false; onLanguageChange(it) },
            onDismiss = { choosingLanguage = false },
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, onClick = { onSelect(value) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RecipeCard(recipe: Recipe, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(recipe.emoji, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(formatDuration(recipe.durationSeconds * 1000L), color = MaterialTheme.colorScheme.primary)
                if (recipe.note.isNotBlank()) {
                    Text(recipe.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onStart) { Text(stringResource(R.string.start)) }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun RecipeDialog(initial: Recipe?, onDismiss: () -> Unit, onSave: (Recipe) -> Unit) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: foodEmojis.first()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var minutes by remember { mutableStateOf(initial?.let { (it.durationSeconds / 60).toString() } ?: "") }
    var seconds by remember { mutableStateOf(initial?.let { (it.durationSeconds % 60).toString() } ?: "0") }
    val totalSeconds = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
    val isValid = title.isNotBlank() && emoji.isNotBlank() && totalSeconds > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_recipe else R.string.edit_recipe)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        emoji, { emoji = it.take(8) },
                        label = { Text(stringResource(R.string.field_emoji)) }, singleLine = true,
                    )
                }
                LazyVerticalGrid(GridCells.Fixed(6), Modifier.height(150.dp)) {
                    items(foodEmojis) { option ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(
                                    if (option == emoji) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else Color.Transparent,
                                    CircleShape,
                                )
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center,
                        ) { Text(option, fontSize = 24.sp) }
                    }
                }
                OutlinedTextField(
                    note, { note = it.take(MAX_NOTE_LENGTH) },
                    label = { Text(stringResource(R.string.field_note)) },
                    supportingText = { Text("${note.length}/$MAX_NOTE_LENGTH") },
                    maxLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        minutes, { minutes = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.field_minutes)) }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        seconds, { seconds = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.field_seconds)) }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = {
                val base = initial ?: Recipe(title = "", emoji = "", durationSeconds = 0)
                onSave(base.copy(title = title.trim(), emoji = emoji.trim(), note = note.trim(), durationSeconds = totalSeconds))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TimerScreen(recipe: Recipe, timer: TimerState, now: Long, onPauseResume: () -> Unit, onStop: () -> Unit) {
    val remaining = timer.remaining(now)
    val finished = !timer.isPaused && remaining == 0L
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(recipe.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (timer.totalMillis > 0) remaining.toFloat() / timer.totalMillis else 0f },
                modifier = Modifier.size(260.dp),
                strokeWidth = 12.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(recipe.emoji, fontSize = 64.sp)
                Text(
                    if (finished) stringResource(R.string.done) else formatDuration(remaining),
                    fontSize = 40.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        if (recipe.note.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(recipe.note)
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!finished) {
                Button(onClick = onPauseResume) {
                    Text(stringResource(if (timer.isPaused) R.string.resume else R.string.pause))
                }
            }
            OutlinedButton(onClick = onStop) { Text(stringResource(if (finished) R.string.stop else R.string.cancel)) }
        }
    }
}

internal fun formatDuration(millis: Long): String {
    // Round up so the display never shows 00:00 while time is still left.
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
