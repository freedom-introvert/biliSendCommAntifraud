package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Comment {
    public CommentArea commentArea;
    public long rpid;
    public String comment;
    public Date date;

    public long getTimeStampDate(){
        return date.getTime();
    }

    public String getFormatDateFor_yMd(){
        return getFormatDateFor_yMd(date);
    }

    public String getFormatDateFor_yMdHms(){
        return getFormatDateFor_yMdHms(date);
    }

    public String getFormatDateFor_yMd(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return sdf.format(date);
    }

    public String getFormatDateFor_yMdHms(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(date);
    }

    public Comment(CommentArea commentArea, long rpid, String comment, Date date) {
        this.commentArea = commentArea;
        this.rpid = rpid;
        this.comment = comment;
        this.date = date;
    }
}
