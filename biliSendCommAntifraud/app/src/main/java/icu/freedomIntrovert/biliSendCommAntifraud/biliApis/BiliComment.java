package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;


import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class BiliComment {
    public long rpid;
    public long oid;
    public int type;
    public long mid;
    public long root;
    public long parent;
    public Content content;
    public Member member;
    public int rcount;
    public List<BiliComment> replies;
    public int like;
    public boolean invisible;
    public long ctime;


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
        public String message;
        public int max_line;
    }


}
