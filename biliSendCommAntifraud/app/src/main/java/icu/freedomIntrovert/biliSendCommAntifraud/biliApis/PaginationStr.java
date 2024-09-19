package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import com.alibaba.fastjson.JSON;

public class PaginationStr {
    public static final String INITIAL = "{\"offset\":\"\"}";

    public PaginationStr(String offset) {
        this.offset = offset;
    }

    public PaginationStr() {
    }

    public String offset;

    public String toJson() {
        return JSON.toJSONString(this);
    }
}