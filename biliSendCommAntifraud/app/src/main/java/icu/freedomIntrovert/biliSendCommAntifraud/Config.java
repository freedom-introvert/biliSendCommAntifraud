package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.Context;
import android.content.SharedPreferences;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.ForwardDynamic;

public class Config {
    private static Config instance;
    public static final int SORT_RULER_DATE_DESC = 0;
    public static final int SORT_RULER_DATE_ASC = 1;
    public static final int SORT_RULER_LIKE_DESC = 2;
    public static final int SORT_RULER_REPLY_COUNT_DESC = 4;
    public SharedPreferences sp_config;

    private Config(Context context) {
        sp_config = context.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public synchronized static Config getInstance(Context context){
        if (instance == null){
            instance = new Config(context.getApplicationContext());
        }
        return instance;
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

    public boolean getFilterRulerEnableType1(){
        return sp_config.getBoolean("filter_ruler_enable_type1",true);
    }

    public void setFilterRulerEnableType1(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_type1",enable).apply();
    }

    public boolean getFilterRulerEnableType12(){
        return sp_config.getBoolean("filter_ruler_enable_type12",true);
    }

    public void setFilterRulerEnableType12(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_type12",enable).apply();
    }

    public void setFilterRulerEnableType11(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_type11",enable).apply();
    }

    public boolean getFilterRulerEnableType11(){
        return sp_config.getBoolean("filter_ruler_enable_type11",true);
    }

    public void setFilterRulerEnableType17(boolean enable){
        sp_config.edit().putBoolean("filter_ruler_enable_type17",enable).apply();
    }

    public boolean getFilterRulerEnableType17(){
        return sp_config.getBoolean("filter_ruler_enable_type17",true);
    }

    public String getRandomComments(){
        return sp_config.getString("random_comments","日照香炉生紫烟\n" +
                "遥看瀑布挂前川\n" +
                "飞流直下三千尺\n" +
                "疑是银河落九天\n" +
                "床前明月光\n" +
                "疑是地上霜\n" +
                "举头望明月\n" +
                "低头思故乡\n" +
                "横看成岭侧成峰\n" +
                "远近高低各不同 \n" +
                "不识庐山真面目\n" +
                "只缘身在此山中");
    }

    public void setRandomComments(String comments){
        sp_config.edit().putString("random_comments",comments).apply();
    }

    public ForwardDynamic getForwardDynamic(){
        String forwardDynamicUrl = sp_config.getString("forward_dynamic_url", null);
        String forwardDynamicId = sp_config.getString("forward_dynamic_id", null);
        if (forwardDynamicUrl == null || forwardDynamicId == null){
            return null;
        }
        return new ForwardDynamic(forwardDynamicUrl,forwardDynamicId);
    }

    public void setForwardDynamic(ForwardDynamic forwardDynamic){
        sp_config.getString("forward_dynamic_url", forwardDynamic.forwardDynamicUrl);
        sp_config.getString("forward_dynamic_id", forwardDynamic.forwardDynamicId);
    }

    public boolean get花里胡哨Enable(){
        return sp_config.getBoolean("花里胡哨",true);
    }

    public void set花里胡哨Enable(boolean enable){
        sp_config.edit().putBoolean("花里胡哨",enable).apply();
    }

    public boolean getUseClientCookie(){
        return sp_config.getBoolean("use_client_cookie", true);
    }

    public void setUseClientCookie(boolean enable){
        sp_config.edit().putBoolean("use_client_cookie",enable).apply();
    }

}
