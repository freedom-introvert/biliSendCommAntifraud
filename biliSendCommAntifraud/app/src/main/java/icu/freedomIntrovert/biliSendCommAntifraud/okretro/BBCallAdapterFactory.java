package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class BBCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull Retrofit retrofit) {
        // 确保返回类型是 Call

        if (getRawType(returnType) == BiliCall.class){
            final Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
            return new CallAdapter<Object, BiliCall<?,?>>() {
                @NonNull
                @Override
                public Type responseType() {
                    return responseType;
                }

                @SuppressWarnings("all")
                @NonNull
                @Override
                public BiliCall<?,?> adapt(@NonNull Call<Object> call) {
                    return new BiliCall(call);
                }
            };
        }
        return null;
    }
}
