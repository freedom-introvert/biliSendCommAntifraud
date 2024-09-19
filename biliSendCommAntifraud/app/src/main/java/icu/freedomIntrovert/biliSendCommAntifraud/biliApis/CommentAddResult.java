package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import com.alibaba.fastjson.annotation.JSONField;

public class CommentAddResult {

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