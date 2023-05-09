package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

public class BiliComment {
    public long oid;
    public long rpid;
    public long parent;
    public long root;
    public long mid;
    public int like;
    public Content content;
    public Member member;
    public static class Content {
        public String message;
    }

    public static class Member{
        public long mid;
        public String uname;

    }

    public String getMessage(){
        return content.message;
    }

}
