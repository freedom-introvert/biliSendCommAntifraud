package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.Map;
import icu.freedomIntrovert.biliSendCommAntifraud.settings.HookSettings;
import io.github.libxposed.api.XposedInterface;

/** Host-only, immutable snapshots; never publishes account or comment data. */
public final class HookConfig {
    private static volatile Map<String, ?> values = Collections.emptyMap();
    private static SharedPreferences preferences;
    private static final SharedPreferences.OnSharedPreferenceChangeListener listener =
            (prefs, key) -> refresh(prefs);
    private HookConfig() {}

    public static void initialize(XposedInterface api) {
        try {
            preferences = api.getRemotePreferences(HookSettings.GROUP);
            preferences.registerOnSharedPreferenceChangeListener(listener);
            refresh(preferences);
        } catch (RuntimeException e) {
            values = Collections.emptyMap();
            XB.log("event=config_unavailable type=" + e.getClass().getSimpleName());
        }
    }

    private static void refresh(SharedPreferences prefs) {
        try {
            values = Collections.unmodifiableMap(new java.util.HashMap<>(prefs.getAll()));
            XB.log("event=config_loaded published=" + enabled(HookSettings.READY));
        } catch (RuntimeException e) {
            values = Collections.emptyMap();
            XB.log("event=config_read_failed type=" + e.getClass().getSimpleName());
        }
    }

    private static boolean enabled(String key) { return HookSettings.value(values, key, false); }
    public static boolean useClientCookie() { return enabled(HookSettings.READY) && enabled(HookSettings.COOKIE); }
    public static boolean replacePicture() { return enabled(HookSettings.READY) && enabled(HookSettings.PICTURE); }
    public static boolean unfoldPictures() { return enabled(HookSettings.READY) && enabled(HookSettings.FOLD); }
}
