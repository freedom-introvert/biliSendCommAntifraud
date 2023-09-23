package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    Context context;
    SharedPreferences sp_config;

    public Config(Context context) {
        this.context = context;
        sp_config = context.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public boolean getRecordeHistory() {
        return sp_config.getBoolean("recordeHistory",true);
    }

    public void setRecordeHistory(boolean recordeHistory) {
        sp_config.edit().putBoolean("recordeHistory",recordeHistory).apply();
    }

    public String getCookie() {
        return sp_config.getString("cookie","");
    }

    public void setCookie(String cookie) {
        sp_config.edit().putString("cookie",cookie).apply();
    }

    public long getWaitTime() {
        return sp_config.getLong("wait_time",5000);
    }

    public void setWaitTime(long waitTime) {
        sp_config.edit().putLong("wait_time",waitTime).apply();
    }

    public long getWaitTimeByDanmakuSend() {
        return sp_config.getLong("wait_time_by_danmaku_sent",20000);
    }

    public void setWaitTimeByDanmakuSend(long waitTimeByDanmakuSend) {
        sp_config.edit().putLong("wait_time_by_danmaku_sent",waitTimeByDanmakuSend).apply();
    }

    public long getWaitTimeByHasPictures() {
        return sp_config.getLong("wait_time_by_has_pictures",15000);
    }

    public void setWaitTimeByHasPictures(long waitTimeByHasPictures) {
        sp_config.edit().putLong("wait_time_by_has_pictures",waitTimeByHasPictures).apply();
    }

    public boolean getEnableRecordeBannedComments() {
        return sp_config.getBoolean("autoRecorde",true);
    }

    public void setEnableRecordeBannedComments(boolean autoRecorde) {
        sp_config.edit().putBoolean("autoRecorde",autoRecorde).apply();
    }
}
