package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.ForwardDynamic;

public class Config {
    private static Config instance;
    public static final int SORT_RULER_DATE_DESC = 0;
    public static final int SORT_RULER_DATE_ASC = 1;
    public static final int SORT_RULER_LIKE_DESC = 2;
    public static final int SORT_RULER_REPLY_COUNT_DESC = 4;
    public SharedPreferences sp;

    private Config(Context context) {
        sp = context.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public synchronized static Config getInstance(Context context){
        if (instance == null){
            instance = new Config(context.getApplicationContext());
        }
        return instance;
    }


    public boolean getRecordeHistory() {
        return sp.getBoolean("recordeHistory",true);
    }

    public void setRecordeHistory(boolean recordeHistory) {
        sp.edit().putBoolean("recordeHistory",recordeHistory).apply();
    }

    public String getCookie() {
        return sp.getString("cookie","");
    }

    public void setCookie(String cookie) {
        sp.edit().putString("cookie",cookie).apply();
    }

    public String getDeputyCookie(){
        return sp.getString("deputy_cookie", "");
    }

    public void setDeputyCookie(String cookie){
        sp.edit().putString("deputy_cookie", cookie).apply();
    }

    public long getWaitTime() {
        return sp.getLong("wait_time",5000);
    }

    public void setWaitTime(long waitTime) {
        sp.edit().putLong("wait_time",waitTime).apply();
    }

    public long getWaitTimeByDanmakuSend() {
        return sp.getLong("wait_time_by_danmaku_sent",20000);
    }

    public void setWaitTimeByDanmakuSend(long waitTimeByDanmakuSend) {
        sp.edit().putLong("wait_time_by_danmaku_sent",waitTimeByDanmakuSend).apply();
    }

    public long getWaitTimeByHasPictures() {
        return sp.getLong("wait_time_by_has_pictures",15000);
    }

    public void setWaitTimeByHasPictures(long waitTimeByHasPictures) {
        sp.edit().putLong("wait_time_by_has_pictures",waitTimeByHasPictures).apply();
    }

    public boolean getEnableRecordeBannedComments() {
        return sp.getBoolean("autoRecorde",true);
    }

    public void setEnableRecordeBannedComments(boolean autoRecorde) {
        sp.edit().putBoolean("autoRecorde",autoRecorde).apply();
    }

    public boolean getRecordeHistoryIsEnable(){
        return sp.getBoolean("recordeHistory",true);
    }

    public void setSortRuler(int sortRuler){
        sp.edit().putInt("sort_ruler",sortRuler).apply();
    }

    public int getSortRuler(){
        return sp.getInt("sort_ruler", SORT_RULER_DATE_DESC);
    }

    public void setFilterRulerEnableNormal(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_normal",enable).apply();
    }

    public boolean getFilterRulerEnableNormal(){
        return sp.getBoolean("filter_ruler_enable_normal", true);
    }

    public void setFilterRulerEnableShadowBan(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_shadow_ban",enable).apply();
    }

    public boolean getFilterRulerEnableShadowBan(){
        return sp.getBoolean("filter_ruler_enable_shadow_ban",true);
    }

    public void setFilterRulerEnableDeleted(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_deleted",enable).apply();
    }

    public boolean getFilterRulerEnableDelete(){
        return sp.getBoolean("filter_ruler_enable_deleted",true);
    }

    public void setFilterRulerEnableOther(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_other",enable).apply();
    }

    public boolean getFilterRulerEnableOther(){
        return sp.getBoolean("filter_ruler_enable_other",true);
    }

    public boolean getFilterRulerEnableType1(){
        return sp.getBoolean("filter_ruler_enable_type1",true);
    }

    public void setFilterRulerEnableType1(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_type1",enable).apply();
    }

    public boolean getFilterRulerEnableType12(){
        return sp.getBoolean("filter_ruler_enable_type12",true);
    }

    public void setFilterRulerEnableType12(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_type12",enable).apply();
    }

    public void setFilterRulerEnableType11(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_type11",enable).apply();
    }

    public boolean getFilterRulerEnableType11(){
        return sp.getBoolean("filter_ruler_enable_type11",true);
    }

    public void setFilterRulerEnableType17(boolean enable){
        sp.edit().putBoolean("filter_ruler_enable_type17",enable).apply();
    }

    public boolean getFilterRulerEnableType17(){
        return sp.getBoolean("filter_ruler_enable_type17",true);
    }

    public String getRandomComments(){
        return sp.getString("random_comments","日照香炉生紫烟\n" +
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
        sp.edit().putString("random_comments",comments).apply();
    }

    public ForwardDynamic getForwardDynamic(){
        String forwardDynamicUrl = sp.getString("forward_dynamic_url", null);
        String forwardDynamicId = sp.getString("forward_dynamic_id", null);
        if (forwardDynamicUrl == null || forwardDynamicId == null){
            return null;
        }
        return new ForwardDynamic(forwardDynamicUrl,forwardDynamicId);
    }

    public void setForwardDynamic(ForwardDynamic forwardDynamic){
        sp.edit().putString("forward_dynamic_url", forwardDynamic.forwardDynamicUrl)
                .putString("forward_dynamic_id", forwardDynamic.forwardDynamicId).apply();
    }

    public boolean get花里胡哨Enable(){
        return sp.getBoolean("花里胡哨",true);
    }

    public void set花里胡哨Enable(boolean enable){
        sp.edit().putBoolean("花里胡哨",enable).apply();
    }

    public boolean getUseClientCookie(){
        return sp.getBoolean("use_client_cookie", false);
    }

    public void setUseClientCookie(boolean enable){
        sp.edit().putBoolean("use_client_cookie",enable).apply();
    }

    public boolean getEnablePostPictureHook() {
        return sp.getBoolean("post_picture_hook", true);
    }

    public void setEnableReplacePostPictureHook(boolean enable) {
        sp.edit().putBoolean("post_picture_hook", enable).apply();
    }
    public boolean getEnableFuckFoldPicturesHook(){
        return sp.getBoolean("fuck_fold_pictures_hook",true);
    }

    public void setEnableFuckFoldPicturesHook(boolean enable){
        sp.edit().putBoolean("fuck_fold_pictures_hook",enable).apply();
    }

    public long getBatchCheckInterval(){
        return sp.getLong("batch_check_interval",0);
    }

    public void setBatchCheckInterval(long interval){
        sp.edit().putLong("batch_check_interval",interval).apply();
    }

    public int getLastCommentLocatorMode(){
        return sp.getInt("last_comment_locator_mode", 0);
    }

    public void setLastCommentLocatorMode(int mode){
        sp.edit().putInt("last_comment_locator_mode",mode).apply();
    }

}
