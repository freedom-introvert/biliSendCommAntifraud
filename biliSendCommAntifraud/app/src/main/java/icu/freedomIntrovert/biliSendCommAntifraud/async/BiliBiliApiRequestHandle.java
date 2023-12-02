package icu.freedomIntrovert.biliSendCommAntifraud.async;

import java.io.IOException;

import icu.freedomIntrovert.async.EventHandler;

public abstract class BiliBiliApiRequestHandle extends EventHandler {

    @Override
    public void handleError(Throwable th) {

    }

    public abstract void handleNetIOException(IOException e);
    public abstract void handleCookieFiledException(CookieFailedException e);
    public abstract void handleBiliBiliApiException(BiliBiliApiException e);
}
