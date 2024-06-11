package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import icu.freedomIntrovert.async.BackstageTaskByMVP;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
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

public class BatchReviewCommentStatusTask extends BackstageTaskByMVP<BatchReviewCommentStatusTask.EventHandler> {

    private final AtomicBoolean isBreak = new AtomicBoolean(false);
    private final CommentManipulator commentManipulator;
    private final StatisticsDBOpenHelper statisticsDB;
    private final List<HistoryComment> comments;


    public BatchReviewCommentStatusTask(CommentManipulator commentManipulator, StatisticsDBOpenHelper statisticsDB, List<HistoryComment> comments, EventHandler uiHandler) {
        super(uiHandler);
        this.commentManipulator = commentManipulator;
        this.statisticsDB = statisticsDB;
        this.comments = comments;
    }

    @Override
    protected void onStart(EventHandler eventHandlerProxy) throws Throwable {
        for (HistoryComment comment : comments) {
            if (isBreak.get()) {
                System.out.println("任务被中断");
                break;
            }
            eventHandlerProxy.onStartCheck(comment);
            eventHandlerProxy.onCheckOver(checkComment(comment));
        }
    }

    public void breakRun(){
        isBreak.set(true);
    }

    public String checkComment(HistoryComment historyComment) throws IOException, CookieFailedException, BiliBiliApiException {
        if (!commentManipulator.checkCookieNotFailed()) {
            throw new CookieFailedException();
        }

        CommentArea commentArea = historyComment.commentArea;

        long rpid = historyComment.rpid;
        GeneralResponse<CommentReplyPage> resp = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1, false);
        OkHttpUtil.respNotNull(resp);
        if (resp.isSuccess()) {
            BiliComment rootComment = resp.data.root;
            //判断该评论是否为根评论，不是楼中楼回复的评论
            if (rootComment.rpid == rpid) {
                GeneralResponse<CommentReplyPage> resp1 = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1);
                OkHttpUtil.respNotNull(resp1);
                if (resp1.isSuccess()) {
                    BiliComment foundComment = commentManipulator.findCommentUsingSeekRpid(historyComment, false);
                    if (foundComment == null) {
                        //评论疑似审核中
                        statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_UNDER_REVIEW, rootComment.like, rootComment.rcount, new Date());
                        return HistoryComment.STATE_UNDER_REVIEW;
                    } else {
                        if (rootComment.invisible) {
                            //评论invisible
                            statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_INVISIBLE, rootComment.like, rootComment.rcount, new Date());
                            return HistoryComment.STATE_INVISIBLE;
                        } else {
                            //评论正常
                            statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_NORMAL, rootComment.like, rootComment.rcount, new Date());
                            return HistoryComment.STATE_NORMAL;
                        }
                    }
                } else {
                    //评论ShadowBan
                    statisticsDB.updateHistoryCommentStates(rootComment.rpid, HistoryComment.STATE_SHADOW_BAN, rootComment.like, rootComment.rcount, new Date());
                    return HistoryComment.STATE_SHADOW_BAN;
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
                            statisticsDB.updateHistoryCommentStates(foundReply.rpid, HistoryComment.STATE_INVISIBLE, foundReply.like, foundReply.rcount, new Date());
                            return HistoryComment.STATE_INVISIBLE;
                        } else {
                            //回复评论正常
                            statisticsDB.updateHistoryCommentStates(foundReply.rpid, HistoryComment.STATE_NORMAL, foundReply.like, foundReply.rcount, new Date());
                            return HistoryComment.STATE_NORMAL;
                        }
                    } else {
                        BiliComment foundReplyHasAcc = commentManipulator.findCommentFromCommentReplyArea(commentArea,
                                rpid, rootComment.rpid, true);
                        if (foundReplyHasAcc != null) {
                            //回复评论ShadowBan
                            statisticsDB.updateHistoryCommentStates(foundReplyHasAcc.rpid, HistoryComment.STATE_SHADOW_BAN,
                                    foundReplyHasAcc.like, foundReplyHasAcc.rcount, new Date());
                            return HistoryComment.STATE_SHADOW_BAN;
                        } else {
                            //回复评论被删除
                            statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_DELETED,
                                    historyComment.like, historyComment.replyCount, new Date());
                            return HistoryComment.STATE_DELETED;
                        }
                    }
                } else {
                    //自己所发布的评论被ShadowBan等，这条回复评论被连累
                    statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_SHADOW_BAN,
                            historyComment.like, historyComment.replyCount, new Date());
                    return HistoryComment.STATE_SHADOW_BAN;
                }
            }
        } else if (resp.code == CommentAddResult.CODE_DELETED) {
            //评论被删除
            statisticsDB.updateHistoryCommentStates(historyComment.rpid, HistoryComment.STATE_DELETED,
                    historyComment.like, historyComment.replyCount, new Date());
            return HistoryComment.STATE_DELETED;
        } else if (resp.code == 12002) {
            return "评论区已关闭";
        } else {
            throw new BiliBiliApiException(resp, "获取评论信息失败");
        }
    }


    public interface EventHandler extends BaseEventHandler {
        void onStartCheck(HistoryComment checkingComment);

        void onCheckOver(String newStatus);

    }
}
