package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import androidx.annotation.NonNull;

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
