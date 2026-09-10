package icu.freedomIntrovert.biliSendCommAntifraud.async;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentOperateTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class SendCommentTask extends CommentOperateTask<SendCommentTask.EventHandler> {
    public final String commentText;
    public final String commentAreaText;
    public final long root;
    public final long parent;
    private final Account account;

    public SendCommentTask(Context context, String commentText, String commentAreaText, Account account, long root, long parent,EventHandler handle) {
        super(handle, context);
        this.commentText = commentText;
        this.commentAreaText = commentAreaText;
        this.account = account;
        this.root = root;
        this.parent = parent;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        CommentArea commentArea = commentManipulator.matchCommentArea(commentAreaText, account);
        if (commentArea == null){
            handler.onCommentAreaMoMatch();
            return;
        }
        try {
            CommentAddResult commentAddResult = commentManipulator.sendComment(commentText, parent, root, commentArea, account);
            handler.onSent(commentArea,commentAddResult);
        } catch (BiliBiliApiException e){
            if (e.code == GeneralResponse.CODE_COMMENT_CONTAIN_SENSITIVE){
                handler.onCommentContainSensitive(commentArea,commentText,account.uid,e);
            } else {
                throw e;
            }
        }
    }

    public interface EventHandler extends BaseEventHandler{

        void onCommentAreaMoMatch();
        void onSent(CommentArea commentArea,CommentAddResult commentAddResult);

        void onCommentContainSensitive(CommentArea commentArea, String commentText,long uid,BiliBiliApiException e);
    }


}
