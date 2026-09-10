package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import android.util.Log;

import androidx.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
//反编译自哔哩哔哩APP
public abstract class BiliApiCallback<T> implements Callback<T> {

    public boolean isCancel() {
        return false;
    }

    public abstract void onError(Throwable th);

    @Override // retrofit2.Callback
    public void onFailure(@Nullable Call<T> call, Throwable th) {
        if (isCancel()) {
            return;
        }
        if (call != null) {
            Log.e("onFailure", call.request().url() + " " + th.getMessage());
        } else {
            Log.e("onFailure", "", th);
        }
        onError(th);
    }

    @Override // retrofit2.Callback
    public void onResponse(@Nullable Call<T> call, Response<T> response) {
        if (isCancel()) {
            return;
        }
        if (!response.isSuccessful()) {
            onFailure(call, new HttpException(response));
            return;
        }
        onSuccess(response.body());
    }

    public abstract void onSuccess(T t);
}