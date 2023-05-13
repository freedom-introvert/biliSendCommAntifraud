package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;


import com.alibaba.fastjson.annotation.JSONField;

public class BiliComment {
    public long rpid;
    public long oid;
    public int type;
    public long mid;
    public long root;
    public long parent;
    public Content content;
    public Member member;


    public static class Member{
        public long mid;
        public String uname;
        public String avatar;
        public LevelInFo level_info;
    }

    public static class LevelInFo {
        @JSONField(name = "current_level")
        public int currentLevel;

    }

    public static class Content {
        String message;
        int max_line;
    }


}
