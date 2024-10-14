package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import java.io.IOException;

import retrofit2.Response;

public class ResponseNullException extends IOException {
    public final Response<?> response;

    public ResponseNullException(Response<?> response) {
        super("Response body was null, response:"+response);
        this.response = response;
    }
}
