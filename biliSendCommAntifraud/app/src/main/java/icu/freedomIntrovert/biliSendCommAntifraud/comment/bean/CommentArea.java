package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

public class CommentArea {
    public static final String AREA_VIDEO = "video";
    public static final String AREA_ARTICLE = "article";
    public static final String AREA_DYNAMIC = "dynamic";

    public static final int AREA_TYPE_VIDEO = 1;
    public static final int AREA_TYPE_ARTICLE = 12;
    public static final int AREA_TYPE_DYNAMIC11 = 11;
    public static final int AREA_TYPE_DYNAMIC17 = 17;

    public String oid, sourceId;
    public int areaType;

    public CommentArea(String oid, String sourceId, int areaType) {
        this.oid = oid;
        this.sourceId = sourceId;
        this.areaType = areaType;
    }

    public String getStringAreaType() {
        return String.valueOf(areaType);
    }

    @Override
    public String toString() {
        return "CommentArea{" +
                "oid='" + oid + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", areaType='" + areaType + '\'' +
                '}';
    }
}
