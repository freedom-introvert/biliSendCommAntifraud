package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.annotation.JSONField;

import org.jetbrains.annotations.Nullable;

public abstract class BaseResponse {
    public int code;

    @Nullable
    public String message;
    public int ttl;

    @JSONField(deserialize = false, serialize = false)
    public boolean isSuccess() {
        return this.code == 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "BaseResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}