# iOS App Setup - YouTube Ratings

## ✅ What's Been Created

### Kotlin Multiplatform Framework
- **shared.xcframework** - Shared KMP framework containing:
  - RatingApiClient (HTTP client for backend)
  - Realm repositories (Notes, Favorites, OfflineVideos)
  - Logger (NSLog-based logging)
  - All shared models and business logic

### Compose Multiplatform UI Framework
- **composeApp.xcframework** - iOS Compose UI framework containing:
  - `MainViewController()` that hosts Compose UI

### SwiftUI Application Structure
```
iosApp/
├── iosApp.xcodeproj          # Xcode project (generated)
├── Info.plist                # App configuration
├── project.yml               # XcodeGen specification
├── shared.xcframework/       # KMP framework
└── iosApp/
    ├── YouTubeRatingApp.swift    # App entry point
    ├── Views/
    │   ├── ContentView.swift     # Main tab navigation
    │   ├── HomeScreen.swift      # Video list with ratings
    │   ├── FavoritesScreen.swift # Saved favorites
    │   ├── NotesScreen.swift     # Content-only notes
    │   └── SettingsScreen.swift  # App preferences
    └── ViewModels/
        ├── HomeViewModel.swift      # Video list state
        ├── FavoritesViewModel.swift # Favorites state
        └── NotesViewModel.swift     # Notes state
```

## 🔨 Building the XCFramework

The Xcode target runs a pre-build script that rebuilds and syncs the frameworks automatically.

To rebuild manually after Kotlin code changes:

```bash
# Sync both frameworks into iosApp/
./gradlew :shared:syncSharedXCFrameworkToXcode :composeApp:syncComposeAppXCFrameworkToXcode

# Or
./iosApp/build-ios.sh
```

This updates:
- `iosApp/shared.xcframework`
- `iosApp/composeApp.xcframework`

## 🔧 Configuration

### Base URL (iOS)
Edit these keys in `iosApp/iosApp/Info.plist`:
- `YT_BASE_URL`
- `YT_DATA_BASE_URL`

## 🚀 Running the iOS App

### Option 1: Xcode IDE (Recommended)
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select a simulator or device from the destination picker
3. Press ⌘R to build and run

### Option 2: Command Line
```bash
cd iosApp
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build
```

## 📱 App Features

### Home Screen
- Browse YouTube videos with ratings
- Display Love ❤️, Faith 💙, Hope 💚 ratings
- Pull-to-refresh support
- Video thumbnails and metadata

### Favorites Screen
- View saved favorite videos
- Tap heart icon to remove
- Timestamps for when videos were favorited

### Notes Screen
- Content-only notes (no title field)
- Auto-generates title from first line
- Tap to edit existing notes
- Swipe to delete

### Settings Screen
- Toggle logging on/off
- Clear all local data
- About screen with app info
- Version display

### Bundle Identifier
Default: `com.youtuberatings.ios`

To change, edit `iosApp/project.yml`:
```yaml
settings:
  base:
    PRODUCT_BUNDLE_IDENTIFIER: com.yourcompany.app
```

Then regenerate:
```bash
cd iosApp
xcodegen generate
```

### Deployment Target
Minimum iOS version: **15.0**

### Code Signing
Currently set to "Automatic" signing. To use a specific team:
1. Open project in Xcode
2. Select iosApp target
3. Signing & Capabilities tab
4. Select your Team from dropdown

Or edit `project.yml`:
```yaml
DEVELOPMENT_TEAM: "YOUR_TEAM_ID"
```

## 🗄️ Data Persistence

The app uses **Realm Kotlin** for local storage (same as Android):
- Notes stored in `NoteModel` realm
- Favorites stored in `FavoriteVideoDto` realm
- Offline videos stored in `OfflineVideo` realm

All Realm operations are handled by the shared KMP module.

## 🔗 API Integration

Backend URL configured in shared module:
```kotlin
// shared/src/commonMain/.../RatingApiClient.kt
private val baseUrl = "https://your-backend-url.com"
```

The iOS app uses the same API client as Android through the shared framework.

## 📝 Development Workflow

1. **Kotlin Code Changes**
   ```bash
   ./gradlew :shared:assembleSharedXCFramework
   ```

2. **Swift Code Changes**
   - Edit files in `iosApp/iosApp/`
   - Build in Xcode (⌘B)

3. **Project Structure Changes**
   - Edit `iosApp/project.yml`
   - Run `xcodegen generate`

## 🐛 Troubleshooting

### "No such module 'shared'" error
- Ensure XCFramework is built: `./gradlew :shared:assembleSharedXCFramework`
- Clean build folder in Xcode: Product → Clean Build Folder (⇧⌘K)
- Check framework is embedded: Target → Frameworks, Libraries, and Embedded Content

### Simulator not found
- Download required iOS SDK: Xcode → Settings → Platforms
- Or use command: `xcodebuild -downloadPlatform iOS`

### Realm initialization error
- Check that `RealmProvider.doInitialize()` is called in app startup
- Verify framework exports Realm: `io.realm.kotlin:library-base`

## 📦 Release Build

To create a release build:

```bash
cd iosApp
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -archivePath ./build/iosApp.xcarchive \
  archive
```

Then export IPA from the archive in Xcode Organizer.

## 🆚 iOS vs Android Feature Parity

| Feature | Android | iOS |
|---------|---------|-----|
| Video List | ✅ | ✅ |
| Video Ratings | ✅ | ✅ |
| Favorites | ✅ | ✅ |
| Notes (content-only) | ✅ | ✅ |
| Offline Storage | ✅ | ✅ |
| Logger | ✅ | ✅ |
| Data Migration | ✅ | ✅ |
| Pagination | ✅ | 🔄 (TODO) |

## 📚 Tech Stack

- **Language**: Swift 5.0
- **UI Framework**: SwiftUI
- **Shared Logic**: Kotlin Multiplatform
- **Database**: Realm Kotlin (exported to iOS)
- **Networking**: Ktor Client (Darwin engine)
- **Project Generator**: XcodeGen 2.44.1

## 🎯 Next Steps

1. **Test on Device/Simulator** - Run app and verify all features work
2. **Add Pagination** - Implement infinite scroll for video list
3. **Video Detail Screen** - Add screen to submit ratings
4. **Search Functionality** - Add video search
5. **App Icon** - Add icon to Assets.xcassets
6. **Launch Screen** - Customize launch screen
7. **Push Notifications** - If backend supports it
8. **App Store Submission** - Configure for release

---

**Created**: January 17, 2026
**iOS Deployment Target**: 15.0+
**Framework**: Kotlin Multiplatform + SwiftUI
