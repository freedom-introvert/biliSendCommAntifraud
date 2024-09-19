package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import de.robv.android.xposed.XposedBridge;

public class XB {
    public static void log(String message){
        XposedBridge.log("[哔哩发评反诈] "+message);
    }
}
