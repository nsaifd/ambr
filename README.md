# Ambr

Offline-first Android app that keeps every receipt and warranty a household owns, and hands over proof the moment something breaks.

## Project

- `app/`: Android app, Kotlin + Jetpack Compose, package `app.ambr`, Android 8.0+ (minSdk 26)
- `design/`: design canvas source, tokens (`tokens.json`), Public Sans, Uicons, official logo
- `docs/`: UX blueprint and payment plan

## Build

Needs JDK 17+ and the Android SDK (API 35).

```
./gradlew assembleDebug
```

Icons are Uicons by Flaticon (credited in the app's About screen). Font is Public Sans (SIL OFL).
