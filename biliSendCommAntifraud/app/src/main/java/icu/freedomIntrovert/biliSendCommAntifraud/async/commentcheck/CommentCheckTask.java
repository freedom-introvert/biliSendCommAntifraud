package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import java.util.Date;

import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.async.CookieFailedException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;

public class CommentCheckTask extends CommentOperateTask<CommentCheckTask.EventHandler> {

    private final String testCommentText;

    public CommentCheckTask(EventHandler handle, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper statisticsDB, Comment comment, String testCommentText) {
        super(handle, commentManipulator, config, statisticsDB, comment);
        this.testCommentText = testCommentText;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        CommentArea commentArea = comment.commentArea;
        HistoryComment historyComment = new HistoryComment(comment);
        historyComment.lastCheckDate = new Date();
        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ON_START_COMMENT_CHECK);
        if (!commentManipulator.checkCookieNotFailed()) {
            eventHandler.sendError(new CookieFailedException());
            return;
        }
        BiliComment biliComment = commentManipulator.findComment(comment.commentArea, comment.rpid, comment.root);
        if (biliComment != null) {
            //判断是否被标记为invisible，使其在前端不可见
            if (biliComment.invisible) {
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_INVISIBLE);
                insertHistoryComment(historyComment);
                eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_INVISIBLE);
            } else {
                //评论正常
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_NORMAL);
                insertHistoryComment(historyComment);
                eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_COMMENT_OK);
            }
        } else {
            //若不是评论回复
            if (comment.root == 0) {
                eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ON_COMMENT_NOT_FOUND);
                GeneralResponse<CommentReplyPage> response = commentManipulator.getCommentReplyHasAccount(commentArea, comment.rpid, 1, false);
                OkHttpUtil.respNotNull(response);
                if (response.code == CommentAddResult.CODE_SUCCESS) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //评论shadowBan或者疑似审核中
                    GeneralResponse<CommentReplyPage> noACResp = commentManipulator.getCommentReplyNoAccount(commentArea, comment.rpid, 0);
                    OkHttpUtil.respNotNull(noACResp);
                    if (noACResp.isSuccess()) {
                        //找不到评论，有账号能获取评论列表，无账号也可以获取评论列表，这种情况大半申诉说无可申诉，除非你是UP发评论被shadowBan
                        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_UNDER_REVIEW);
                        insertHistoryComment(historyComment);
                        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_UNDER_REVIEW);
                    } else if (noACResp.code == CommentAddResult.CODE_DELETED) {
                        //评论shadowBan
                        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SHADOW_BAN);
                        insertHistoryComment(historyComment);
                        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_SHADOW_BAN);
                    } else {
                        eventHandler.sendError(new BiliBiliApiException(noACResp, null));
                    }
                } else if (response.code == CommentAddResult.CODE_DELETED) {
                    //再尝试对评论进行回复，看看是否应session过期导致变成了游客视角
                    GeneralResponse<CommentAddResult> response1 = commentManipulator.getSendCommentCall(testCommentText, comment.rpid, comment.root, commentArea, false).execute().body();
                    OkHttpUtil.respNotNull(response1);
                    if (response1.isSuccess()) {
                        //应该不存在有账号获取评论列表被删除了还能回复的吧:(
                        sleep(config.getWaitTime());
                        commentManipulator.deleteComment(comment.commentArea, comment.rpid,false);
                        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_SHADOW_BAN);
                    } else if (response1.code == CommentAddResult.CODE_DELETED) {
                        //如果获取的评论列表提示被删除和回复评论提示也被删除才算秒删
                        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_DELETED);
                        insertHistoryComment(historyComment);
                        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_DELETED);
                    } else {
                        //登录信息过期或其他异常
                        eventHandler.sendError(new BiliBiliApiException(response1.code, response1.message, null));
                    }
                } else {
                    eventHandler.sendError(new BiliBiliApiException(response.code, response.message, null));
                }
                //是评论回复的处理方式
            } else {
                BiliComment foundReply = commentManipulator.findCommentFromCommentReplyArea(commentArea,
                        comment.rpid, comment.root, true);
                if (foundReply != null) {
                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SHADOW_BAN);
                    insertHistoryComment(historyComment);
                    eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_SHADOW_BAN);
                } else {
                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_DELETED);
                    insertHistoryComment(historyComment);
                    eventHandler.sendEmptyEventMessage(EventHandler.WHAT_THEN_DELETED);
                }
            }
        }
    }

    public abstract static class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_ON_START_COMMENT_CHECK = 1;
        public static final int WHAT_ON_COMMENT_NOT_FOUND = 2;
        public static final int WHAT_ON_PAGE_TURN_FOR_HAS_ACC_REPLY = 3;
        public static final int WHAT_THEN_COMMENT_OK = 100;
        public static final int WHAT_THEN_SHADOW_BAN = 101;
        public static final int WHAT_THEN_DELETED = 102;
        public static final int WHAT_THEN_UNDER_REVIEW = 103;
        public static final int WHAT_THEN_INVISIBLE = 110;

        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
