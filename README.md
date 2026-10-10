<p align="center">
  <img src="icon.png" alt="little-chef-timer Logo" width="120" />
</p>

# Little Chef Timer

[English](README.md) | [Español](README.es.md)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-7B61D9)](https://m3.material.io)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue)](https://developer.android.com/about/versions/15)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Product page: [little-chef-timer.ana-catalina.com](https://little-chef-timer.ana-catalina.com)

---

Little Chef Timer is an offline, resilient Android kitchen timer built with modern Jetpack Compose and Material 3. It allows users to register custom preparations with customizable titles, emojis, cooking notes, and precision durations. The alarm fires accurately even when the device is locked, in Doze mode, or if the application process has been terminated by the operating system.

### Key Features

- **Reliable & Exact Alarms**: Powered by `AlarmManager.setAlarmClock`, leveraging `USE_EXACT_ALARM` (Android 13+) and `SCHEDULE_EXACT_ALARM` (Android 12) with automatic fallback to `setAndAllowWhileIdle`, guaranteeing wakeups in Doze mode without runtime settings prompts.
- **Looping Alarm & Actionable Notifications**: High-importance notifications equipped with `FLAG_INSISTENT` and looping alarm sound until dismissed or confirmed with the **Stop** action.
- **Process Death & Reboot Resilience**: Elapsed and remaining time are derived mathematically against the target completion timestamp (`endAtMillis`), surviving process death and device configuration changes without drift.
- **Reactive UI without Polling**: Replaced disk-based polling loops with `SharedPreferences.OnSharedPreferenceChangeListener` bound through Compose `DisposableEffect`, keeping the main thread free and CPU usage minimal.
- **Customizable Alarm Ringtones**: Native integration with `RingtoneManager` allowing users to select standard system alarm tones. Dynamically creates isolated `NotificationChannel` instances matching the sound signature.
- **Privacy-Friendly Backup & Restore**: Full JSON import/export using the Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) without asking for broad file system permissions.
- **Dynamic Theming & In-App Localization**: Supports Light, Dark, and System theme synchronization with an artisanal lilac/rose palette, paired with on-the-fly English/Spanish localization.

### Architecture Overview

Single-module application architecture designed for low complexity and zero background battery drain:

| Component | Responsibility |
|-----------|----------------|
| `MainActivity.kt` | Root Compose UI host, theme engine, navigation between recipe catalog and active timer view. |
| `Recipe.kt` | Data domain models (`Recipe`, `TimerState`), JSON validation/serialization, and `RecipeStore` preference storage. |
| `TimerAlarm.kt` | System scheduling through `AlarmManager.setAlarmClock`, notification channel provisioning, and intent dispatching. |
| `TimerAlarmReceiver.kt` | BroadcastReceiver handling alarm triggers (`FIRE`) and dismissal events (`STOP`) from system actions. |

For deep-dive architectural diagrams and data contracts, see:
- [docs/architecture.md](docs/architecture.md)
- [docs/data-model.md](docs/data-model.md)

### Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Toolkit**: Jetpack Compose (BOM 2024.12.01), Material 3
- **Build System**: Gradle 8.11.1 (Kotlin DSL), Android Gradle Plugin 8.7.3
- **Target Platform**: Android SDK 35 (Android 15), Min SDK 26 (Android 8.0)
- **Testing**: JUnit 4 + org.json for JVM-isolated unit tests

### Getting Started

#### Prerequisites
- JDK 17 or newer (bundled with Android Studio JBR).
- Android SDK 35 installed.

#### Build & Run via Command Line

Run local JVM unit tests:
```bash
./gradlew testDebugUnitTest
```

*(On Windows PowerShell: `.\gradlew.bat testDebugUnitTest`)*

Assemble debug APK:
```bash
./gradlew assembleDebug
```

The resulting package will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

#### Run via Android Studio
1. Open the project root folder in Android Studio.
2. Allow Gradle sync to complete.
3. Select an active Android emulator or connected device and press **Run 'app'** (`Shift + F10`).

### Key Learnings & Architectural Decisions

1. **Why no Room database?**
   - The dataset represents a compact, flat list of cooking preparations. Storing validated JSON payloads within `SharedPreferences` provides sub-millisecond retrieval, avoids schema migration overhead, and reduces APK footprint.
2. **Why no Foreground Service during countdown?**
   - Foreground services consume user battery and trigger persistent foreground notifications. Since countdown timers are deterministic based on target timestamps (`now - endAtMillis`), the system's native `AlarmClock` handles wakeups at exact intervals with zero CPU cycles spent while idle.
3. **Reactive state synchronization over continuous polling:**
   - Instead of polling storage on an interval ticker to check for external notification actions, an `OnSharedPreferenceChangeListener` automatically updates Compose state only when mutations occur, restricting active tickers solely to in-memory rendering.
4. **Exact Alarms permission handling on Android 12-15:**
   - Android 12+ requires exact alarm permissions for `setAlarmClock()`. Declaring `USE_EXACT_ALARM` allows timer and clock apps to receive exact alarm capabilities automatically upon install without intrusive runtime settings redirects. Pairing this with `canScheduleExactAlarms()` verification and fallback to `setAndAllowWhileIdle()` prevents unhandled `SecurityException` crashes across diverse OEM Android variants.

---

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

