package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

public class CommentScanResult {
    public boolean isExists;
    public boolean isInvisible;

    public CommentScanResult(boolean isExists, boolean isInvisible) {
        this.isExists = isExists;
        this.isInvisible = isInvisible;
    }
}
