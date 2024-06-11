package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import com.alibaba.fastjson.annotation.JSONField;

public class CommentAddResult {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_DELETED = 12022;
    public static final int CODE_CONTAIN_SENSITIVE = 12016;
    public static final int CODE_NOT_THE_COMMENT = 12006;
    @JSONField(name = "rpid")
    public long rpid;
    @JSONField(name = "success_toast")
    public String success_toast;
    @JSONField(name = "success_action")
    public int success_action;
    public long root;
    public long parent;
    public BiliComment reply;
}