package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class OkHttpUtil {
    private static OkHttpClient okHttpClient;
    public static synchronized OkHttpClient getHttpClient(){
        if (okHttpClient == null){
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new LoggerInterceptor())
                    .build();
        }
        return okHttpClient;
    }
    public static void respNotNull(GeneralResponse<?> generalResponse) throws IOException {
        if (generalResponse == null){
            throw new IOException("response is null!");
        }
    }
    public static void respNotNull(ResponseBody resp) throws IOException {
        if (resp == null){
            throw new IOException("response is null!");
        }
    }
    public static <T> GeneralResponse<T> getBody(Response<GeneralResponse<T>> response) throws IOException {
        if (response.isSuccessful()){
            return response.body();
        } else {
            throw new IOException("response code:"+response.code());
        }
    }
}
