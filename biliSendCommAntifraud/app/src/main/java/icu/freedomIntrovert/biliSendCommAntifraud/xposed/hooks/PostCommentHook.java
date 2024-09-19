package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

public abstract class PostCommentHook extends BaseHook {
    Activity currentActivity;
    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                currentActivity = (Activity) param.thisObject;
            }
        });


        XposedHelpers.findAndHookMethod(getBiliCallClassName()/*com.bilibili.okretro.call.BiliCall 混淆*/, classLoader, "execute", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object arg = param.getResult();
                if (arg == null){
                    return;
                }
                Object body = XposedHelpers.callMethod(arg, getBiliCall_body_MethodName()/* body 混淆*/);
                if (body == null) {
                    return;
                }

                String bodyCanonicalName = body.getClass().getCanonicalName();
                if (!(bodyCanonicalName != null && bodyCanonicalName.equals("com.bilibili.okretro.GeneralResponse"))) {
                    return;
                }
                Object data = XposedHelpers.getObjectField(body, "data");
                Object request = XposedHelpers.callMethod(param.thisObject, getBiliCall_request_MethodName()/*request 混淆*/);
                if (data != null && "com.bilibili.app.comm.comment2.model.BiliCommentAddResult".equals(data.getClass().getCanonicalName())) {
                    Bundle extras = new Bundle();
                    Class<?> biliCommentAddResultClass = data.getClass();
                    Object reply = XposedHelpers.getObjectField(data, "reply");
                    Object content = XposedHelpers.getObjectField(reply, "mContent");
                    Integer type = (Integer) XposedHelpers.getObjectField(reply, "mType");
                    Long oid = (Long) XposedHelpers.getObjectField(reply, "mOid");
                    if (!((Integer) biliCommentAddResultClass.getField("action").get(data) == 0)) {
                        return;
                    }
                    extras.putInt("action", ByXposedLaunchedActivity.ACTION_CHECK_COMMENT);
                    extras.putString("toast_message", (String) biliCommentAddResultClass.getField("message").get(data));
                    extras.putLong("oid", oid);
                    extras.putInt("type", type);
                    extras.putLong("rpid", XposedHelpers.getLongField(data, "rpid"));
                    extras.putLong("root", XposedHelpers.getLongField(data, "root"));
                    extras.putLong("parent", XposedHelpers.getLongField(data, "parent"));
                    extras.putString("comment_text", (String) XposedHelpers.getObjectField(content, "mMsg"));
                    extras.putString("source_id", tryGetSourceId(type, oid));
                    extras.putLong("uid", XposedHelpers.getLongField(reply, "mMid"));
                    try {
                        Field picturesField = content.getClass().getField("pictures");
                        List<?> pictures = (List<?>) picturesField.get(content);
                        extras.putString("pictures", Utils.picturesObjToString(pictures));
                    } catch (NoSuchFieldException e) {
                        XposedBridge.log("当前哔哩哔哩版本不支持发送图片");
                    }
                    long ctime = XposedHelpers.getLongField(reply, "mCtime");
                    extras.putLong("ctime", ctime);
                    ArrayList<String> cookies = new ArrayList<>();
                    for (String cookieDBFilePath : getCookieDBFilePaths()) {
                        String cookie = getCookiesAsString(cookieDBFilePath);
                        if (cookie.contains("SESSDATA") && cookie.contains("buvid3")){
                            cookies.add(cookie);
                        }
                    }
                    extras.putStringArrayList("cookies",cookies);
                    //extras.putString("cookie",getCookiesAsString("/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili/Default/Cookies"));
                    Utils.startActivity(currentActivity, extras);
                } else if (XposedHelpers.getIntField(body, "code") == GeneralResponse.CODE_COMMENT_CONTAIN_SENSITIVE) {
                    Object requestBody = XposedHelpers.callMethod(request, "body"/*混淆注意*/);
                    Map<String, String> requsetMap = new HashMap<>();
                    for (int i = 0; i < (Integer) XposedHelpers.callMethod(requestBody, "size"); i++) {
                        String name = (String) XposedHelpers.callMethod(requestBody, "name");
                        String value = (String) XposedHelpers.callMethod(requestBody, "value");
                        requsetMap.put(name, value);
                    }

                    Bundle extras = new Bundle();
                    extras.putInt("action", ByXposedLaunchedActivity.ACTION_SAVE_CONTAIN_SENSITIVE_CONTENT);
                    int oid = Integer.parseInt(Objects.requireNonNull(requsetMap.get("oid")));
                    int type = Integer.parseInt(Objects.requireNonNull(requsetMap.get("type")));
                    extras.putLong("oid", oid);
                    extras.putInt("type", type);
                    extras.putString("source_id", tryGetSourceId(type, oid));
                    extras.putString("comment_text", requsetMap.get("message"));
                    extras.putString("toast_message", (String) XposedHelpers.getObjectField(body, "message"));
                    Utils.startActivity(currentActivity, extras);
                }
            }
        });
        hook(appVersionCode, classLoader);
    }

    public abstract void hook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException;

    protected abstract String getBiliCallClassName();
    protected abstract String getBiliCall_body_MethodName();
    protected abstract String getBiliCall_request_MethodName();

    protected abstract String[] getCookieDBFilePaths();

    protected String tryGetSourceId(int type, long oid) throws ExecutionException, InterruptedException {
        switch (type) {
            case CommentArea.AREA_TYPE_VIDEO:
                return Utils.getBvidFormAvid(oid);
            case CommentArea.AREA_TYPE_ARTICLE:
                return "cv" + oid;
            case CommentArea.AREA_TYPE_DYNAMIC17:
                return String.valueOf(oid);
            case CommentArea.AREA_TYPE_DYNAMIC11:
                return getDynamic11ID(currentActivity);
            default:
                String msg = "不支持的评论区类型：" + type + "，无法获取源ID，请报告哔哩发评反诈开发者！";
                XB.log(msg);
                Toast.makeText(currentActivity, msg, Toast.LENGTH_SHORT).show();
                return null;
        }
    }

    public static String getDynamic11ID(Activity activity) {
        String activityName = activity.getClass().getCanonicalName();
        Bundle extras = activity.getIntent().getExtras();
        if (extras == null) {
            return null;
        }
        String id;
        if ("com.bilibili.lib.ui.ComposeActivity".equals(activityName)) {
            Bundle fragmentArgs = extras.getBundle("fragment_args");
            if (fragmentArgs == null) {
                return null;
            }
            id = fragmentArgs.getString("oid");
            XB.log("动态ID:" + id);
            return id;
        }
        //信息箱打开评论详情页的情况
        if ("com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity".equals(activityName)
                || "com.bilibili.app.comm.comment2.comments.view.CommentFeedListActivity".equals(activityName)) {
            Intent activityIntent = activity.getIntent();
            String enterUri = activityIntent.getStringExtra("enterUri");
            if (enterUri == null) {
                return null;
            }
            String[] split = enterUri.split("/");
            String dynamicId = split[split.length - 1];
            XB.log("评论详情页，获取到动态ID：" + dynamicId);
            return dynamicId;
        }
        String msg = "糟糕，无法获取当前动态ID！当前Activity：" + activityName;
        XB.log(msg);
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        return null;
    }

    public static String getCookiesAsString(String dbPath) {
        SQLiteDatabase db = null;
        StringBuilder cookieString = new StringBuilder();
        try {
            // 打开数据库
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

            // 查询 cookies 表
            String query = "SELECT name, value FROM cookies WHERE host_key = '.bilibili.com';";
            Cursor cursor = db.rawQuery(query, null);

            // 遍历查询结果，生成cookie字符串
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String value = cursor.getString(cursor.getColumnIndexOrThrow("value"));

                    // 拼接cookie
                    if (cookieString.length() > 0) {
                        cookieString.append("; ");
                    }
                    cookieString.append(name).append("=").append(value);
                } while (cursor.moveToNext());
            }

            // 关闭Cursor
            cursor.close();
        } catch (SQLiteException e) {
            XB.log("获取App cookie失败，无法打开或查询数据库: " + e.getMessage());
        } finally {
            // 关闭数据库连接
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        // 返回cookie字符串
        return cookieString.toString();
    }
}
