package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

public class MartialLawCommentArea extends CommentArea {
    public static final String DISPOSAL_METHOD_SHADOW_BAN = BannedCommentBean.BANNED_TYPE_SHADOW_BAN;
    public static final String DISPOSAL_METHOD_QUICK_DELETE = BannedCommentBean.BANNED_TYPE_QUICK_DELETE;
    public String title,defaultDisposalMethod,up;
    public byte[] coverImageData;

    public MartialLawCommentArea(CommentArea commentArea, String defaultDisposalMethod,String title,String up,byte[] coverImageData) {
        this(String.valueOf(commentArea.oid),commentArea.sourceId,commentArea.areaType,defaultDisposalMethod,title,up,coverImageData);
    }
    public MartialLawCommentArea(String oid, String sourceId,int areaType, String defaultDisposalMethod,String title,String up,byte[] coverImageData) {
        super(Long.parseLong(oid),sourceId,areaType);
        this.up = up;
        this.title = title;
        this.defaultDisposalMethod = defaultDisposalMethod;
        this.coverImageData = coverImageData;
    }
    public String[] toStringArrays(){
        return new String[]{String.valueOf(oid),sourceId, String.valueOf(areaType),defaultDisposalMethod,title,up,null};
    }

    @Override
    public String toString() {
        return "MartialLawCommentArea{" +
                "oid='" + oid + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", areaType=" + areaType +
                ", title='" + title + '\'' +
                ", defaultDisposalMethod='" + defaultDisposalMethod + '\'' +
                ", up='" + up + '\'' +
                '}';
    }
}
