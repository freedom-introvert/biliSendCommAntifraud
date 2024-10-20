package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;

public class Utils {
    public static String picturesObjToString(List<?> obj) {
        List<BiliComment.Picture> pictures = new ArrayList<>();
        if (obj == null || obj.size() == 0){
            return null;
        }
        for (Object o : obj) {
            BiliComment.Picture picture = new BiliComment.Picture();
            picture.img_src = (String) XposedHelpers.getObjectField(o, "src");
            picture.img_width = (Double) XposedHelpers.getObjectField(o, "width");
            picture.img_height = (Double) XposedHelpers.getObjectField(o, "height");
            picture.img_size = (Double) XposedHelpers.getObjectField(o, "size");
            pictures.add(picture);
        }
        return JSON.toJSONString(pictures);
    }
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
        XB.log("当前Activity：" + activity);
        XB.log("启动反诈参数：");
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            if (key.equals("cookies")){
                XB.log("    Key: " + key + ", Value: ■■■■");
            } else {
                XB.log("    Key: " + key + ", Value: " + value);
            }
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("icu.freedomIntrovert.biliSendCommAntifraud",
                "icu.freedomIntrovert.biliSendCommAntifraud.ByXposedLaunchedActivity"));
        intent.putExtras(extras);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e){
            Toast.makeText(activity, "你好像没有安装哔哩发评反诈应用，如果你使用了内置模块，请安装模块本体一起",
                    Toast.LENGTH_SHORT).show();
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
