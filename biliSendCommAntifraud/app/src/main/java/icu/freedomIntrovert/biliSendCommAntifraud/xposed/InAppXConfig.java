package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.Context;
import android.content.SharedPreferences;

public class InAppXConfig extends XConfig{
    protected InAppXConfig(SharedPreferences sharedPreferences) {
        super(sharedPreferences);
    }
    public static InAppXConfig newInstance(Context context) {
        SharedPreferences pref;
        try {
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            pref = null;
        }
        if (pref != null) {
            return new InAppXConfig(pref);
        }
        return null;
    }

}
