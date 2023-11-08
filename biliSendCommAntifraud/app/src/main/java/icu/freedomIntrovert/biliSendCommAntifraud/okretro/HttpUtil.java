package icu.freedomIntrovert.biliSendCommAntifraud.okretro;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import okhttp3.ResponseBody;

public class HttpUtil {
    public static void respNotNull(GeneralResponse<?> generalResponse) throws IOException {
        if (generalResponse == null){
            throw new IOException("response is null!");
        }
    }
    public static void respNotNull(ResponseBody resp) throws IOException {
        if (resp == null){
            throw new IOException("response is null!");
        }
    }
}
