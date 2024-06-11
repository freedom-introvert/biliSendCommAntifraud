package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import androidx.annotation.NonNull;

public class MartialLawCommentArea extends CommentArea {
    public static final String DISPOSAL_METHOD_SHADOW_BAN = "shadowBan";
    public static final String DISPOSAL_METHOD_QUICK_DELETE = "quickDelete";
    public String title,defaultDisposalMethod,up;
    public byte[] coverImageData;

    public MartialLawCommentArea(CommentArea commentArea, String defaultDisposalMethod,String title,String up,byte[] coverImageData) {
        this(String.valueOf(commentArea.oid),commentArea.sourceId,commentArea.type,defaultDisposalMethod,title,up,coverImageData);
    }
    public MartialLawCommentArea(String oid, String sourceId,int areaType, String defaultDisposalMethod,String title,String up,byte[] coverImageData) {
        super(Long.parseLong(oid),sourceId,areaType);
        this.up = up;
        this.title = title;
        this.defaultDisposalMethod = defaultDisposalMethod;
        this.coverImageData = coverImageData;
    }
    public String[] toStringArrays(){
        return new String[]{String.valueOf(oid),sourceId, String.valueOf(type),defaultDisposalMethod,title,up,null};
    }

    @NonNull
    @Override
    public String toString() {
        return "MartialLawCommentArea{" +
                "oid='" + oid + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", areaType=" + type +
                ", title='" + title + '\'' +
                ", defaultDisposalMethod='" + defaultDisposalMethod + '\'' +
                ", up='" + up + '\'' +
                '}';
    }
}
