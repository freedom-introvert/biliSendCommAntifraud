package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;

public class HttpUtil {
    public static void respNotNull(GeneralResponse<?> generalResponse) throws IOException {
        if (generalResponse == null){
            throw new IOException("response is null!");
        }
    }
}
