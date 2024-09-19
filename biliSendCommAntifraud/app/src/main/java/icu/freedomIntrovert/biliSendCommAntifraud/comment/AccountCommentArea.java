package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class AccountCommentArea extends CommentArea {
    public String commentAreaLocation;
    public AccountCommentArea(CommentArea commentArea,String commentAreaLocation){
        super(commentArea.oid,commentArea.sourceId,commentArea.type);
        this.commentAreaLocation = commentAreaLocation;
    }
    public AccountCommentArea(long oid, String sourceId, int type,String commentAreaLocation) {
        super(oid, sourceId, type);
        this.commentAreaLocation = commentAreaLocation;
    }
}
