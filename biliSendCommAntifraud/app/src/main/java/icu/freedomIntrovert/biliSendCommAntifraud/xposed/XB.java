package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.util.Log;
import io.github.libxposed.api.XposedInterface;

/** Used only by injected code. App-only utilities use android.util.Log. */
public final class XB {
    private static final String TAG = "BiliAntifraud";
    private static volatile XposedInterface api;
    static void initialize(XposedInterface value) { api = value; }
    public static void log(String message) {
        XposedInterface value = api;
        if (value != null) value.log(Log.INFO, TAG, message);
        else Log.i(TAG, message);
    }
    public static void error(String message, Throwable failure) {
        XposedInterface value = api;
        if (value != null) value.log(Log.ERROR, TAG, message, failure);
        else Log.e(TAG, message, failure);
    }
}
