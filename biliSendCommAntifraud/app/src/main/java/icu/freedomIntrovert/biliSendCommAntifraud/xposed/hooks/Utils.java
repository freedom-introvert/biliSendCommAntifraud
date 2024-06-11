package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;

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
            OkHttpUtil.respNotNull(body);
            return body.data.bvid;
        };
        FutureTask<String> task = new FutureTask<>(callable);
        task.run();
        return task.get();
    }
}
