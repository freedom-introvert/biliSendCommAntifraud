package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import java.util.Date;

import icu.freedomIntrovert.async.BackstageTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.async.CookieFailedException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;

public class ReviewCommentStatusTask extends BackstageTask<ReviewCommentStatusTask.EventHandler> {
    private CommentManipulator commentManipulator;
    private StatisticsDBOpenHelper statisticsDB;
    private HistoryComment historyComment;

    public ReviewCommentStatusTask(EventHandler handle, CommentManipulator commentManipulator, StatisticsDBOpenHelper statisticsDB, HistoryComment historyComment) {
        super(handle);
        this.commentManipulator = commentManipulator;
        this.statisticsDB = statisticsDB;
        this.historyComment = historyComment;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        if (!commentManipulator.checkCookieNotFailed()){
            eventHandler.sendError(new CookieFailedException());
            return;
        }
        CommentArea commentArea = historyComment.commentArea;
        long rpid = historyComment.rpid;
        GeneralResponse<CommentReplyPage> resp = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1,false);
        OkHttpUtil.respNotNull(resp);
        if (resp.isSuccess()) {
            BiliComment rootComment = resp.data.root;
            //判断该评论是否为根评论，不是楼中楼回复的评论
            if (rootComment.rpid == rpid) {
                GeneralResponse<CommentReplyPage> resp1 = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1);
                OkHttpUtil.respNotNull(resp1);
                if (resp1.isSuccess()) {
                    BiliComment foundComment = commentManipulator.findCommentUsingSeekRpid(historyComment, false);
                    if (foundComment == null){
                        //评论疑似审核中
                        statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_UNDER_REVIEW,rootComment.like, rootComment.rcount, new Date());
                        eventHandler.sendEventMessage(EventHandler.WHAT_UNDER_REVIEW,rootComment);
                    } else {
                        if (rootComment.invisible){
                            //评论invisible
                            statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_INVISIBLE,rootComment.like, rootComment.rcount, new Date());
                            eventHandler.sendEventMessage(EventHandler.WHAT_INVISIBLE,rootComment);
                        } else {
                            //评论正常
                            statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_NORMAL,rootComment.like, rootComment.rcount, new Date());
                            eventHandler.sendEventMessage(EventHandler.WHAT_OK,rootComment);
                        }
                    }
                } else {
                    //评论ShadowBan
                    statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_SHADOW_BAN,rootComment.like, rootComment.rcount, new Date());
                    eventHandler.sendEventMessage(EventHandler.WHAT_SHADOW_BANNED,rootComment);
                }
            } else {
                //楼中楼评论
                GeneralResponse<CommentReplyPage> body = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1);
                OkHttpUtil.respNotNull(body);
                //前面评论回复列表是带cookie获取的，如果是你自己发的，shadowBan情况下可以获取成功，但无账号会“已经被删除了”
                if (body.isSuccess()) {
                    BiliComment foundReply = commentManipulator.findCommentFromCommentReplyArea(commentArea,
                            rpid, rootComment.rpid, false);
                    if (foundReply != null) {
                        if (rootComment.invisible) {
                            //回复评论invisible
                            statisticsDB.updateHistoryCommentStates(foundReply.rpid, HistoryComment.STATE_INVISIBLE,foundReply.like, foundReply.rcount, new Date());
                            eventHandler.sendEventMessage(EventHandler.WHAT_INVISIBLE,foundReply);
                        } else {
                            //回复评论正常
                            statisticsDB.updateHistoryCommentStates(foundReply.rpid, HistoryComment.STATE_NORMAL,foundReply.like, foundReply.rcount, new Date());
                            eventHandler.sendEventMessage(EventHandler.WHAT_REPLY_OK,foundReply);
                        }
                    } else {
                        BiliComment foundReplyHasAcc = commentManipulator.findCommentFromCommentReplyArea(commentArea,
                                rpid, rootComment.rpid, true);
                        if (foundReplyHasAcc != null) {
                            //回复评论ShadowBan
                            statisticsDB.updateHistoryCommentStates(foundReplyHasAcc.rpid, HistoryComment.STATE_SHADOW_BAN,
                                    foundReplyHasAcc.like, foundReplyHasAcc.rcount, new Date());
                            eventHandler.sendEventMessage(EventHandler.WHAT_SHADOW_BANNED,foundReplyHasAcc);
                        } else {
                            //回复评论被删除
                            statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_DELETED,
                                    historyComment.like, historyComment.replyCount, new Date());
                            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_DELETED);
                        }
                    }
                } else {
                    //自己所发布的评论被ShadowBan等，这条回复评论被连累
                    statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_SHADOW_BAN,
                            historyComment.like, historyComment.replyCount, new Date());
                    eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ROOT_COMMENT_IS_SHADOW_BAN);
                }
            }
        } else if (resp.code == CommentAddResult.CODE_DELETED){
            //评论被删除
            statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_DELETED,
                    historyComment.like, historyComment.replyCount, new Date());
            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_DELETED);
        } else {
            eventHandler.sendError(new BiliBiliApiException(resp,"获取评论信息失败"));
        }
    }

    public static abstract class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_ROOT_COMMENT_IS_SHADOW_BAN = -1;
        public static final int WHAT_ON_PAGE_TURN_FOR_NO_ACC_REPLY = 1;
        public static final int WHAT_ON_PAGE_TURN_FOR_HAS_ACC_REPLY = 2;
        public static final int WHAT_OK = 10;
        public static final int WHAT_SHADOW_BANNED = 11;
        public static final int WHAT_DELETED = 12;
        public static final int WHAT_INVISIBLE = 13;
        public static final int WHAT_UNDER_REVIEW = 14;
        public static final int WHAT_REPLY_OK = 20;

        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
