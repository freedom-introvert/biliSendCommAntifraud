package icu.freedomIntrovert.biliSendCommAntifraud.biliApis;

import androidx.annotation.Nullable;

import java.util.List;

public class CommentPage {
    public Page page;
    public List<BiliComment> replies;
    @Nullable
    public List<BiliComment> top_replies;

    public static class Page {
        public int num;
        public int size;
        public int count;
        public int acount;
    }
}
