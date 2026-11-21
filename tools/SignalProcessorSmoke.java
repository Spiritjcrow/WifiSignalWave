import com.wifisignal.wave.sensor.SignalProcessor;
public final class SignalProcessorSmoke {
    public static void main(String[] args) {
        SignalProcessor p = new SignalProcessor();
        if (p.getSignalQuality(-50) != 100 || p.getSignalQuality(-75) != 50 ||
                p.getSignalQuality(-100) != 0) throw new AssertionError("Quality mapping failed");
        if (p.filterRSSI(-80) != -80f) throw new AssertionError("Initial estimate failed");
        float filtered = p.filterRSSI(-40);
        if (!(filtered > -80 && filtered < -40)) throw new AssertionError("RSSI filter failed");
        p.reset();
        if (p.filterRSSI(-40) != -40f) throw new AssertionError("Reset failed");
        System.out.println("PASS: SignalProcessor quality, smoothing and reset");
    }
}
