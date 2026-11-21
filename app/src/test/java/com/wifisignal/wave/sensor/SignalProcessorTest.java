package com.wifisignal.wave.sensor;
import org.junit.Test;
import static org.junit.Assert.*;
public final class SignalProcessorTest {
    @Test public void qualityClamps() {
        SignalProcessor p=new SignalProcessor();
        assertEquals(100,p.getSignalQuality(-30));
        assertEquals(100,p.getSignalQuality(-50));
        assertEquals(50,p.getSignalQuality(-75));
        assertEquals(0,p.getSignalQuality(-100));
        assertEquals(0,p.getSignalQuality(-115));
    }
    @Test public void smoothAndReset() {
        SignalProcessor p=new SignalProcessor();
        assertEquals(-80f,p.filterRSSI(-80f),0.0001f);
        float next=p.filterRSSI(-40f);
        assertTrue(next>-80f&&next< -40f);
        p.reset();
        assertEquals(-40f,p.filterRSSI(-40f),0.0001f);
    }
}
