package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import icu.freedomIntrovert.async.BackstageTask;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressTimer;

public class ResendCommentTask extends BackstageTask<ResendCommentTask.EventHandler> {
    private final CommentManipulator commentManipulator;
    private final Config config;
    private final String newCommentText;
    private final HistoryComment historyComment;

    public ResendCommentTask(EventHandler handle, CommentManipulator commentManipulator, Config config, String newCommentText, HistoryComment historyComment) {
        super(handle);
        this.commentManipulator = commentManipulator;
        this.config = config;
        this.newCommentText = newCommentText;
        this.historyComment = historyComment;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        GeneralResponse<CommentAddResult> body = commentManipulator.sendComment(newCommentText, historyComment.parent, historyComment.root, historyComment.commentArea,false).execute().body();
        OkHttpUtil.respNotNull(body);
        long waitTime = config.getWaitTime();
        eventHandler.sendEventMessage(EventHandler.WHAT_ON_SEND_SUCCESS_AND_SLEEP,waitTime);
        if (body.isSuccess()) {
            new ProgressTimer(waitTime, ProgressBarDialog.DEFAULT_MAX_PROGRESS, (progress, sleepSeg) -> eventHandler.sendEventMessage(EventHandler.WHAT_ON_NEW_PROGRESS,progress, sleepSeg,waitTime)).start();
            eventHandler.sendEventMessage(EventHandler.WHAT_ON_RESENT_COMMENT,body.data);
        } else {
            eventHandler.sendError(new BiliBiliApiException(body,"发送评论失败！"));
        }
    }

    public static abstract class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_ON_SEND_SUCCESS_AND_SLEEP = 1;
        public static final int WHAT_ON_NEW_PROGRESS = 2;
        public static final int WHAT_ON_RESENT_COMMENT = 10;


        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
