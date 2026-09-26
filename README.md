# Little Chef Timer 👩‍🍳

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-7B61D9)](https://m3.material.io)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue)](https://developer.android.com/about/versions/15)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[English](#english) | [Español](#español)

---

## English

Little Chef Timer is an offline, resilient Android kitchen timer built with modern Jetpack Compose and Material 3. It allows users to register custom preparations with customizable titles, emojis, cooking notes, and precision durations. The alarm fires accurately even when the device is locked, in Doze mode, or if the application process has been terminated by the operating system.

### Key Features

- **Reliable & Exact Alarms**: Powered by `AlarmManager.setAlarmClock`, ensuring notifications wake up the device on time without requiring invasive background runtime permissions (`SCHEDULE_EXACT_ALARM`).
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

---

## Español

Little Chef Timer es un temporizador de cocina para Android, offline y confiable, desarrollado con Jetpack Compose y Material 3. Permite guardar preparaciones culinarias personalizadas con títulos, emojis, notas de cocción y tiempos exactos. La alarma suena puntualmente incluso con la pantalla bloqueada, en modo Doze o si el sistema operativo cerró el proceso de la aplicación en segundo plano.

### Características Principales

- **Alarmas Exactas y Confiables**: Basado en `AlarmManager.setAlarmClock`, asegurando que la campana suene a tiempo sin requerir permisos especiales invasivos (`SCHEDULE_EXACT_ALARM`).
- **Alarma Cíclica y Notificaciones Interactivas**: Notificaciones de alta prioridad con la bandera `FLAG_INSISTENT` y sonido en bucle hasta que el usuario confirme o presione la acción **Detener**.
- **Resistencia al Cierre de Procesos y Reinicios**: El tiempo transcurrido y restante se calcula matemáticamente respecto a la marca de fin (`endAtMillis`), sobreviviendo cierres de proceso sin desfase de tiempo.
- **Interfaz Reactiva sin Polling Continuo**: Reemplazo de bucles de lectura a disco por `SharedPreferences.OnSharedPreferenceChangeListener` acoplado mediante `DisposableEffect` en Compose, reduciendo drásticamente el consumo de CPU.
- **Tonos de Alarma Configurables**: Selección nativa con `RingtoneManager` y creación automática de canales de notificación independientes adaptados al sonido elegido.
- **Respaldo y Restauración Seguro**: Exportación e importación completa de recetas en formato JSON mediante Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) sin pedir permisos amplios de almacenamiento.
- **Tema y Bilingüismo en Vivo**: Soporte para temas Claro, Oscuro y Seguir el sistema con una paleta lila/rosada, además de selección de idioma (Español / English) en tiempo real.

### Estructura de Componentes

| Componente | Responsabilidad |
|------------|-----------------|
| `MainActivity.kt` | Contenedor principal de Compose UI, gestión de tema y navegación entre catálogo y temporizador activo. |
| `Recipe.kt` | Modelos de datos (`Recipe`, `TimerState`), serialización/validación JSON y persistencia en `RecipeStore`. |
| `TimerAlarm.kt` | Programación en el sistema con `AlarmManager.setAlarmClock`, gestión de canales y disparo de intents. |
| `TimerAlarmReceiver.kt` | BroadcastReceiver que procesa el disparo de la alarma (`FIRE`) o su cancelación (`STOP`) desde las notificaciones. |

Documentación técnica complementaria:
- [docs/architecture.md](docs/architecture.md)
- [docs/data-model.md](docs/data-model.md)

### Stack Tecnológico

- **Lenguaje**: Kotlin 2.1.0
- **Interfaz**: Jetpack Compose (BOM 2024.12.01), Material 3
- **Construcción**: Gradle 8.11.1 (Kotlin DSL), Android Gradle Plugin 8.7.3
- **Compatibilidad**: Android SDK 35 (Android 15), SDK mínimo 26 (Android 8.0)
- **Pruebas**: JUnit 4 + org.json para pruebas unitarias en JVM

### Compilación y Ejecución

#### Prerrequisitos
- JDK 17 o superior (incluido en Android Studio JBR).
- Android SDK 35 configurado.

#### Comandos de Terminal

Ejecutar pruebas unitarias en JVM:
```bash
./gradlew testDebugUnitTest
```

*(En Windows PowerShell: `.\gradlew.bat testDebugUnitTest`)*

Compilar APK de depuración:
```bash
./gradlew assembleDebug
```

El binario resultante se generará en: `app/build/outputs/apk/debug/app-debug.apk`.

### Decisiones de Arquitectura y Aprendizajes

1. **¿Por qué no usar Room?**
   - Para una lista plana y liviana de recetas, almacenar un archivo JSON validado en `SharedPreferences` ofrece lectura inmediata, elimina la complejidad de migraciones de esquemas relacionales y mantiene la aplicación ligera.
2. **¿Por qué evitar Foreground Services en la cuenta regresiva?**
   - Los servicios en primer plano consumen batería y obligan a mostrar notificaciones persistentes. Al calcular la cuenta regresiva como una resta respecto al tiempo objetivo (`now - endAtMillis`), `AlarmClock` garantiza el despertar exacto sin gastar recursos mientras el teléfono reposa.
3. **Sincronización reactiva frente a lecturas continuas:**
   - La UI reacciona a través de eventos con `OnSharedPreferenceChangeListener`, restringiendo el consumo de reloj a actualizaciones en memoria únicamente cuando el temporizador está visible y corriendo.
