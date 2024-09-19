package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CommentArea {
    public static final String AREA_VIDEO = "video";
    public static final String AREA_ARTICLE = "article";
    public static final String AREA_DYNAMIC = "dynamic";
    public static final int AREA_TYPE_VIDEO = 1;
    public static final int AREA_TYPE_ARTICLE = 12;
    public static final int AREA_TYPE_DYNAMIC11 = 11;
    public static final int AREA_TYPE_DYNAMIC17 = 17;

    public long oid;
    public String sourceId;
    public int type;

    public CommentArea(long oid, String sourceId, int type) {
        this.oid = oid;
        this.sourceId = sourceId;
        this.type = type;
    }

    public String getAreaTypeDesc(){
        switch (type) {
            case CommentArea.AREA_TYPE_VIDEO:
                return "视频(type=" + type + ")";
            case CommentArea.AREA_TYPE_ARTICLE:
                return"专栏(type=" + type + ")";
            case CommentArea.AREA_TYPE_DYNAMIC11:
            case CommentArea.AREA_TYPE_DYNAMIC17:
                return "动态(type=" + type + ")";
            default:
                return String.valueOf(type);
        }
    }

    /**
     * 转换成用于申诉的来源URL
     * @return
     */
    public String toSourceUrl(){
        String url = null;
        if (type == CommentArea.AREA_TYPE_VIDEO) {
            url = "https://www.bilibili.com/video/" + sourceId;
        } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
            url = "https://www.bilibili.com/read/" + sourceId;
        } else if (type == CommentArea.AREA_TYPE_DYNAMIC11 || type == CommentArea.AREA_TYPE_DYNAMIC17) {
            url = "https://t.bilibili.com/" + sourceId;
        }
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentArea that = (CommentArea) o;
        return oid == that.oid && type == that.type && Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, sourceId, type);
    }

    @NonNull
    @Override
    public String toString() {
        return "CommentArea{" +
                "oid='" + oid + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", areaType='" + type + '\'' +
                '}';
    }
}
