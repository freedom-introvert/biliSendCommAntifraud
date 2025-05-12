package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;
import retrofit2.Call;

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


        //若ComposeActivity启动其他Activity，将动态ID往上传递
        XposedHelpers.findAndHookMethod(Activity.class, "startActivityForResult", Intent.class, int.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity activity = (Activity) param.thisObject;
                if ("com.bilibili.lib.ui.ComposeActivity".equals(activity.getClass().getCanonicalName())) {
                    Intent intent = (Intent) param.args[0];
                    String dynamic11ID = getDynamic11ID(activity);
                    intent.putExtra("inject_dynamic_id", dynamic11ID);
                    XB.log(String.format("动态ID:%s 已注入将要打开的Activity: %s",dynamic11ID,intent.getComponent()));
                }
            }
        });

        XposedHelpers.findAndHookMethod(getBiliCallClassName(classLoader)/*com.bilibili.okretro.call.BiliCall 混淆*/, classLoader, "execute", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object arg = param.getResult();
                if (arg == null) {
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
                if (data != null && "com.bilibili.app.comm.comment2.model.BiliCommentAddResult".equals(data.getClass().getCanonicalName())) {
                    Bundle extras = new Bundle();
                    Class<?> biliCommentAddResultClass = data.getClass();
                    Object reply = XposedHelpers.getObjectField(data, "reply");
                    Object content = XposedHelpers.getObjectField(reply, "mContent");
                    Integer type = (Integer) XposedHelpers.getObjectField(reply, "mType");
                    Long oid = (Long) XposedHelpers.getObjectField(reply, "mOid");
                    //判断是否是评论区要精选的，是的话就不要检查了
                    Integer action = ((Integer)biliCommentAddResultClass.getField("action").get(data));
                    if (action != null && !(action == 0)) {
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
                    extras.putString("source_id", tryGetSourceId(currentActivity,type, oid));
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
                    if (Config.getInstanceByXPEnvironment().getUseClientCookie()){
                        ArrayList<String> cookies = new ArrayList<>();
                        for (String cookieDBFilePath : getCookieDBFilePaths()) {
                            String cookie = getCookiesAsString(cookieDBFilePath);
                            if (cookie != null && cookie.contains("SESSDATA") && cookie.contains("buvid3")) {
                                cookies.add(cookie);
                            }
                        }
                        extras.putStringArrayList("cookies", cookies);
                    }
                    //extras.putString("cookie",getCookiesAsString("/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili/Default/Cookies"));
                    Utils.startActivity(currentActivity, extras);
                } else if (XposedHelpers.getIntField(body, "code") == GeneralResponse.CODE_COMMENT_CONTAIN_SENSITIVE) {
                    Object request = XposedHelpers.callMethod(param.thisObject, getBiliCall_request_MethodName()/*request 混淆*/);
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
                    extras.putString("source_id", tryGetSourceId(currentActivity,type, oid));
                    extras.putString("comment_text", requsetMap.get("message"));
                    extras.putString("toast_message", (String) XposedHelpers.getObjectField(body, "message"));
                    Utils.startActivity(currentActivity, extras);
                }
            }
        });
        hook(appVersionCode, classLoader);
    }


    public abstract void hook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException;

    protected abstract String getBiliCallClassName(ClassLoader classLoader);

    protected abstract String getBiliCall_body_MethodName();

    protected abstract String getBiliCall_request_MethodName();

    protected abstract String[] getCookieDBFilePaths();

    protected String tryGetSourceId(Activity activity,int type, long oid) {
        switch (type) {
            case CommentArea.AREA_TYPE_VIDEO:
                String bvid = getBvidFromActivity(activity);
                if (bvid == null){
                    XB.log("⚠️从Activity："+activity+" 中获取BV号失败，尝试调用API获取AV号对应的BV号");
                    try {
                        return Utils.getBvidFormAvid(oid);
                    } catch (ExecutionException | InterruptedException e) {
                        XB.log("⚠️⚠️网络错误，返回视频的AV号");
                        return "AV" + oid;
                    }
                } else {
                    return bvid;
                }
            case CommentArea.AREA_TYPE_ARTICLE:
                return "cv" + oid;
            case CommentArea.AREA_TYPE_DYNAMIC17:
                return String.valueOf(oid);
            case CommentArea.AREA_TYPE_DYNAMIC11:
                return getDynamic11ID(currentActivity);
            default:
                String msg = "不支持的评论区类型：" + type + "，无法获取源ID，请报告哔哩发评反诈开发者！";
                XB.log(msg);
                toastInUi(currentActivity, msg, Toast.LENGTH_SHORT);
                return null;
        }
    }

    public static String getDynamic11ID(Activity activity) {
        String activityName = activity.getClass().getCanonicalName();
        Bundle extras = activity.getIntent().getExtras();
        if (extras == null) {
            return null;
        }
        String id = null;
        if (activityName == null) {
            return null;
        }
        switch (activityName) {
            case "com.bilibili.lib.ui.ComposeActivity":
                Bundle fragmentArgs = extras.getBundle("fragment_args");
                if (fragmentArgs == null) {
                    break;
                }
                id = fragmentArgs.getString("dynamicId");
                if (id == null) {
                    id = fragmentArgs.getString("oid");
                }
                if (id == null) {
                    String targetUrl = extras.getString("blrouter.targeturl");
                    if (targetUrl != null) {
                        String[] split = targetUrl.split("/");
                        id = split[split.length - 1];
                    }
                }
                break;
            case "com.bilibili.app.comm.comment2.comments.view.CommentDetailActivity"://信息箱打开评论详情页的情况
            case "com.bilibili.app.comm.comment2.comments.view.CommentFeedListActivity"://
                Intent activityIntent = activity.getIntent();
                String enterUri = activityIntent.getStringExtra("enterUri");
                if (enterUri != null) {
                    String[] split = enterUri.split("/");
                    id = split[split.length - 1];
                }
                break;
            case "com.bilibili.lib.ui.GeneralActivity"://楼中楼回复，动态ID接力，需在com.bilibili.lib.ui.ComposeActivity启动本Activity时注入动态ID
                id = extras.getString("inject_dynamic_id");
                break;
        }
        if (id == null){
            String msg = "糟糕，无法获取当前动态ID！当前Activity：" + activityName;
            XB.log(msg);
            dumpIntent(activity);
            toastInUi(activity, msg, Toast.LENGTH_SHORT);
        } else {
            XB.log("动态ID:" + id);
            XB.log("Activity：" + activityName);
        }
        return id;
    }

    @SuppressLint("DiscouragedApi")
    public static String getBvidFromActivity(Activity activity){
        int viewId = activity.getResources().getIdentifier("avid_title", "id", activity.getPackageName());
        if (viewId == 0){
            return null;
        }
        TextView descTextView = activity.findViewById(viewId);
        if (descTextView == null){
            return null;
        }
        CharSequence text = descTextView.getText();
        if (text == null){
            return null;
        }
        String avidTitle = text.toString();
        if (avidTitle.startsWith("BV") || avidTitle.startsWith("AV") || avidTitle.startsWith("av")){
            XB.log("从Activity里获取到BV号："+avidTitle);
            return avidTitle;
        }
        return null;
    }

    public String getCookiesAsString(String dbPath) {
        SQLiteDatabase db = null;
        try {
            Map<String,String> cookieMap = new HashMap<>();
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
                    cookieMap.put(name,value);
                } while (cursor.moveToNext());
            }
            // 关闭Cursor
            cursor.close();

            File biliAccountStorage = new File(currentActivity.getFilesDir(),"bili.account.storage");
            @SuppressWarnings("all")//Android Studio 你是不是有什么大病 'InputStream' can be constructed using 'Files.newInputStream()，然后按照你的做我的最低API不支持，加了API判断你还他妈的在老的分支报黄😅
            DataInputStream dis = new DataInputStream(new FileInputStream(biliAccountStorage));
            byte[] buffer = new byte[(int) biliAccountStorage.length()];
            dis.readFully(buffer);
            dis.close();
            byte[] decode = Base64.decode(buffer, Base64.DEFAULT);
            JSONObject cookieInfo = JSON.parseObject(new String(decode));
            JSONArray cookies = cookieInfo.getJSONArray("cookies");
            for (int i = 0; i < cookies.size(); i++) {
                JSONObject cookie = cookies.getJSONObject(i);
                cookieMap.put(cookie.getString("name"),cookie.getString("value"));
            }
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = cookieMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (iterator.hasNext()) {
                    sb.append("; ");
                }
            }
            return sb.toString();
        } catch (SQLiteException e) {
            XB.log("获取App cookie失败，无法打开或查询数据库: " + e.getMessage());
        } catch (IOException e) {
            XB.log("获取App cookie失败，无法打开bili.account.storage文件，异常信息: " + e.getMessage());
        } finally {
            // 关闭数据库连接
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        // 返回cookie字符串
        return null;
    }

    public static void toastInUi(Context context, CharSequence text, int duration) {
        TaskManger.postOnUiThread(() -> Toast.makeText(context, text, duration).show());
    }

    public static void dumpIntent(Activity activity) {
        // 获取当前Activity的Intent
        Intent intent = activity.getIntent();
        if (intent == null) {
            XB.log("No Intent found.");
            return;
        }

        // 打印Intent的基本信息
        XB.log("Action: " + intent.getAction());
        XB.log("Data: " + intent.getDataString());
        XB.log("Categories: " + intent.getCategories());

        // 获取Intent的extras
        Bundle extras = intent.getExtras();
        if (extras != null) {
            XB.log("Extras:");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                XB.log("  Key: " + key + ", Value: " + value);
            }
        } else {
            XB.log("No extras found.");
        }
    }
}
