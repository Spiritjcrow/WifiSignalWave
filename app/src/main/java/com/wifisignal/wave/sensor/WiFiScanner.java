package com.wifisignal.wave.sensor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Reports metrics for the connected network only. Does not discover nearby access points. */
public final class WiFiScanner {
    public interface ScanCallback {
        void onWiFiData(String ssid, int rssi, int frequency, int linkSpeed);
        void onError(String message);
    }

    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanCallback callback;
    private Runnable poller;
    private boolean running;

    public WiFiScanner(Context context) {
        Context app = context.getApplicationContext();
        wifiManager = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void startScan(ScanCallback newCallback) {
        stopScan();
        callback = newCallback;
        running = true;
        poller = new Runnable() {
            @Override public void run() {
                if (!running) return;
                pollConnectedNetwork();
                handler.postDelayed(this, 500L);
            }
        };
        handler.post(poller);
    }

    @SuppressWarnings("deprecation")
    private void pollConnectedNetwork() {
        try {
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                reportError("Wi-Fi is disabled");
                return;
            }
            WifiInfo info = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivityManager != null) {
                Network active = connectivityManager.getActiveNetwork();
                NetworkCapabilities capabilities = active == null ? null
                        : connectivityManager.getNetworkCapabilities(active);
                if (capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                        capabilities.getTransportInfo() instanceof WifiInfo) {
                    info = (WifiInfo) capabilities.getTransportInfo();
                }
            }
            if (info == null) info = wifiManager.getConnectionInfo();
            if (info == null || info.getRssi() <= -127 || info.getRssi() > 0) {
                reportError("No readable Wi-Fi connection; check Wi-Fi and Location.");
                return;
            }
            String ssid = info.getSSID();
            if (ssid != null && ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\""))
                ssid = ssid.substring(1, ssid.length() - 1);
            if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equalsIgnoreCase(ssid))
                ssid = "Connected Wi-Fi";
            if (running && callback != null)
                callback.onWiFiData(ssid, info.getRssi(), info.getFrequency(), info.getLinkSpeed());
        } catch (SecurityException e) {
            reportError("Wi-Fi permission blocked; check permissions and Location.");
        } catch (RuntimeException e) {
            reportError("Wi-Fi data unavailable on this device.");
        }
    }

    private void reportError(String message) {
        if (running && callback != null) callback.onError(message);
    }

    public void stopScan() {
        running = false;
        if (poller != null) handler.removeCallbacks(poller);
        poller = null;
        callback = null;
    }
}
