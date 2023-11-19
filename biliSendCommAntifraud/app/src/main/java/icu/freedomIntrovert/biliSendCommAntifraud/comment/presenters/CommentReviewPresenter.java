package icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters;

import android.os.Handler;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import icu.freedomIntrovert.biliSendCommAntifraud.NetworkCallBack;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReply;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;

public class CommentReviewPresenter {
    private Handler handler;
    CommentManipulator commentManipulator;
    Executor executor;

    public CommentReviewPresenter(Handler handler, CommentManipulator commentManipulator) {
        this.handler = handler;
        this.commentManipulator = commentManipulator;
        executor = Executors.newSingleThreadExecutor();
    }

    public void reviewStatus(CommentArea commentArea, long rpid, ReviewStatusCallBack callBack) {
        executor.execute(() -> {
            try {
                if (!commentManipulator.checkCookieNotFailed()){
                    handler.post(callBack::onCookieFiled);
                    return;
                }
                GeneralResponse<CommentReply> resp = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1).execute().body();
                OkHttpUtil.respNotNull(resp);
                if (resp.isSuccess()) {
                    BiliComment rootComment = resp.data.root;
                    //判断该评论是否为根评论，不是楼中楼回复的评论
                    if (rootComment.rpid == rpid) {
                        GeneralResponse<CommentReply> resp1 = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1).execute().body();
                        OkHttpUtil.respNotNull(resp1);
                        if (resp1.isSuccess()) {
                            if (rootComment.invisible){
                                handler.post(() -> callBack.invisible(rootComment.like, rootComment.rcount));
                            } else {
                                handler.post(() -> callBack.ok(rootComment.like, rootComment.rcount));
                            }
                        } else {
                            handler.post(() -> callBack.shadowBanned(rootComment.like, rootComment.rcount));
                        }
                    } else {
                        GeneralResponse<CommentReply> body = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1).execute().body();
                        OkHttpUtil.respNotNull(body);
                        //前面评论回复列表是带cookie获取的，如果是你自己发的，shadowBan情况下可以获取成功，但无账号会“已经被删除了”
                        if (body.isSuccess()) {
                            BiliComment foundReply = commentManipulator.findCommentFromCommentReplyArea(commentArea, rpid, rootComment.rpid, false, page -> handler.post(() -> callBack.onPageTurnForNoAccReply(page)));
                            if (foundReply != null) {
                                if (rootComment.invisible) {
                                    handler.post(() -> callBack.invisible(foundReply.like, foundReply.rcount));
                                } else {
                                    handler.post(() -> callBack.replyOk(foundReply.like, foundReply.rcount));
                                }
                            } else {
                                BiliComment foundReplyHasAcc = commentManipulator.findCommentFromCommentReplyArea(commentArea, rpid, rootComment.rpid, true, page -> handler.post(() -> callBack.onPageTurnForHasAccReply(page)));
                                if (foundReplyHasAcc != null) {
                                    handler.post(() -> callBack.shadowBanned(foundReplyHasAcc.like, foundReplyHasAcc.rcount));
                                } else {
                                    handler.post(callBack::deleted);
                                }
                            }
                        } else {
                            handler.post(callBack::rootCommentIsShadowBan);
                        }
                    }
                } else if (resp.code == CommentAddResult.CODE_DELETED){
                    handler.post(callBack::deleted);
                } else {
                    handler.post(() -> callBack.onCodeError(resp.code,resp.message));
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callBack.onNetworkError(e));
            }
        });
    }

    public interface ReviewStatusCallBack extends NetworkCallBack {
        void onCookieFiled();
        void deleted();

        void shadowBanned(int like, int replyCount);

        void ok(int like, int replyCount);

        void onPageTurnForNoAccReply(int pn);

        void onPageTurnForHasAccReply(int pn);

        void replyOk(int like, int replyCount);

        void rootCommentIsShadowBan();

        void invisible(int like, int replyCount);

        void onCodeError(int code,String message);
    }

    //评论区搜遍
    public void searchThroughoutTheCommentArea(CommentArea commentArea, long rpid, SearchTTCommAreaCallback callback) {
        executor.execute(() -> {
            try {
                BiliComment comment = commentManipulator.findThisCommentFromEntireCommentArea(commentArea, rpid, false, page -> handler.post(() -> callback.onPageTurn(page)));
                if (comment != null){
                    handler.post(callback::found);
                } else {
                    handler.post(callback::notFound);
                }
            } catch (IOException e){
                e.printStackTrace();
                handler.post(() -> callback.onNetworkError(e));
            }
        });
    }

    public interface SearchTTCommAreaCallback extends NetworkCallBack{
        void onPageTurn(int pn);
        void found();
        void notFound();
    }
//    public void updateCommentState(HistoryComment historyComment,UpdateCommentStatusCallBack callBack){
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//    }
//
//    public interface UpdateCommentStatusCallBack extends NetworkCallBack{
//        void deleted();
//        void shadowBanned(int like,int replyCount);
//        void ok(int like,int replyCount);
//    }


}
