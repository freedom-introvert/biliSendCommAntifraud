package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BannedCommentBean {
    public static final String BANNED_TYPE_SHADOW_BAN = "shadowBan";
    public static final String BANNED_TYPE_QUICK_DELETE = "quickDelete";
    public static final String BANNED_TYPE_SENSITIVE = "sensitive";
    //基于前端的隐藏，评论正常出现在JSON数据中，但是"invisible": true，只是客户端不展示
    public static final String BANNED_TYPE_INVISIBLE = "invisible";
    //疑似没问题
    public static final String BANNED_TYPE_SUSPECTED_NO_PROBLEM = "suspectedNoProblem";
    //直接去申诉所导致的未知状态
    public static final String BANNED_TYPE_UNKNOWN = "unknown";

    //没有检查
    public static final int CHECKED_NO_CHECK = 0;
    //只检查了没有戒严
    public static final int CHECKED_NOT_MARTIAL_LAW = 1;
    //检查了这只是在此被ban
    public static final int CHECKED_ONLY_BANNED_IN_THIS_AREA = 2;
    //检查了在此未被ban
    public static final int CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA = 3;

    public CommentArea commentArea;
    public String rpid,comment, bannedType;
    public int checkedArea;
    public Date date;

    public BannedCommentBean(CommentArea commentArea, String rpid, String comment, String bandType, Date date, int checkedArea) {
        this.commentArea = commentArea;
        this.rpid = rpid;
        this.comment = comment;
        this.bannedType = bandType;
        this.checkedArea = checkedArea;
        this.date = date;
    }

    public BannedCommentBean(String rpid, long oid, String sourceId, String comment, String bannedType, String commentAreaType, String checkedArea, String date){
        this.commentArea = new CommentArea(oid, sourceId, Integer.parseInt(commentAreaType));
        this.rpid = rpid;
        this.comment = comment;
        this.bannedType = bannedType;
        this.checkedArea = Integer.parseInt(checkedArea);
        this.date = new Date(Long.parseLong(date));
    }

    public BannedCommentBean(CommentArea commentArea, long rpid, String comment, String bannedType, Date date, int checkedArea) {
        this(commentArea,String.valueOf(rpid),comment,bannedType,date,checkedArea);
    }

    public long getTimeStampDate(){
        return date.getTime();
    }

    public String getFormatDateFor_yMd(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public String getFormatDateFor_yMdHms(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    @Override
    public String toString() {
        return "BannedCommentBean{" +
                "commentArea=" + commentArea +
                ", rpid='" + rpid + '\'' +
                ", comment='" + comment + '\'' +
                ", bannedType='" + bannedType + '\'' +
                ", checkedArea=" + checkedArea +
                ", date=" + date +
                '}';
    }

    public String[] toCSVStringArray() {
        return new String[]{rpid, String.valueOf(commentArea.oid), commentArea.sourceId, comment, bannedType, String.valueOf(commentArea.areaType), String.valueOf(checkedArea), String.valueOf(getTimeStampDate())};
    }




}
