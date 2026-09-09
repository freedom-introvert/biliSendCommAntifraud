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

import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.HookConfig;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;
import retrofit2.Call;

public abstract class PostCommentHook extends BaseHook {
    private volatile java.lang.ref.WeakReference<Activity> resumed = new java.lang.ref.WeakReference<>(null);
    private final CaptureDeduper captured = CaptureDeduper.SHARED;
    private volatile Context hostContext;

    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws Throwable {
        hook(Activity.class, "onResume", chain -> {
            Object result = chain.proceed();
            Activity activity = (Activity) chain.getThisObject();
            resumed = new java.lang.ref.WeakReference<>(activity);
            hostContext = activity.getApplicationContext();
            return result;
        });
        hook(Activity.class, "startActivityForResult", chain -> {
            Activity activity = (Activity) chain.getThisObject();
            String host = activity.getClass().getName();
            if (HostCallAdapter.isComposeOrVertexActivity(host)) {
                Intent intent = (Intent) chain.getArg(0);
                if (intent != null) intent.putExtra("inject_dynamic_id", getDynamic11ID(activity));
            }
            return chain.proceed();
        }, Intent.class, int.class, Bundle.class);
        hook(classLoader.loadClass(getBiliCallClassName(classLoader)), "execute", chain -> {
            Object result = chain.proceed();
            try {
                if (!isCommentAddCall(chain.getThisObject())) {
                    return result;
                }
                Activity activity = resumed.get();
                processResponse(chain.getThisObject(), result, activity, hostContext);
            } catch (Throwable failure) {
                XB.error("event=comment_capture_failed", failure);
            }
            return result;
        });
    }

