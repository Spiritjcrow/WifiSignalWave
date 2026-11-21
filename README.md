# WifiSignalWave v1.1.0

Android OpenGL ES 3.0 live artistic wave visualization for the phone's **currently connected Wi-Fi** network. Displays RSSI in dBm, a smoothed estimated quality percentage, Wi-Fi frequency and reported link speed.

- **What works:** local-only Wi-Fi metric polling (500 ms), Kalman smoothing, animated 3D wave (about 30 fps while active), touch rotation and pinch zoom.
- **Not a spatial RF scanner:** no surrounding network discovery, positioning, signal direction detection, triangulation, user tracking or remote telemetry.
- **No fees or accounts:** the app needs only local Android permissions. Building through GitHub Actions may be subject to account-level availability or limits.
- **Permissions:** fine location to reveal Wi-Fi identity; nearby Wi-Fi permission also requested on Android 13+. Enable system Location if Android obscures the SSID.
- **Requirements:** Android 8.0+ (API 26), OpenGL ES 3.0-capable phone, Android SDK 34, JDK 17 for building.

## Build

Open a pull request from the source branch to main, or run the 'WifiSignalWave Android Debug' workflow from GitHub Actions after merging. Workflow installs Gradle 8.2.1 and Android SDK 34, then runs unit tests and debug assembly. A successful run uploads **WifiSignalWave-debug** containing **app-debug.apk**. Nothing is labeled verified until the workflow actually succeeds and the APK passes on-device testing.

For local Android Studio: use JDK 17 and Gradle 8.2.1. There is intentionally no fake `gradlew` wrapper.

**Project status:** source staged; APK build and device behavior must be verified with CI/device evidence.
