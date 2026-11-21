# WifiSignalWave v1.1.0

Native Android OpenGL ES 3.0 wave visualizer showing RSSI in dBm, filtered *estimated* strength percentage, frequency and link speed for **currently connected Wi-Fi only**. The animated 3D grid is illustrative, not an RF spatial map. It does not detect nearby access points, geolocate devices or upload telemetry.

Grant system Wi-Fi/location permissions if prompted. Tap START MONITORING, drag to rotate, pinch to zoom; STOP MONITORING stops polling and animation. Monitoring stops when app leaves foreground.

## Build routes

- GitHub Actions: .github/workflows/android-debug.yml (JDK17, SDK34, Gradle 8.2.1, JUnit, debug APK, SHA256).
- Manual Codemagic mac_mini_m2: codemagic.yaml (debug only, no app-store publishing).
- Local SDK/JDK17 environment: bash tools/check.sh && bash tools/build-debug-apk.sh. The build script verifies the official Gradle ZIP checksum before executing.

No Gradle-wrapper impostor is used. APK compilation and Android device testing are **PENDING**. See docs/BUILD_VERIFICATION.md. Do not install an unverified or nonexistent APK.
