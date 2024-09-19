package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

public class GeneralResponse<T> extends BaseResponse {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_COMMENT_DELETED = 12022;
    public static final int CODE_COMMENT_CONTAIN_SENSITIVE = 12016;
    public static final int CODE_COMMENT_NOT_THIS = 12006;
    public static final int CODE_COMMENT_AREA_CLOSED = 12002;
    public T data;
}