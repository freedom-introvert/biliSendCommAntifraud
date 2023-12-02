package icu.freedomIntrovert.biliSendCommAntifraud.async.comment;

import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandle;

public abstract class CommentCheckEventHandler extends BiliBiliApiRequestHandle {
    public static final int WHAT_COOKIE_FAILED = -1;
    public static final int WHAT_RESULT_OK = 0;
    public static final int WHAT_RESULT_SHADOW_BAN = 1;
    public static final int WHAT_RESULT_DELETED = 2;
    public static final int WHAT_RESULT_UNDER_REVIEW = 3;
    public static final int WHAT_ON_START_CHECK = 10;
    public static final int WHAT_ON_COMMENT_NOT_FOUNT = 11;

}
