package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import android.content.SharedPreferences;

public class XConfig {
    public static final String PREF_NAME = "bili_anti_fraud_config";
    private final SharedPreferences sp;

    protected XConfig(SharedPreferences sharedPreferences) {
        this.sp = sharedPreferences;
    }

    public void setHookPictureSelectEnable(boolean enable){
        sp.edit().putBoolean("hook_picture_select",enable).apply();
    }

    public boolean getHookPictureSelectIsEnable(){
        return sp.getBoolean("hook_picture_select", true);
    }

}
