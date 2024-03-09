package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    public static final int SORT_RULER_DATE_DESC = 0;
    public static final int SORT_RULER_DATE_ASC = 1;
    Context context;
    public SharedPreferences sp_config;

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

    public String getDeputyCookie(){
        return sp_config.getString("deputy_cookie", "");
    }

    public void setDeputyCookie(String cookie){
        sp_config.edit().putString("deputy_cookie", cookie).apply();
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

    public void setRecordeHistoryIsEnable(boolean enable){
        sp_config.edit().putBoolean("recordeHistory", enable).apply();
    }

    public boolean getRecordeHistoryIsEnable(){
        return sp_config.getBoolean("recordeHistory",true);
    }

    public void setSortRuler(int sortRuler){
        sp_config.edit().putInt("sort_ruler",sortRuler).apply();
    }

    public int getSortRuler(){
        return sp_config.getInt("sort_ruler", SORT_RULER_DATE_DESC);
    }

    public void setFilterRulerEnableNormal(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_normal",enable).apply();
    }

    public boolean getFilterRulerEnableNormal(){
        return sp_config.getBoolean("filter_ruler_enable_normal", true);
    }

    public void setFilterRulerEnableShadowBan(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_shadow_ban",enable).apply();
    }

    public boolean getFilterRulerEnableShadowBan(){
        return sp_config.getBoolean("filter_ruler_enable_shadow_ban",true);
    }

    public void setFilterRulerEnableDeleted(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_deleted",enable).apply();
    }

    public boolean getFilterRulerEnableDelete(){
        return sp_config.getBoolean("filter_ruler_enable_deleted",true);
    }

    public void setFilterRulerEnableOther(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_other",enable).apply();
    }

    public boolean getFilterRulerEnableOther(){
        return sp_config.getBoolean("filter_ruler_enable_other",true);
    }

    public boolean get花里胡哨Enable(){
        return sp_config.getBoolean("花里胡哨",false);
    }

    public void set花里胡哨Enable(boolean enable){
        sp_config.edit().putBoolean("花里胡哨",enable).apply();
    }


}
