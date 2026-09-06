package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import org.junit.Test;
import static org.junit.Assert.*;

public class CaptureDeduperTest {
    @Test public void remembersRpidUntilEvicted() {
        CaptureDeduper deduper = new CaptureDeduper(2);
        assertTrue(deduper.firstSeen(CaptureDeduper.checkKey(1)));
        assertFalse(deduper.firstSeen(CaptureDeduper.checkKey(1)));
        assertTrue(deduper.firstSeen(CaptureDeduper.checkKey(2)));
        assertTrue(deduper.firstSeen(CaptureDeduper.checkKey(3)));
        assertTrue(deduper.firstSeen(CaptureDeduper.checkKey(1)));
    }

    @Test public void distinguishesSensitiveSendsByMessageIdentity() {
        CaptureDeduper deduper = new CaptureDeduper();
        assertTrue(deduper.firstSeen(CaptureDeduper.sensitiveKey(9, 1, "a")));
        assertFalse(deduper.firstSeen(CaptureDeduper.sensitiveKey(9, 1, "a")));
        assertTrue(deduper.firstSeen(CaptureDeduper.sensitiveKey(9, 1, "b")));
    }
}
