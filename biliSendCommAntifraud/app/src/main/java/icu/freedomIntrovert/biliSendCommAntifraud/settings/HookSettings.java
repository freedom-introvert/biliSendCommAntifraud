package icu.freedomIntrovert.biliSendCommAntifraud.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/** Only these non-sensitive settings may cross into host processes. */
public final class HookSettings {
    public static final String GROUP = "hook_settings";
    public static final String READY = "published_v1";
    public static final String COOKIE = "use_client_cookie";
    public static final String PICTURE = "post_picture_hook";
    public static final String FOLD = "fuck_fold_pictures_hook";

    private HookSettings() {}

    public static boolean isSharedKey(String key) {
        return COOKIE.equals(key) || PICTURE.equals(key) || FOLD.equals(key);
    }

    public static Map<String, Boolean> snapshot(Map<String, ?> local) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put(COOKIE, value(local, COOKIE, false));
        result.put(PICTURE, value(local, PICTURE, true));
        result.put(FOLD, value(local, FOLD, true));
        return result;
    }

    public static boolean value(Map<String, ?> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
}
