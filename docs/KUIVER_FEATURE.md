# Kuiver Feature

This project now includes a Kuiver-based graph visualization screen in `composeApp`.

## What was added

- Dependency: `io.github.justdeko:kuiver-android`
- Route: `feature/kuiver`
- Screen: `composeApp/src/androidMain/kotlin/com/youtube/rating/android/ui/screens/KuiverGraphScreen.kt`
- Settings entry: `Settings -> About -> Kuiver graf`

## Manual check

1. Open the app and navigate to `Settings`.
2. In the About section, tap `Kuiver graf`.
3. Verify graph renders and these controls work:
   - layout toggle (`Hijerarhijski` / `Force`)
   - zoom in / zoom out
   - center graph

