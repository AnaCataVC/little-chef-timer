# Agent Operating Guidelines: Little Chef Timer

This document outlines architectural invariants, development conventions, and operational workflows for AI coding agents working on the **Little Chef Timer** repository.

---

## 1. Project Overview & Tech Stack

- **Platform**: Android Native (Single-Module)
- **Language**: Kotlin 2.1.0 (JVM target 17)
- **UI Framework**: Jetpack Compose (BOM 2024.12.01), Material 3
- **Android Target**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (Android 8.0 Oreo)
- **Build System**: Gradle 8.11.1 (Kotlin DSL), Android Gradle Plugin 8.7.3
- **Testing**: JUnit 4 + org.json for JVM local unit tests (no Android emulator/device required)

---

## 2. Core Architectural Invariants

### Exact Alarm Clock Mechanics
- The countdown uses `AlarmManager.setAlarmClock(AlarmClockInfo, PendingIntent)` rather than standard alarms or foreground services.
- **Why**: `setAlarmClock` guarantees execution in Android Doze mode without requiring the sensitive/restricted `android.permission.SCHEDULE_EXACT_ALARM` permission.
- **Timer Drift Prevention**: Timers are deterministic based on target epoch timestamps (`endAtMillis`), never accumulated elapsed ticks. Always calculate remaining time as `((endAtMillis ?: now) - now).coerceAtLeast(0)`.

### Reactive Preference Observation (Zero Disk Polling)
- External mutations (such as notification dismiss or "Stop" action from `TimerAlarmReceiver`) write directly to `RecipeStore` (`SharedPreferences`).
- Compose UI must observe store mutations reactively via `RecipeStore.registerTimerListener` (`OnSharedPreferenceChangeListener`) bound through `DisposableEffect`.
- NEVER introduce interval loops (`while(true) delay(x)`) that perform repeated disk reads (`store.loadTimer()`). Active tickers should strictly update in-memory UI time (`now = System.currentTimeMillis()`) when a running timer is present.

### Alarm Notification Channel Hash Invariant
- Android `NotificationChannel` audio attributes cannot be updated once created.
- Each selected alarm sound creates a discrete channel identified by `alarm_${soundUri.toString().hashCode()}`, deleting previous obsolete alarm channels via `NotificationManager.deleteNotificationChannel`.

### Zero Flags Rule (Documentation & UI)
- Never use country flag emojis (`🇺🇸`, `🇬🇧`, `🇪🇸`, `🇲🇽`, etc.) in UI pickers, documentation, or release notes. Language options must be represented using native names (e.g. `English`, `Español`) or ISO codes.

### Privacy & SAF File Operations
- Recipe import/export uses Android Storage Access Framework (`CreateDocument` / `OpenDocument` contracts).
- NEVER request coarse or fine external storage permissions (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`).
- All imported payloads must be strictly validated with `recipesFromJson()` before committing to `RecipeStore`.

---

## 3. Directory Layout

```
.
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/littlechef/timer/
│       │   │   ├── MainActivity.kt        # Compose UI and App state host
│       │   │   ├── Recipe.kt              # Models, JSON parser & RecipeStore
│       │   │   └── TimerAlarm.kt          # AlarmManager & Notification dispatch
│       │   └── res/
│       │       ├── values/strings.xml     # English strings (default)
│       │       ├── values-es/strings.xml  # Spanish strings
│       │       └── values-night/          # Dark theme window resources
│       └── test/java/com/littlechef/timer/
│           ├── RecipeJsonTest.kt          # JSON serialization & validation tests
│           └── TimerLogicTest.kt          # Math, rounding, and countdown logic tests
├── docs/
│   ├── architecture.md                    # Deep-dive architecture and component roles
│   └── data-model.md                      # JSON contract specification
├── gradle/                                # Gradle wrapper and version catalogs
├── gradlew / gradlew.bat                  # Gradle CLI wrapper scripts
└── README.md                              # Bilingual developer portfolio documentation
```

---

## 4. Verification & Testing Workflow

Before submitting or committing any modifications, agents must verify project health through the CLI:

### Running Unit Tests (PowerShell / Windows)
```powershell
# Ensure JAVA_HOME points to a JDK 17+ runtime if not globally set
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

### Compiling Debug Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

### Git Hygiene & Secrets
- Never commit `local.properties` (contains absolute local SDK paths).
- Always verify working tree status with `git status` before finishing changes.
