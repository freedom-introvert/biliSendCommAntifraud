package icu.freedomIntrovert.biliSendCommAntifraud.async.comment;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.CookieFailedException;

public class DialogCommentCheckEventHandler extends CommentCheckEventHandler{

    @Override
    public void handleEvent(int what, Object data) {
        switch (what) {
            case WHAT_RESULT_OK:
                //评论显示正常
                
        }
    }

    @Override
    public void handleNetIOException(IOException e) {

    }

    @Override
    public void handleCookieFiledException(CookieFailedException e) {

    }

    @Override
    public void handleBiliBiliApiException(BiliBiliApiException e) {

    }
}
