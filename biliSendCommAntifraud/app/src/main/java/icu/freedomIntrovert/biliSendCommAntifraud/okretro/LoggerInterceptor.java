package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import icu.freedomIntrovert.biliSendCommAntifraud.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoggerInterceptor implements Interceptor {
    private final Logger logger;

    public LoggerInterceptor() {
        File logDir = new File("/data/data/" + BuildConfig.APPLICATION_ID + "/files/logs/");
        if (!logDir.exists()){
            logDir.mkdirs();
        }
        logger = new Logger(logDir);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        response = chain.proceed(request);
        // 打印请求信息
        String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(new Date());
        logger.log("========[OkHttpClient][" + time + "]=========");
        logger.log("Request URL: " + request.url());
        logger.log("Request Method: " + request.method());
        // 打印请求头
        logger.log("Request Headers: \n" + request.headers());

        // 打印响应信息
        logger.log("Response Code: " + response.code());
        logger.log("Response Message: " + response.message());

        // 打印响应头
        logger.log("Response Headers: \n" + response.headers());

        // 打印响应体 (仅在响应数据是 JSON 时打印)
        ResponseBody body = response.body();
        if (body != null) {
            MediaType contentType = body.contentType();
            if (contentType != null && contentType.toString().contains("json")) {
                String responseBody = body.string();
                logger.log("Response JSON Data: " + responseBody);
                // 由于 OkHttp 的 ResponseBody 只能读取一次，所以在打印后需要重新构建一个 Response 并返回
                logger.log("===========[OkHttpClient][end]===========");
                return response.newBuilder()
                        .body(ResponseBody.create(contentType, responseBody))
                        .build();
            }
        }
        logger.log("===========[OkHttpClient][end]===========");
        return response;
    }
}
