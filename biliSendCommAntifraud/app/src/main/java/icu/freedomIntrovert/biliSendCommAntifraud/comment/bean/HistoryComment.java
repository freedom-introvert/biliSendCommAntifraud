package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import androidx.annotation.NonNull;

import java.util.Date;

public class HistoryComment  extends Comment{
    public static final String STATE_NORMAL = "normal";
    public static final String STATE_SHADOW_BAN = "shadowBan";
    public static final String STATE_DELETED = "deleted";
    public static final String STATE_INVISIBLE = "invisible";

    public long parent,root;
    public int like, replyCount;
    public String state;
    public Date lastCheckDate;

    public HistoryComment(CommentArea commentArea, long rpid,long parent,long root, String comment,Date date,int like,int replyCount,String state,Date lastCheckDate) {
        super(commentArea, rpid, comment, date);
        this.like = like;
        this.replyCount = replyCount;
        this.state = state;
        this.lastCheckDate = lastCheckDate;
        this.parent = parent;
        this.root = root;
    }

    public HistoryComment(long oid,String sourceId,int type, long rpid,long parent,long root, String comment, Date date, int like, int replyCount,String state,Date lastCheckDate) {
        super(new CommentArea(oid,sourceId,type), rpid, comment, date);
        this.like = like;
        this.replyCount = replyCount;
        this.state = state;
        this.lastCheckDate = lastCheckDate;
        this.parent = parent;
        this.root = root;
    }

    public String getFormatLastCheckDateFor_yMd(){
        return getFormatDateFor_yMd(lastCheckDate);
    }

    public String getFormatLastCheckDateFor_yMdHms(){
        return getFormatDateFor_yMdHms(lastCheckDate);
    }

    public String getStateDesc(){
        switch (state) {
            case STATE_NORMAL:
                return "正常";
            case STATE_SHADOW_BAN:
                return "评论仅自己可见";
            case STATE_DELETED:
                return "评论已被删除";
            case STATE_INVISIBLE:
                return "评论invisible，前端不可见";
            default:
                return state;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "HistoryComment{" +
                "commentArea=" + commentArea +
                ", rpid='" + rpid + '\'' +
                ", comment='" + comment + '\'' +
                ", date=" + date +
                ", like=" + like +
                ", reply=" + replyCount +
                '}';
    }

    public static String[] getCSVHeader(){
        return new String[]{"oid","sourceId","type","rpid", "parent", "root",  "comment",  "date",  "like",  "replyCount", "state", "lastCheckDate"};
    }

    public String[] toCSVStringArray() {
        return new String[]{String.valueOf(commentArea.oid),commentArea.sourceId, String.valueOf(commentArea.areaType), String.valueOf(rpid), String.valueOf(parent), String.valueOf(root),comment, String.valueOf(date.getTime()), String.valueOf(like), String.valueOf(replyCount),state, String.valueOf(lastCheckDate.getTime())};
    }
}
