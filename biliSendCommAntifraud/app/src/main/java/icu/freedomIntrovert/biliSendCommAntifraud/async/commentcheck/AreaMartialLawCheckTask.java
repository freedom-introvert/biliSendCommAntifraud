package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class AreaMartialLawCheckTask extends CommentOperateTask<AreaMartialLawCheckTask.EventHandler> {
    Account account;
    Comment comment;

    public AreaMartialLawCheckTask(Context context, Account account, Comment comment,EventHandler handle) {
        super(handle, context);
        this.account = account;
        this.comment = comment;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        CommentArea commentArea = comment.commentArea;

        String randomComment = randomComments.getRandomComment(commentArea);
        CommentAddResult commentAddResult = commentManipulator.sendComment(randomComment, 0, 0/*不知道有没有楼中楼域的戒严，默认没有吧*/, commentArea, account);
        int waitTime = (int) config.getWaitTime();
        handler.onTestCommentSent(randomComment,waitTime);
        long testCommentRpid = commentAddResult.rpid;
        for (int i = 0; i < waitTime; i+=10) {
            Thread.sleep(10);
            handler.onWaitProgress(i);
        }
        handler.onStartCheck();
        if (commentManipulator.findComment(commentAddResult.reply, account) != null) {
            commentManipulator.deleteComment(commentArea, testCommentRpid, account);
            statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_NOT_MARTIAL_LAW);
            handler.onAreaOk();
        } else {
            if (config.getRecordeHistoryIsEnable()) {
                statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_MARTIAL_LAW);
                statisticsDB.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(commentArea, testCommentRpid, account));
            }
            commentManipulator.deleteComment(commentArea, testCommentRpid, account);
            handler.onMartialLaw();
        }
    }

    public interface EventHandler extends BaseEventHandler{
        void onTestCommentSent(String testComment, int maxProgress);
        void onWaitProgress(int progress);
        void onStartCheck();
        void onAreaOk();
        void onMartialLaw();
    }
}
