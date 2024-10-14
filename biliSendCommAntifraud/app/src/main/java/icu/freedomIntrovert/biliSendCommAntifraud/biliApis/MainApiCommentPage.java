package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import java.util.List;

public class MainApiCommentPage extends CommentPage{
    public Cursor cursor;
    public List<BiliComment> replies;
    public Top top;
    public List<BiliComment> top_replies;
    public UpSelection up_selection;
    public Effects effects;
    public int assist;
    public int blacklist;
    public int vote;
    public Config config;
    public Upper upper;
    public Control control;
    public int note;
    public Object esports_grade_card;
    public Object callbacks;
    public String context_feature;
    public static class Cursor {
        public boolean is_begin;
        public int prev;
        public int next;
        public boolean is_end;
        public int mode;
        public String mode_text;
        public List<Integer> support_mode;
        public String name;
        public PaginationReply pagination_reply;
        public String session_id;

        // 嵌套类的定义
        public static class PaginationReply {
            public String next_offset;
            public String prev_offset;
        }
    }

    public static class Top {
        public Object admin; // 未知类型
        public Object upper; // 未知类型
        public Object vote; // 未知类型
    }

    public static class UpSelection {
        public int pending_count;
        public int ignore_count;
    }

    public static class Effects {
        public String preloading;
    }

    public static class Config {
        public int showtopic;
        public boolean show_up_flag;
        public boolean read_only;
    }

    public static class Upper {
        public long mid;
    }

    public static class Control {
        public boolean input_disable;
        public String root_input_text;
        public String child_input_text;
        public String giveup_input_text;
        public int screenshot_icon_state;
        public int upload_picture_icon_state;
        public String answer_guide_text;
        public String answer_guide_icon_url;
        public String answer_guide_ios_url;
        public String answer_guide_android_url;
        public String bg_text;
        public Object empty_page; // 假设不知道具体类型
        public int show_type;
        public String show_text;
        public boolean web_selection;
        public boolean disable_jump_emote;
        public boolean enable_charged;
        public boolean enable_cm_biz_helper;
        public Object preload_resources; // 假设不知道具体类型
    }


}