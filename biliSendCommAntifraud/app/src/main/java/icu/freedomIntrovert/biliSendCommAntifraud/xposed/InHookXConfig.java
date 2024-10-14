package icu.freedomIntrovert.biliSendCommAntifraud.xposed;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import icu.freedomIntrovert.biliSendCommAntifraud.BuildConfig;

public class InHookXConfig extends XConfig{

    public static final InHookXConfig config = new InHookXConfig(new XSharedPreferences(BuildConfig.APPLICATION_ID, PREF_NAME));
    protected InHookXConfig(XSharedPreferences sharedPreferences) {
        super(sharedPreferences);
        XposedBridge.log(sharedPreferences.getFile().toString());
    }

    public static InHookXConfig getInstance(){
        return config;
    }



}
