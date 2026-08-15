# WiFi Signal Wave

Termux-first, Android-controlled archive for real Wi-Fi observations. No generated observations,
mock sensor values, or RSSI-to-direction claims are accepted.

## Install

1. Download the debug APK artifact from the latest successful GitHub Actions run and install it.
2. Install Termux and Termux:API from the same trusted distribution source.
3. In Termux: `cd termux && sh install.sh`, then run `~/.local/bin/wifisignalwave`.
4. Enter the generated pairing token once in the Android app.
5. Tap **Collect real scan**, then **Sync archive to Termux**.

Android may throttle scans or return prior results. Each observation records `resultsUpdated`, UTC
capture time, source API, permission provenance, device API/model, and a SHA-256-redacted BSSID.
RSSI is retained only as a relative dBm observation; it is not bearing or exact distance.

## Historical and calibration policy

The production database accepts only records whose source is `android.wifi.ScanResult`. Historical
archives are real records retained unchanged with content hashes. Published standards tables may be
used as separately labeled frequency/channel reference markers, with source URL, publication date,
retrieval date, and hash. Reference markers never masquerade as measurements and do not calibrate
RSSI without documented external instrumentation.

## Security

The worker binds only to `127.0.0.1`, requires a random 256-bit bearer token, limits requests to
10 MB, uses an allowlist of `/v1/import` and `/v1/status`, and provides no arbitrary shell endpoint.

Official behavior references:

- https://developer.android.com/develop/connectivity/wifi/wifi-scan
- https://developer.android.com/develop/connectivity/wifi/wifi-permissions
- https://github.com/termux/termux-api
- https://github.com/termux/termux-api-package
