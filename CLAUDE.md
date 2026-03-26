# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ESNMessenger is an Android messaging app built with Kotlin, Jetpack Compose, and Firebase. The project is in early stages — the initial Kotlin/Firebase scaffold is in place but core features are not yet implemented.

- **Package:** `com.example.esnmessenger`
- **Min SDK:** 24 | **Target/Compile SDK:** 36
- **Build system:** Gradle with version catalog (`gradle/libs.versions.toml`)

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.esnmessenger.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture & Key Technologies

- **UI:** Jetpack Compose + Material3. All UI should be written as `@Composable` functions.
- **Theme:** Defined in `app/src/main/java/com/example/esnmessenger/ui/theme/` — `Color.kt`, `Theme.kt`, `Type.kt`. The root theme is `ESNMessengerTheme`.
- **Firebase:** Integrated via `firebase-bom:34.10.0`. `google-services.json` is in `app/`. Currently only `firebase-analytics` is added; add other Firebase products (Auth, Firestore, etc.) as needed via the BOM.
- **Entry point:** `MainActivity.kt` — single activity, sets Compose content via `setContent { ESNMessengerTheme { ... } }`.

## Adding Firebase Services

Add dependencies in `app/build.gradle.kts` without specifying versions (BOM manages them):
```kotlin
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
```
