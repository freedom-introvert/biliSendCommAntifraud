package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded in-process dedup. Identity is rpid (or oid/type/code when rpid is absent). */
public final class CaptureDeduper {
    private final int limit;
    private final LinkedHashMap<String, Boolean> seen;

    public static final CaptureDeduper SHARED = new CaptureDeduper();

    public CaptureDeduper() {
        this(64);
    }

    public CaptureDeduper(int limit) {
        this.limit = Math.max(1, limit);
        this.seen = new LinkedHashMap<String, Boolean>(this.limit + 8, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > CaptureDeduper.this.limit;
            }
        };
    }

    public synchronized boolean firstSeen(String key) {
        if (key == null || key.isEmpty()) return true;
        if (seen.containsKey(key)) return false;
        seen.put(key, Boolean.TRUE);
        return true;
    }

    public static String checkKey(long rpid) {
        return "c:" + rpid;
    }

    public static String sensitiveKey(long oid, int type) {
        return "s:" + oid + ":" + type;
    }
    public static String sensitiveKey(long oid, int type, String message) {
        return "s:" + oid + ":" + type + ":" + (message == null ? 0 : message.hashCode());
    }
}
