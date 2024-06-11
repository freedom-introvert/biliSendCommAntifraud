package icu.freedomIntrovert.biliSendCommAntifraud.async;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;

public class BiliBiliApiException extends Exception{
    public final int code;
    public final String message;
    public final String tipsMessage;

    public BiliBiliApiException(int code, String message,@Nullable String tipsMessage) {
        this.code = code;
        this.message = message;
        this.tipsMessage = tipsMessage;
    }

    public BiliBiliApiException(GeneralResponse<?> response,@Nullable String tipsMessage){
        super(String.format("Tips:%s\ncode:%s\nmessage:%s",
                tipsMessage,response.code,response.message));
        this.code = response.code;
        this.message = response.message;
        this.tipsMessage = tipsMessage;
    }

    @NonNull
    @Override
    public String toString() {
        return "BiliBiliApiException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
