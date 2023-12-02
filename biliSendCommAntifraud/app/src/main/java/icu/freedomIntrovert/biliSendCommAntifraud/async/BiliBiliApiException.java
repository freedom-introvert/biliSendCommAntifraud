package icu.freedomIntrovert.biliSendCommAntifraud.async;

import androidx.annotation.NonNull;

public class BiliBiliApiException extends Exception{
    public final int code;
    public final String message;

    public BiliBiliApiException(int code, String message) {
        this.code = code;
        this.message = message;
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
