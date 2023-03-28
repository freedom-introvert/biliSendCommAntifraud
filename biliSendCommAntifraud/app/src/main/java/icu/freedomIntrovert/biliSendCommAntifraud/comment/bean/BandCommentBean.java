package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BandCommentBean{
    public static final String BANNED_TYPE_SHADOW_BAN = "shadowBan";
    public static final String BANNED_TYPE_QUICK_DELETE = "quickDelete";
    public static final String BANNED_TYPE_SENSITIVE = "sensitive";
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

    public BandCommentBean(CommentArea commentArea, String rpid, String comment, String bandType, Date date, int checkedArea) {
        this.commentArea = commentArea;
        this.rpid = rpid;
        this.comment = comment;
        this.bannedType = bandType;
        this.checkedArea = checkedArea;
        this.date = date;
    }

    public BandCommentBean(String rpid,String oid,String sourceId,String comment,String bannedType,String commentAreaType,String checkedArea,String date){
        this.commentArea = new CommentArea(oid, sourceId, Integer.parseInt(commentAreaType));
        this.rpid = rpid;
        this.comment = comment;
        this.bannedType = bannedType;
        this.checkedArea = Integer.parseInt(checkedArea);
        this.date = new Date(Long.parseLong(date));
    }

    public BandCommentBean(CommentArea commentArea, long rpid, String comment, String bannedType, Date date, int checkedArea) {
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
        return "BandCommentBean{" +
                "commentArea=" + commentArea +
                ", rpid='" + rpid + '\'' +
                ", comment='" + comment + '\'' +
                ", bannedType='" + bannedType + '\'' +
                ", checkedArea=" + checkedArea +
                ", date=" + date +
                '}';
    }

    public String[] toCSVStringArray() {
        return new String[]{rpid, commentArea.oid, commentArea.sourceId, comment, bannedType, String.valueOf(commentArea.areaType), String.valueOf(checkedArea), String.valueOf(getTimeStampDate())};
    }




}
