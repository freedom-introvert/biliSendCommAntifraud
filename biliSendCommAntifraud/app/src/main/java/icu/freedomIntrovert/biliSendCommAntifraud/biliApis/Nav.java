package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

public class Nav {
    public boolean isLogin;
    public String face;
    public LevelInfo level_info;
    public String uname;
    public long mid;
    public WbiImg wbi_img;
    public static class LevelInfo {
        public int current_level;
        public int current_min;
        public int current_exp;
        public String next_exp;
    }

    public static class WbiImg {
        public String img_url;
        public String sub_url;
    }
}
