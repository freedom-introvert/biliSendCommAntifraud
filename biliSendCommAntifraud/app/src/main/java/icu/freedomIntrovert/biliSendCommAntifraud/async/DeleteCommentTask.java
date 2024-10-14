package icu.freedomIntrovert.biliSendCommAntifraud.async;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentOperateTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;

public class DeleteCommentTask extends CommentOperateTask<DeleteCommentTask.EventHandler> {
    public final Comment comment;

    public DeleteCommentTask(Context context, Comment comment,EventHandler handle) {
        super(handle, context);
        this.comment = comment;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        Account account = accountManger.getAccount(comment.uid);
        if (account == null){
            handler.onAccountNotFound(comment.uid);
            return;
        }
        commentManipulator.deleteComment(comment.commentArea,comment.rpid,account);
        handler.onSuccess();
    }

    public interface EventHandler extends BaseEventHandler{
        void onAccountNotFound(long uid);

        void onSuccess();
    }
}
