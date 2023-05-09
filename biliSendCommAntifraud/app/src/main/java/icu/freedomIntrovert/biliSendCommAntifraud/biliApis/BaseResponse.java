package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import com.alibaba.fastjson.annotation.JSONField;

import org.jetbrains.annotations.Nullable;

import kotlin.jvm.JvmField;

public abstract class BaseResponse {
    @JvmField
    public int code;
    @JvmField
    @Nullable
    public String message;
    @JvmField
    public int ttl;

    @JSONField(deserialize = false, serialize = false)
    public boolean isSuccess() {
        return this.code == 0;
    }
}