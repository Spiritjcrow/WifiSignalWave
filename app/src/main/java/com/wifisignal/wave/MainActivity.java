package com.wifisignal.wave;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.wifisignal.wave.renderer.SignalRenderer;
import com.wifisignal.wave.sensor.SignalProcessor;
import com.wifisignal.wave.sensor.WiFiScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends AppCompatActivity {
    private static final int PERM_REQ=1001;
    private final Handler ticker=new Handler(Looper.getMainLooper());
    private GLSurfaceView surface;
    private SignalRenderer renderer;
    private WiFiScanner scanner;
    private final SignalProcessor processor=new SignalProcessor();
    private TextView network,rssi,quality,status;
    private Button button;
    private boolean monitoring=false;
    private float lastX,lastY;

    private final Runnable frameTick=new Runnable(){
        @Override public void run(){
            if(!monitoring)return;
            surface.requestRender();
            ticker.postDelayed(this,33L);
        }
    };

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        scanner=new WiFiScanner(getApplicationContext());
        buildUi();
        addGestures();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String value,int color,float size){
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }
    private void buildUi(){
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(0xFF0A0A1A);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(3);
        renderer=new SignalRenderer();
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18),dp(32),dp(18),dp(12));
        panel.setBackgroundColor(0xA60A0A1A);
        network=label("WiFi Signal Wave",0xFF00FF88,22f);
        rssi=label("RSSI: -- dBm",0xFFEAEAEA,16f);
        quality=label("Estimated quality: --%",0xFFEAEAEA,16f);
        status=label("Connected Wi-Fi only; no nearby network discovery",0xFFB8C9D8,12f);
        button=new Button(this);
        button.setText("START MONITORING");
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundTintList(ColorStateList.valueOf(0xFF0066CC));
        button.setOnClickListener(v->{
            if(monitoring)stopMonitoring();
            else if(hasPermissions())startMonitoring();
            else askPermissions();
        });
        panel.addView(network);panel.addView(rssi);panel.addView(quality);
        panel.addView(status);panel.addView(button);
        root.addView(panel,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        setContentView(root);
    }
    private void addGestures(){
        ScaleGestureDetector pinch=new ScaleGestureDetector(this,
            new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(ScaleGestureDetector d){
                    float factor=d.getScaleFactor();
                    surface.queueEvent(()->renderer.setZoom(factor));
                    surface.requestRender();
                    return true;
                }
            });
        surface.setOnTouchListener((view,event)->{
            pinch.onTouchEvent(event);
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    lastX=event.getX();lastY=event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(event.getPointerCount()==1&&!pinch.isInProgress()){
                        float dx=event.getX()-lastX,dy=event.getY()-lastY;
                        surface.queueEvent(()->renderer.rotate(dx,dy));
                        surface.requestRender();
                    }
                    lastX=event.getX();lastY=event.getY();
                    break;
                default:break;
            }
            return true;
        });
    }
    private boolean hasPermissions(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED)return false;
        return Build.VERSION.SDK_INT<33 ||
            ContextCompat.checkSelfPermission(this,Manifest.permission.NEARBY_WIFI_DEVICES)
                ==PackageManager.PERMISSION_GRANTED;
    }
    private void askPermissions(){
        List<String> missing=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(this,Manifest.permission.NEARBY_WIFI_DEVICES)
                !=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        status.setText("Wi-Fi permissions needed; system Location may also be required.");
        ActivityCompat.requestPermissions(this,missing.toArray(new String[0]),PERM_REQ);
    }
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] results){
        super.onRequestPermissionsResult(request,names,results);
        if(request!=PERM_REQ)return;
        if(hasPermissions())startMonitoring();
        else status.setText("Permission denied: monitoring unavailable.");
    }
    private void startMonitoring(){
        if(monitoring)return;
        monitoring=true;processor.reset();
        button.setText("STOP MONITORING");
        button.setBackgroundTintList(ColorStateList.valueOf(0xFFCC3300));
        status.setText("Reading the connected Wi-Fi");
        scanner.startScan(new WiFiScanner.ScanCallback(){
            @Override public void onWiFiData(String ssid,int power,int frequency,int link){
                runOnUiThread(()->{
                    if(!monitoring)return;
                    float avg=processor.filterRSSI(power);
                    int q=processor.getSignalQuality(avg);
                    network.setText(ssid);
                    rssi.setText(String.format(Locale.getDefault(),"RSSI: %.0f dBm",avg));
                    quality.setText(String.format(Locale.getDefault(),"Estimated quality: %d%%",q));
                    status.setText(String.format(Locale.getDefault(),
                        "Frequency: %d MHz   Link: %d Mbps",frequency,link));
                    surface.queueEvent(()->renderer.updateSignalData(q/100f,frequency));
                    surface.requestRender();
                });
            }
            @Override public void onError(String message){
                runOnUiThread(()->{
                    if(!monitoring)return;
                    network.setText("Wi-Fi signal unavailable");
                    rssi.setText("RSSI: -- dBm");
                    quality.setText("Estimated quality: --%");
                    status.setText(message);
                    processor.reset();
                    surface.queueEvent(()->renderer.updateSignalData(0f,2412));
                    surface.requestRender();
                });
            }
        });
        ticker.removeCallbacks(frameTick);
        ticker.post(frameTick);
    }
    private void stopMonitoring(){
        monitoring=false;
        ticker.removeCallbacks(frameTick);
        scanner.stopScan();
        button.setText("START MONITORING");
        button.setBackgroundTintList(ColorStateList.valueOf(0xFF0066CC));
        status.setText("Monitoring stopped");
    }
    @Override protected void onPause(){
        if(monitoring)stopMonitoring();
        surface.onPause();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        surface.onResume();
        surface.requestRender();
    }
    @Override protected void onDestroy(){
        ticker.removeCallbacks(frameTick);
        scanner.stopScan();
        super.onDestroy();
    }
}