    private void processResponse(Object call, Object response, Activity currentActivity, Context hostContext) throws Throwable {
                Object arg = response;
                if (arg == null) {
                    return;
                }
                Object body = extractResponseBody(arg);
                if (body == null) {
                    return;
                }

                String bodyCanonicalName = body.getClass().getCanonicalName();
                if (!(bodyCanonicalName != null && bodyCanonicalName.equals("com.bilibili.okretro.GeneralResponse"))) {
                    return;
                }
                Object data = Reflect.getObjectField(body, "data");
                if (data != null && "com.bilibili.app.comm.comment2.model.BiliCommentAddResult".equals(data.getClass().getCanonicalName())) {
                    Bundle extras = new Bundle();
                    Class<?> biliCommentAddResultClass = data.getClass();
                    Object reply = Reflect.getObjectField(data, "reply");
                    Object content = Reflect.getObjectField(reply, "mContent");
                    Integer type = (Integer) Reflect.getObjectField(reply, "mType");
                    Long oid = (Long) Reflect.getObjectField(reply, "mOid");
                    //判断是否是评论区要精选的，是的话就不要检查了
                    Integer action = ((Integer)biliCommentAddResultClass.getField("action").get(data));
                    if (skipNonZeroHostAction() && action != null && action != 0) {
                        XB.log("event=comment_skipped reason=host_action action=" + action);
                        return;
                    }
                    extras.putInt("action", ByXposedLaunchedActivity.ACTION_CHECK_COMMENT);
                    extras.putString("toast_message", (String) biliCommentAddResultClass.getField("message").get(data));
                    extras.putLong("oid", oid);
                    extras.putInt("type", type);
                    extras.putLong("rpid", Reflect.getLongField(data, "rpid"));
                    extras.putLong("root", Reflect.getLongField(data, "root"));
                    extras.putLong("parent", Reflect.getLongField(data, "parent"));
                    long rpid = extras.getLong("rpid");
                    if (rpid == 0 || !captured.firstSeen(CaptureDeduper.checkKey(rpid))) {
                        XB.log("event=comment_skipped rpid=" + rpid + " reason=no_rpid_or_duplicate");
                        return;
                    }
                    extras.putString("comment_text", (String) Reflect.getObjectField(content, "mMsg"));
                    extras.putString("source_id", tryGetSourceId(currentActivity,type, oid));
                    extras.putLong("uid", Reflect.getLongField(reply, "mMid"));
                    try {
                        Field picturesField = content.getClass().getField("pictures");
                        List<?> pictures = (List<?>) picturesField.get(content);
                        extras.putString("pictures", Utils.picturesObjToString(pictures));
                    } catch (NoSuchFieldException e) {
                        XB.log("当前哔哩哔哩版本不支持发送图片");
                    }
                    long ctime = Reflect.getLongField(reply, "mCtime");
                    extras.putLong("ctime", ctime);
                    if (HookConfig.useClientCookie()){
                        ArrayList<String> cookies = new ArrayList<>();
                        Context cookieCtx = currentActivity != null ? currentActivity : hostContext;
                        for (String cookieDBFilePath : getCookieDBFilePaths()) {
                            String cookie = cookieCtx == null ? null : getCookiesAsString(cookieCtx, cookieDBFilePath);
                            if (cookie != null && cookie.contains("SESSDATA")) {
                                cookies.add(cookie);
                            }
                        }
                        extras.putStringArrayList("cookies", cookies);
                    }
                    //extras.putString("cookie",getCookiesAsString("/data/data/tv.danmaku.bili/app_webview_tv.danmaku.bili/Default/Cookies"));
                    XB.log("event=comment_captured rpid=" + extras.getLong("rpid")
                            + " oid=" + oid + " type=" + type + " host_action=" + action);
                    Utils.startActivity(currentActivity != null ? currentActivity : hostContext, extras);
                } else if (Reflect.getIntField(body, "code") == GeneralResponse.CODE_COMMENT_CONTAIN_SENSITIVE) {
                    Map<String, String> requsetMap = extractFormFields(extractRequest(call));
                    Bundle extras = new Bundle();
                    extras.putInt("action", ByXposedLaunchedActivity.ACTION_SAVE_CONTAIN_SENSITIVE_CONTENT);
                    long oid = Long.parseLong(Objects.requireNonNull(requsetMap.get("oid")));
                    int type = Integer.parseInt(Objects.requireNonNull(requsetMap.get("type")));
                    extras.putLong("oid", oid);
                    extras.putInt("type", type);
                    extras.putString("source_id", tryGetSourceId(currentActivity,type, oid));
                    extras.putString("comment_text", requsetMap.get("message"));
                    extras.putString("toast_message", (String) Reflect.getObjectField(body, "message"));
                    if (!captured.firstSeen(CaptureDeduper.sensitiveKey(oid, type, requsetMap.get("message")))) {
                        XB.log("event=comment_skipped kind=sensitive oid=" + oid + " type=" + type);
                        return;
                    }
                    XB.log("event=comment_captured kind=sensitive oid=" + oid + " type=" + type);
                    Utils.startActivity(currentActivity != null ? currentActivity : hostContext, extras);
                }
    }

    protected abstract String getBiliCallClassName(ClassLoader classLoader);

    protected abstract String getBiliCall_body_MethodName();

    protected abstract String getBiliCall_request_MethodName();

    protected abstract String[] getCookieDBFilePaths();

    protected Object extractResponseBody(Object response) {
        return Reflect.callMethod(response, getBiliCall_body_MethodName());
    }

    protected Object extractRequest(Object call) {
        return Reflect.callMethod(call, getBiliCall_request_MethodName());
    }

    protected Map<String, String> extractFormFields(Object request) {
        return HostCallAdapter.formFieldsOf(request);
    }

    protected boolean isCommentAddCall(Object call) {
        return true;
    }

    protected boolean skipNonZeroHostAction() {
        return true;
    }

    protected String tryGetSourceId(Activity activity,int type, long oid) {
        switch (type) {
            case CommentArea.AREA_TYPE_VIDEO:
                String bvid = getBvidFromActivity(activity);
                if (bvid == null){
                    String data = (activity != null && activity.getIntent() != null) ? activity.getIntent().getDataString() : null;
                    XB.log("event=source_id_fallback oid=" + oid + " activity=" + activity);
                    return HostCallAdapter.videoSourceId(null, null, data, oid);
                } else {
                    return bvid;
                }
            case CommentArea.AREA_TYPE_ARTICLE:
                return "cv" + oid;
            case CommentArea.AREA_TYPE_DYNAMIC17:
                return String.valueOf(oid);
            case CommentArea.AREA_TYPE_DYNAMIC11:
                String dynId = getDynamic11ID(activity);
                return dynId != null ? dynId : String.valueOf(oid);
            default:
                XB.log("event=source_id_unknown_type type=" + type + " oid=" + oid);
                return String.valueOf(oid);
        }
    }

