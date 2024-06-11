package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Comment {
    public CommentArea commentArea;
    public long rpid;
    public long parent;
    public long root;
    public String comment;
    public String pictures;
    public Date date;

    public Comment(CommentArea commentArea, long rpid,long parent,long root, String comment,String pictures, Date date) {
        this.commentArea = commentArea;
        this.rpid = rpid;
        this.comment = comment;
        this.date = date;
        this.parent = parent;
        this.root = root;
        this.pictures = pictures;
    }

    public long getTimeStampDate(){
        return date.getTime();
    }

    public String getFormatDateFor_yMd(){
        return getFormatDateFor_yMd(date);
    }

    public String getFormatDateFor_yMdHms(){
        return getFormatDateFor_yMdHms(date);
    }

    public static String getFormatDateFor_yMd(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return sdf.format(date);
    }

    public static String getFormatDateFor_yMdHms(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(date);
    }

    public String getPictures() {
        return pictures;
    }

    public void setPictures(String pictures) {
        this.pictures = pictures;
    }

    public List<PictureInfo> getPictureInfoList(){
        if (!hasPictures()){
            return null;
        }
        return PictureInfo.parseJson(pictures);
    }

    public boolean hasPictures(){
        return !TextUtils.isEmpty(pictures);
    }

    public static class PictureInfo {
        public int img_height;
        public double img_size;
        public String img_src;
        public int img_width;

        public static List<PictureInfo> parseJson(String jsonString){
            return JSON.parseArray(jsonString, PictureInfo.class);
        }

        public static String toJsonString(List<PictureInfo> imageInfoList){
            return JSON.toJSONString(imageInfoList);
        }
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentArea=" + commentArea +
                ", rpid=" + rpid +
                ", parent=" + parent +
                ", root=" + root +
                ", comment='" + comment + '\'' +
                ", pictures='" + pictures + '\'' +
                ", date=" + date +
                '}';
    }
}
