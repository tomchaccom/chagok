# Chagok

## Flutter migration (Android-first)

### How to run in Android Studio
1. Open the repository in Android Studio.
2. Open the `chagok_flutter` directory as a Flutter module/project (Android Studio will detect it if the Flutter plugin is installed).
3. Run `flutter pub get` from the `chagok_flutter` directory.
4. Choose an Android device/emulator and run `flutter run`.

### Migrated screens
- Present home entry for the "현재" flow (photo-first entry point).
- "현재" record creation screen with photo selection and basic interactions.
- Photo capture/gallery selection UI (mocked for now).
- Main image selection with duplicate-selection messaging.
- Photo orientation confirmation screen.

### Known limitations
- Backend/API and authentication are not wired yet (see TODOs in Dart code).
- Photo selection uses mocked placeholders instead of device gallery/camera integration.
- State is local to widgets; no persistent storage yet.