    public static String getDynamic11ID(Activity activity) {
        if (activity == null) return null;
        String activityName = activity.getClass().getCanonicalName();
        Bundle extras = activity.getIntent().getExtras();
        if (extras == null) {
            return null;
        }
        String id = null;
        if (activityName == null) {
            return null;
        }
        Bundle fragmentArgs = extras.getBundle("fragment_args");
        id = HostCallAdapter.dynamicIdFrom(
                extras.getString("inject_dynamic_id", extras.getString("dynamicId", extras.getString("dynamic_id"))),
                fragmentArgs != null ? fragmentArgs.getString("dynamicId") : null,
                fragmentArgs != null ? fragmentArgs.getString("oid") : null,
                extras.getString("blrouter.targeturl"),
                extras.getString("enterUri", activity.getIntent().getDataString()));
        if (id == null) {
            id = HostCallAdapter.opusOrDynamicId(activity.getIntent().getDataString());
        }
        if (id == null) {
            Object vertexSource = extras.get("kntr:vertex_source");
            if (vertexSource != null) {
                id = HostCallAdapter.opusOrDynamicId(String.valueOf(vertexSource));
            }
        }
        if (id != null) {
            XB.log("动态ID:" + id);
            XB.log("Activity：" + activityName);
            return id;
        }
        switch (activityName) {
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
            if (HostCallAdapter.isComposeOrVertexActivity(activityName)) {
                XB.log("event=source_id_missing_vertex activity=" + activityName);
                dumpIntent(activity);
                return null;
            }
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
        if (activity == null) return null;
        Intent intent = activity.getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            String fromExtras = HostCallAdapter.videoSourceId(
                    extras != null ? extras.getString("bvid") : null,
                    extras != null ? extras.getString("id") : null,
                    intent.getDataString(),
                    0L);
            if (fromExtras != null && !fromExtras.startsWith("AV")) {
                XB.log("从Intent获取到BV号：" + fromExtras);
                return fromExtras;
            }
        }
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

    public String getCookiesAsString(Activity currentActivity, String dbPath) {
        return getCookiesAsString((Context) currentActivity, dbPath);
    }

    public String getCookiesAsString(Context currentActivity, String dbPath) {
        SQLiteDatabase db = null;
        Map<String, String> cookieMap = new HashMap<>();
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT name, value FROM cookies WHERE host_key = '.bilibili.com';", null);
            if (cursor.moveToFirst()) {
                do {
                    cookieMap.put(cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            cursor.getString(cursor.getColumnIndexOrThrow("value")));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (SQLiteException e) {
            XB.log("event=cookie_sqlite_failed path_present=1");
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        try {
            File biliAccountStorage = new File(currentActivity.getFilesDir(), "bili.account.storage");
            DataInputStream dis = new DataInputStream(new FileInputStream(biliAccountStorage));
            byte[] buffer = new byte[(int) biliAccountStorage.length()];
            dis.readFully(buffer);
            dis.close();
            JSONObject cookieInfo = JSON.parseObject(new String(Base64.decode(buffer, Base64.DEFAULT)));
            JSONArray cookies = cookieInfo.getJSONArray("cookies");
            if (cookies != null) {
                for (int i = 0; i < cookies.size(); i++) {
                    JSONObject cookie = cookies.getJSONObject(i);
                    cookieMap.put(cookie.getString("name"), cookie.getString("value"));
                }
            }
        } catch (IOException e) {
            XB.log("event=cookie_storage_failed");
        }
        if (!cookieMap.containsKey("SESSDATA")) return null;
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = cookieMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (iterator.hasNext()) sb.append("; ");
        }
        return sb.toString();
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
                XB.log("  Extra key: " + key);
            }
        } else {
            XB.log("No extras found.");
        }
    }
}
