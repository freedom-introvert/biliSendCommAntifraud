package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class BannedOnlyInThisAreaCheckTask extends CommentOperateTask<BannedOnlyInThisAreaCheckTask.EventHandler> {

    Comment comment;
    Account account;

    public BannedOnlyInThisAreaCheckTask(Context context, Comment comment,Account account,EventHandler handle) {
        super(handle, context);
        this.comment = comment;
        this.account = account;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        //在自己评论区发送内容一样的评论
        CommentArea commentArea = account.accountCommentArea;
        if (commentArea == null){
            handler.onAccountCommentAreaNotSet(account);
            return;
        }
        int waitTime = (int) config.getWaitTime();
        handler.onCommentSentToYourArea(commentArea,waitTime);
        CommentAddResult commentAddResult = commentManipulator.sendComment(comment.comment, 0, 0, commentArea, account);
        long testCommentRpid = commentAddResult.rpid;
        Thread.sleep(config.getWaitTime());
        handler.onStartCheck();
        //在自己评论区寻找此条测试评论
        if (commentManipulator.findComment(comment,account) != null) {
            commentManipulator.deleteComment(commentArea,testCommentRpid,account);
            if (config.getRecordeHistoryIsEnable()){
                statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_ONLY_BANNED_IN_THIS_AREA);
            }
            handler.thenOnlyBannedInThisArea();
        } else {
            commentManipulator.deleteComment(commentArea,testCommentRpid,account);
            if (config.getRecordeHistoryIsEnable()) {
                statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
            }
            handler.thenBannedInYourArea();
        }
    }

    public interface EventHandler extends BaseEventHandler{
        void onAccountCommentAreaNotSet(Account account);

        void onCommentSentToYourArea(CommentArea commentArea, int waitTime);

        void onStartCheck();

        void thenOnlyBannedInThisArea();

        void thenBannedInYourArea();
    }
}
