package icu.freedomIntrovert.biliSendCommAntifraud.settings;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class HookSettingsTest {
    @Test public void publishesOnlyThreeSwitchesNeverCredentials() {
        Map<String, Object> local = new HashMap<>();
        local.put("cookie", "private-test-cookie");
        local.put("deputy_cookie", "private-test-cookie-2");
        local.put("random_comments", "private text");
        local.put(HookSettings.COOKIE, true);
        local.put(HookSettings.FOLD, false);
        Map<String, Boolean> remote = HookSettings.snapshot(local);
        assertEquals(3, remote.size());
        assertEquals(Boolean.TRUE, remote.get(HookSettings.COOKIE));
        assertEquals(Boolean.FALSE, remote.get(HookSettings.FOLD));
        assertFalse(remote.containsKey("cookie"));
        assertFalse(remote.containsKey("deputy_cookie"));
        assertFalse(remote.containsKey("random_comments"));
    }

    @Test public void rejectsWrongTypesAndPreservesDefaults() {
        Map<String, Object> local = new HashMap<>();
        local.put(HookSettings.COOKIE, "true");
        local.put(HookSettings.PICTURE, false);
        Map<String, Boolean> remote = HookSettings.snapshot(local);
        assertEquals(Boolean.FALSE, remote.get(HookSettings.COOKIE));
        assertEquals(Boolean.FALSE, remote.get(HookSettings.PICTURE));
        assertEquals(Boolean.TRUE, remote.get(HookSettings.FOLD));
        assertFalse(HookSettings.isSharedKey("cookie"));
        assertFalse(HookSettings.isSharedKey(null));
    }
}
