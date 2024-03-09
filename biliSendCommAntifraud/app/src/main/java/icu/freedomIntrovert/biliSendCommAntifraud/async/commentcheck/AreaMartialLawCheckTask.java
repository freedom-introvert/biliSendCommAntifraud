package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import icu.freedomIntrovert.async.EventMessage;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;

public class AreaMartialLawCheckTask extends CommentOperateTask<AreaMartialLawCheckTask.EventHandler> {


    private final CommentUtil commentUtil;
    private final boolean isDeputyAccount;

    public AreaMartialLawCheckTask(EventHandler handle, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper statisticsDB, Comment comment, CommentUtil commentUtil, boolean isDeputyAccount) {
        super(handle, commentManipulator, config, statisticsDB, comment);
        this.commentUtil = commentUtil;
        this.isDeputyAccount = isDeputyAccount;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        CommentArea commentArea = comment.commentArea;
        String randomComment = commentUtil.getRandomComment(commentArea);
        GeneralResponse<CommentAddResult> response = commentManipulator.sendComment(randomComment, 0, 0, commentArea,isDeputyAccount).execute().body();
        OkHttpUtil.respNotNull(response);
        if (response.isSuccess()) {
            eventHandler.sendEventMessage(new EventMessage(EventHandler.WHAT_ON_TEST_COMMENT_SENT,randomComment));
            long testCommentRpid = response.data.rpid;
            sleep(config.getWaitTime());
            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ON_START_CHECK);
            if (commentManipulator.findComment(commentArea, response.data.rpid, 0) != null) {
                commentManipulator.deleteComment(commentArea, testCommentRpid,isDeputyAccount);
                statisticsDB.updateCheckedArea(comment.rpid,HistoryComment.CHECKED_NOT_MARTIAL_LAW);
                eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_AREA_OK);
            } else {
                if (config.getRecordeHistoryIsEnable()) {
                    statisticsDB.updateCheckedArea(comment.rpid, HistoryComment.CHECKED_MARTIAL_LAW);
                    statisticsDB.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(commentArea, testCommentRpid, isDeputyAccount));
                }
                commentManipulator.deleteComment(commentArea, testCommentRpid,isDeputyAccount);
                eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_MARTIAL_LAW);
            }
        } else {
            eventHandler.sendError(new BiliBiliApiException(response,"发送评论失败"));
        }
    }

    public static abstract class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_ON_TEST_COMMENT_SENT = 1;
        public static final int WHAT_ON_START_CHECK = 2;
        public static final int WHAT_THEN_AREA_OK = 10;
        public static final int WHAT_THEN_MARTIAL_LAW = 11;

        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
