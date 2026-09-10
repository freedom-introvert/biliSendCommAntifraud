package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import icu.freedomIntrovert.biliSendCommAntifraud.xposed.Reflect;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;


public class Utils {
    private static void log(String message) { android.util.Log.i("BiliAntifraud", message); }
    public static String picturesObjToString(List<?> obj) {
        List<BiliComment.Picture> pictures = new ArrayList<>();
        if (obj == null || obj.size() == 0){
            return null;
        }
        for (Object o : obj) {
            BiliComment.Picture picture = new BiliComment.Picture();
            picture.img_src = (String) Reflect.getObjectField(o, "src");
            picture.img_width = number(Reflect.getObjectField(o, "width"));
            picture.img_height = number(Reflect.getObjectField(o, "height"));
            picture.img_size = number(Reflect.getObjectField(o, "size"));
            pictures.add(picture);
        }
        return JSON.toJSONString(pictures);
    }
    private static double number(Object value) { return value instanceof Number ? ((Number) value).doubleValue() : 0; }

    public static String getBvidFormAvid(long avid) throws ExecutionException, InterruptedException {
        Callable<String> callable = () -> {
            BiliApiService biliApiService = ServiceGenerator.getBiliApiService();
            GeneralResponse<VideoInfo> body = biliApiService.getVideoInfoByAid(avid).execute().body();
            return Objects.requireNonNull(body).data.bvid;
        };
        FutureTask<String> task = new FutureTask<>(callable);
        task.run();
        return task.get();
    }


    public static void startActivity(Activity activity, Bundle extras) {
        startActivity((Context) activity, extras);
    }

    public static void startActivity(Context context, Bundle extras) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            Bundle snapshot = new Bundle(extras);
            icu.freedomIntrovert.async.TaskManger.postOnUiThread(() -> startActivity(context, snapshot));
            return;
        }
        Activity activity = context instanceof Activity ? (Activity) context : null;
        boolean live = activity != null && !activity.isFinishing() && !activity.isDestroyed();
        if (!live && context == null) {
            log("event=dispatch_failed reason=no_activity");
            return;
        }
        log("event=dispatch_comment");

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud",
                "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
        intent.putExtras(extras);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            if (live) {
                activity.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.getApplicationContext().startActivity(intent);
            }
        } catch (ActivityNotFoundException e){
            Context toastAt = live ? activity : context.getApplicationContext();
            Toast.makeText(toastAt, "你好像没有安装哔哩发评反诈应用，如果你使用了内置模块，请安装模块本体一起",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static void printFields(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(obj);
                log("Type: " + field.getType().getName() +
                        ", Field Name: " + field.getName() +
                        ", Value: " + fieldValue);
            } catch (IllegalAccessException e) {
                log("Failed to access the field: " + field.getName());
                e.printStackTrace();
            }
        }
    }

    public static boolean checkExtras(Bundle extras, String... keys){
        for (String key : keys) {
            if (!extras.containsKey(key)) {
                return false;
            }
        }
        return true;
    }
}
