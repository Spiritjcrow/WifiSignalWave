package com.spiritjcrow.wifisignalwave;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 7;
    private WifiManager wifi;
    private TextView status;
    private EditText token;
    private File archive;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            boolean updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            persistRealResults(updated);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        archive = new File(getFilesDir(), "observations.jsonl");
        buildUi();
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 48, 32, 32);
        TextView title = new TextView(this); title.setText("WiFi Signal Wave"); title.setTextSize(28);
        TextView policy = new TextView(this);
        policy.setText("Real device observations only. RSSI is relative signal strength, never direction or exact distance. BSSIDs are hashed before storage.");
        token = new EditText(this); token.setHint("Termux pairing token");
        Button scan = button("Collect real scan", v -> requestRealScan());
        Button sync = button("Sync archive to Termux", v -> syncArchive());
        Button verify = button("Create device verification report", v -> createReport());
        Button erase = button("Delete app-owned archive", v -> { if (archive.delete()) status.setText("Archive deleted"); else status.setText("No archive present"); });
        status = new TextView(this); status.setText("Ready"); status.setPadding(0, 24, 0, 0);
        for (View v : new View[]{title, policy, token, scan, sync, verify, erase, status}) root.addView(v);
        setContentView(root);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(text); b.setOnClickListener(listener); return b;
    }

    private void requestRealScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST); return;
        }
        boolean accepted = wifi.startScan();
        status.setText(accepted ? "Scan requested; waiting for Android result broadcast" : "Android rejected scan request (possibly throttled)");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) requestRealScan();
        else status.setText("Location permission is required by Android for Wi-Fi scan results");
    }

    private void persistRealResults(boolean updated) {
        try (FileOutputStream out = new FileOutputStream(archive, true)) {
            int count = 0;
            for (ScanResult r : wifi.getScanResults()) {
                JSONObject o = new JSONObject();
                o.put("schemaVersion", 1); o.put("observationId", UUID.randomUUID().toString());
                o.put("capturedAtUtc", Instant.now().toString()); o.put("source", "android.wifi.ScanResult");
                o.put("resultsUpdated", updated); o.put("ssid", r.SSID == null ? "" : r.SSID);
                o.put("bssidSha256", sha256(r.BSSID == null ? "" : r.BSSID));
                o.put("frequencyMhz", r.frequency); o.put("rssiDbm", r.level);
                o.put("channelWidth", r.channelWidth); o.put("centerFreq0Mhz", r.centerFreq0);
                o.put("deviceApi", android.os.Build.VERSION.SDK_INT); o.put("deviceModel", android.os.Build.MODEL);
                o.put("permission", "ACCESS_FINE_LOCATION:granted");
                o.put("interpretation", "relative signal strength only; no bearing or exact distance");
                out.write((o.toString() + "\n").getBytes(StandardCharsets.UTF_8)); count++;
            }
            status.setText("Archived " + count + " real results; updated=" + updated);
        } catch (Exception e) { status.setText("Collection failed: " + e.getClass().getSimpleName()); }
    }

    private void syncArchive() {
        if (!archive.exists()) { status.setText("Collect a real scan first"); return; }
        String pairingToken = token.getText().toString();
        if (pairingToken.isEmpty()) { status.setText("Enter the Termux pairing token"); return; }
        status.setText("Syncing to authenticated loopback worker…");
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:8765/v1/import").openConnection();
                c.setRequestMethod("POST"); c.setRequestProperty("Authorization", "Bearer " + pairingToken);
                c.setRequestProperty("Content-Type", "application/x-ndjson"); c.setDoOutput(true);
                try (InputStream in = new FileInputStream(archive); OutputStream out = c.getOutputStream()) { in.transferTo(out); }
                int code = c.getResponseCode(); runOnUiThread(() -> status.setText("Termux sync HTTP " + code));
            } catch (Exception e) { runOnUiThread(() -> status.setText("Termux sync failed: " + e.getClass().getSimpleName())); }
        }).start();
    }

    private void createReport() {
        try {
            JSONObject r = new JSONObject(); r.put("schemaVersion", 1); r.put("createdAtUtc", Instant.now().toString());
            r.put("manufacturer", android.os.Build.MANUFACTURER); r.put("model", android.os.Build.MODEL);
            r.put("apiLevel", android.os.Build.VERSION.SDK_INT); r.put("wifiEnabled", wifi.isWifiEnabled());
            r.put("locationEnabled", ((android.location.LocationManager)getSystemService(LOCATION_SERVICE)).isLocationEnabled());
            r.put("fineLocationGranted", checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            r.put("archiveExists", archive.exists()); r.put("archiveBytes", archive.exists() ? archive.length() : 0);
            r.put("androidIdSha256", sha256(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)));
            File report = new File(getFilesDir(), "device-verification.json");
            try (FileWriter w = new FileWriter(report)) { w.write(r.toString(2)); }
            status.setText("Report created in app storage: " + report.getName());
        } catch (Exception e) { status.setText("Report failed: " + e.getClass().getSimpleName()); }
    }

    private static String sha256(String value) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder(); for (byte b : d) s.append(String.format("%02x", b)); return s.toString();
    }

    @Override protected void onDestroy() { unregisterReceiver(receiver); super.onDestroy(); }
}
