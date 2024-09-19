package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BBCall<T extends GeneralResponse<R>,R> implements Call<T> {

    private final Call<T> delegate; // 原始的 Retrofit Call

    public BBCall(Call<T> delegate) {
        this.delegate = delegate;
    }


    @NonNull
    @Override
    public Response<T> execute() throws IOException {
        // 可以在这里对请求结果进行自定义处理
        // 对 response 进行自定义处理
        return delegate.execute();
    }

    @Override
    public void enqueue(@NonNull Callback<T> callback) {
        // 可以在这里自定义异步处理逻辑
        delegate.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                // 对 response 进行自定义处理
                callback.onResponse(BBCall.this, response);
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                // 对错误进行自定义处理
                callback.onFailure(BBCall.this, t);
            }
        });
    }

    @Override
    public boolean isExecuted() {
        return delegate.isExecuted();
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    @SuppressWarnings("all")
    @NonNull
    @Override
    public Call<T> clone() {
        return new BBCall<>(delegate.clone());
    }

    @NonNull
    @Override
    public Request request() {
        return delegate.request();
    }

    @NonNull
    @Override
    public Timeout timeout() {
        return delegate.timeout();
    }

    @NotNull
    public T exe() throws IOException {
        Response<T> response = delegate.execute();
        T body = response.body();
        if (body == null){
            throw new ResponseNullException(response);
        } else {
            return body;
        }
    }

    public R data() throws IOException {
        return exe().data;
    }

    public R success(String errorTips) throws IOException, BiliBiliApiException {
        Response<T> response = delegate.execute();
        T body = response.body();
        if (body == null){
            throw new ResponseNullException(response);
        }
        if (body.isSuccess()){
            return body.data;
        } else {
            throw new BiliBiliApiException(body,errorTips);
        }
    }


}

