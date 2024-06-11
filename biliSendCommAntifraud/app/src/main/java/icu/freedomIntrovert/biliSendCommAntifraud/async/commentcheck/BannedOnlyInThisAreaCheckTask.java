package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class BannedOnlyInThisAreaCheckTask extends CommentOperateTask<BannedOnlyInThisAreaCheckTask.EventHandler>{

    CommentArea yourCommentArea;

    public BannedOnlyInThisAreaCheckTask(EventHandler handle, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper statisticsDB, Comment comment, CommentArea yourCommentArea) {
        super(handle, commentManipulator, config, statisticsDB, comment);
        this.yourCommentArea = yourCommentArea;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        //在自己评论区发送内容一样的评论
        eventHandler.sendEventMessage(EventHandler.WHAT_ON_COMMENT_SENT_TO_YOUR_AREA,yourCommentArea.sourceId);
        CommentAddResult commentAddResult = commentManipulator.sendComment(comment.comment, 0, 0, yourCommentArea, false);
        long testCommentRpid = commentAddResult.rpid;
        sleep(config.getWaitTime());
        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ON_START_CHECK);
        //在自己评论区寻找此条测试评论
        if (commentManipulator.findComment(yourCommentArea, testCommentRpid, 0) != null) {
            commentManipulator.deleteComment(comment.commentArea,testCommentRpid,false);
            if (config.getRecordeHistoryIsEnable()){
                statisticsDB.updateCheckedArea(comment.rpid,HistoryComment.CHECKED_ONLY_BANNED_IN_THIS_AREA);
            }
            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_ONLY_BANNED_IN_THIS_AREA);
        } else {
            commentManipulator.deleteComment(yourCommentArea,testCommentRpid,false);
            if (config.getRecordeHistoryIsEnable()) {
                statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
            }
            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_BANNED_IN_YOUR_AREA);
        }
    }

    public static abstract class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_ON_COMMENT_SENT_TO_YOUR_AREA = 1;
        public static final int WHAT_ON_START_CHECK = 2;
        public static final int WHAT_THEN_ONLY_BANNED_IN_THIS_AREA = 10;
        public static final int WHAT_THEN_BANNED_IN_YOUR_AREA = 11;


        /*
        void onCommentSent(String yourCommentArea);

        void onStartCheck();

        void thenOnlyBannedInThisArea();

        void thenBannedInYourArea();
         */
        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
