package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;


import com.alibaba.fastjson.annotation.JSONField;

public class Comment {
    public long rpid;
    public Member member;

    public static class Member{
        public long mid;
        public String uname;
        public String avatar;
        public LevelInFo levelInFo;
    }

    public static class LevelInFo {
        @JSONField(name = "current_level")
        public int currentLevel;

    }


}
