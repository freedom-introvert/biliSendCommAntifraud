package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;

public class ServiceGenerator {
    private static Retrofit retrofit;
    private static BiliApiService biliApiService;

    public static <T> T createService(Class<T> cls) {
        return (T) getRetrofit().create(cls);
    }

    public synchronized static Retrofit getRetrofit(){
        if (retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.bilibili.com")
                    .addCallAdapterFactory(new BBCallAdapterFactory())
                    .addConverterFactory(FastJsonConverterFactory.create())
                    .client(OkHttpUtil.getHttpClient())
                    .build();
        }
        return retrofit;
    }

    public synchronized static BiliApiService getBiliApiService(){
        if (biliApiService == null){
            biliApiService = getRetrofit().create(BiliApiService.class);
        }
        return biliApiService;
    }


}
