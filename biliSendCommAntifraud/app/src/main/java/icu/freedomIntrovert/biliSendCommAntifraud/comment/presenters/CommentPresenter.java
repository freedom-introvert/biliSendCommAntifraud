package icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters;

import android.os.Handler;
import android.text.SpannableStringBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliApiService;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReply;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.ServiceGenerator;
import retrofit2.Call;

public class CommentPresenter {
    private Handler handler;
    public CommentManipulator commentManipulator;
    public StatisticsDBOpenHelper statisticsDBOpenHelper;
    private boolean enableStatistics;
    public long waitTime;
    private Executor executor;
    public BiliApiService biliApiService;

    public CommentPresenter(Handler handler, CommentManipulator manipulator, StatisticsDBOpenHelper statisticsDBOpenHelper, long waitTime, boolean enableStatistics) {
        this.handler = handler;
        this.commentManipulator = manipulator;
        this.statisticsDBOpenHelper = statisticsDBOpenHelper;
        executor = Executors.newSingleThreadExecutor();
        biliApiService = ServiceGenerator.createService(BiliApiService.class);
        this.waitTime = waitTime;
        this.enableStatistics = enableStatistics;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public void setEnableStatistics(boolean enableStatistics) {
        this.enableStatistics = enableStatistics;
    }

    public void matchToArea(String areaText, MatchToAreaCallBack callBack) {
        executor.execute(() -> {
            try {
                CommentArea commentArea = commentManipulator.matchCommentArea(areaText);
                handler.post(() -> callBack.onMatchedArea(commentArea));
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callBack.onNetworkError(e));
            }
        });
    }

    public interface MatchToAreaCallBack extends NetworkCallBack {
        void onMatchedArea(CommentArea commentArea);
    }


    public Call<Void> deleteComment(CommentArea commentArea, long rpid) {
        return biliApiService.deleteComment(commentManipulator.getCookie(), commentManipulator.getCsrfFromCookie(), commentArea.oid, commentArea.areaType, rpid);
    }

    public void checkCommentStatus(CommentArea commentArea, String mainComment, String testComment, long rpid, long parent, long root, CheckCommentStatusCallBack callBack) {
        executor.execute(() -> {
            try {
                try {
                    handler.post(() -> callBack.onSleeping(waitTime));
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(callBack::onStartCheckComment);
                CommentScanResult commentScanResult = commentManipulator.scanComment(commentArea, rpid, root);
                if (commentScanResult.isExists) {
                    //判断是否被标记为invisible，使其在前端不可见
                    if (commentScanResult.isInvisible){
                        insertBannedComment(new BannedCommentBean(commentArea, rpid, mainComment, BannedCommentBean.BANNED_TYPE_INVISIBLE, new Date(), BannedCommentBean.CHECKED_NO_CHECK));
                        handler.post(callBack::thenInvisible);
                    } else {
                        handler.post(callBack::thenOk);
                    }
                } else {
                    handler.post(() -> callBack.onCommentNotFound(testComment));
                    GeneralResponse<CommentReply> response = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1).execute().body();
                    if (response.code == CommentAddResult.CODE_SUCCESS) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //deleteComment(commentArea, response.data.rpid).execute();
                        insertBannedComment(new BannedCommentBean(commentArea, rpid, mainComment, BannedCommentBean.BANNED_TYPE_SHADOW_BAN, new Date(), BannedCommentBean.CHECKED_NO_CHECK));
                        handler.post(callBack::thenShadowBan);
                    } else if (response.code == CommentAddResult.CODE_DELETED) {
                        //再尝试对评论进行回复，看看是否应session过期导致变成了游客视角
                        GeneralResponse<CommentAddResult> response1 = commentManipulator.sendComment(testComment, rpid, root, commentArea).execute().body();
                        if (response1.isSuccess()) {
                            //应该不存在有账号获取评论列表被删除了还能回复的吧:(
                            sleep(waitTime);
                            deleteComment(commentArea, response1.data.rpid).execute();
                            handler.post(callBack::thenShadowBan);
                        } else if (response1.code == CommentAddResult.CODE_DELETED) {
                            //如果获取的评论列表提示被删除和回复评论提示也被删除才算秒删
                            insertBannedComment(new BannedCommentBean(commentArea, rpid, mainComment, BannedCommentBean.BANNED_TYPE_QUICK_DELETE, new Date(), BannedCommentBean.CHECKED_NO_CHECK));
                            handler.post(callBack::thenQuickDelete);
                        } else {
                            //登录信息过期或其他异常
                            handler.post(() -> callBack.onAccountFailure(response1.code, response1.message));
                        }
                    } else {
                        handler.post(() -> callBack.onOtherError(response.code, response.message));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callBack.onNetworkError(e));
            }
        });
    }


    public interface CheckCommentStatusCallBack extends NetworkCallBack {
        public void onSleeping(long waitTime);

        public void onStartCheckComment();

        public void thenOk();

        public void onCommentNotFound(String sentTestComment);

        public void onOtherError(int code, String message);

        public void onAccountFailure(int code, String message);

        public void thenInvisible();

        public void thenShadowBan();

        public void thenQuickDelete();
    }


    public void checkCommentStatusByNewMethod(CommentArea commentArea, String comment, long rpid, CheckCommentStatusByNewMethodCallBack callBack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GeneralResponse<CommentReply> response = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1).execute().body();
                    GeneralResponse<CommentReply> response1 = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1).execute().body();
                    handler.post(() -> callBack.onSleeping(waitTime));
                    sleep(waitTime);
                    if (response != null && response1 != null) {
                        if (response.code == 0 && response1.code == 0) {
                            //ok
                            handler.post(callBack::thenOk);
                        } else if (response.code == CommentAddResult.CODE_DELETED && response1.code == 0) {
                            //shadowBan
                            handler.post(callBack::thenShadowBan);
                        } else if (response.code == CommentAddResult.CODE_DELETED && response1.code == CommentAddResult.CODE_DELETED) {
                            //quickDelete
                            handler.post(callBack::thenQuickDelete);
                        } else {
                            //error
                            handler.post(callBack::thenError);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public interface CheckCommentStatusByNewMethodCallBack extends NetworkCallBack {

        public void onSleeping(long waitTime);

        public void onStartCheckComment();

        public void thenOk();

        public void thenShadowBan();

        public void thenQuickDelete();

        public void thenError();
    }

    public void checkCommentAreaMartialLaw(CommentArea commentArea, long mainCommRpid, String testComment, String testComment2, CheckCommentAreaMartialLawCalBack callBack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GeneralResponse<CommentAddResult> response = commentManipulator.sendComment(testComment, 0, 0, commentArea).execute().body();
                    if (response.isSuccess()) {
                        handler.post(() -> callBack.onTestCommentSent(testComment));
                        long testCommentRpid = response.data.rpid;
                        sleep(waitTime);
                        handler.post(callBack::onStartCheck);
                        if (commentManipulator.scanComment(commentArea, response.data.rpid, 0).isExists) {
                            deleteComment(commentArea, testCommentRpid).execute();
                            handler.post(callBack::thenAreaOk);
                        } else {
                            if (enableStatistics) {
                                statisticsDBOpenHelper.deleteBannedComment(String.valueOf(mainCommRpid));
                                statisticsDBOpenHelper.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(commentArea, testCommentRpid, testComment2));
                            }
                            deleteComment(commentArea, testCommentRpid).execute();
                            handler.post(callBack::thenMartialLaw);
                        }
                    } else {
                        handler.post(() -> callBack.onCommentSendFail(response.code, response.message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> callBack.onNetworkError(e));
                }
            }
        });
    }

    public interface CheckCommentAreaMartialLawCalBack extends NetworkCallBack {
        public void onTestCommentSent(String testComment);

        public void onStartCheck();

        public void onCommentSendFail(int code, String message);

        public void thenAreaOk();

        public void thenMartialLaw();
    }

    public void checkIfOnlyBannedInThisArea(CommentArea yourCommentArea, long mainCommentRpid, String comment, CheckIfOnlyBannedInThisAreaCallBack callBack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GeneralResponse<CommentAddResult> response = commentManipulator.sendComment(comment, 0, 0, yourCommentArea).execute().body();
                    handler.post(() -> callBack.onCommentSent(yourCommentArea.sourceId));
                    long rpid = response.data.rpid;
                    sleep(waitTime);
                    handler.post(callBack::onStartCheck);
                    if (commentManipulator.scanComment(yourCommentArea, rpid, 0).isExists) {
                        deleteComment(yourCommentArea, rpid).execute();
                        updateCheckedArea(mainCommentRpid, BannedCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA);
                        handler.post(callBack::thenOnlyBannedInThisArea);
                    } else {
                        deleteComment(yourCommentArea, rpid).execute();
                        updateCheckedArea(mainCommentRpid, BannedCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
                        handler.post(callBack::thenBannedInYourArea);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> callBack.onNetworkError(e));
                }
            }
        });
    }

    public interface CheckIfOnlyBannedInThisAreaCallBack extends NetworkCallBack {
        public void onCommentSent(String yourCommentArea);

        public void onStartCheck();

        public void thenOnlyBannedInThisArea();

        public void thenBannedInYourArea();
    }

    public void readyToScanSensitiveWorld(CommentArea mainCommentArea, CommentArea yourCommentArea, long mainCommRpid, String comment, String testComment1, String testComment2, ReadyToScanSensitiveWorldCallBack callBack) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //从数据库里查询之前是否评论仅在此被ban,null是没有检查过
                Boolean commentIsOnlyBan = statisticsDBOpenHelper.getCommentIsOnlyBannedInThisArea(String.valueOf(mainCommRpid));
                if (commentIsOnlyBan == null) {
                    //从数据库查询该评论区是否戒严,null是没有检查过
                    Boolean areaIsMartialLaw = statisticsDBOpenHelper.getCommentAreaIsMartialLaw(String.valueOf(yourCommentArea.oid), String.valueOf(mainCommRpid));
                    if (areaIsMartialLaw == null) {
                        checkAreaIfMartialLaw();
                    } else if (!areaIsMartialLaw) {
                        checkIfOnlyBannedInThisArea();
                    } else {
                        handler.post(callBack::onCommentAreaIsMartialLaw);
                    }
                } else if (!commentIsOnlyBan) {
                    handler.post(callBack::startScan);
                } else {
                    handler.post(callBack::onCommentIsOnlyBannedInThisArea);
                }
            }

            private void checkAreaIfMartialLaw() {
                try {
                    handler.post(callBack::onStartCheckAreaMartialLaw);
                    GeneralResponse<CommentAddResult> response = commentManipulator.sendComment(testComment1, 0, 0, mainCommentArea).execute().body();
                    sleep(waitTime);
                    long testCommentRpid = response.data.rpid;
                    if (commentManipulator.scanComment(mainCommentArea, testCommentRpid, 0).isExists) {
                        deleteComment(mainCommentArea, testCommentRpid).execute();
                        checkIfOnlyBannedInThisArea();
                    } else {
                        if (enableStatistics) {
                            statisticsDBOpenHelper.deleteBannedComment(String.valueOf(mainCommRpid));
                            statisticsDBOpenHelper.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(mainCommentArea, testCommentRpid, testComment2));
                        }
                        deleteComment(mainCommentArea, testCommentRpid).execute();
                        handler.post(callBack::onCommentAreaIsMartialLaw);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> callBack.onNetworkError(e));
                }
            }

            private void checkIfOnlyBannedInThisArea() {
                try {
                    handler.post(callBack::onStartCheckIsOnlyBannedInThisArea);
                    GeneralResponse<CommentAddResult> response = commentManipulator.sendComment(comment, 0, 0, yourCommentArea).execute().body();
                    sleep(waitTime);
                    long testCommentRpid = response.data.rpid;
                    if (commentManipulator.scanComment(yourCommentArea, testCommentRpid, 0).isExists) {
                        updateCheckedArea(mainCommRpid, BannedCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA);
                        deleteComment(mainCommentArea, testCommentRpid).execute();
                        handler.post(callBack::onCommentIsOnlyBannedInThisArea);
                    } else {
                        updateCheckedArea(mainCommRpid, BannedCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
                        deleteComment(mainCommentArea, testCommentRpid).execute();
                        handler.post(callBack::startScan);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> callBack.onNetworkError(e));
                }
            }

        });
    }

    public interface ReadyToScanSensitiveWorldCallBack extends NetworkCallBack {
        public void onCommentIsOnlyBannedInThisArea();

        public void onCommentAreaIsMartialLaw();

        public void onStartCheckIsOnlyBannedInThisArea();

        public void onStartCheckAreaMartialLaw();

        public void startScan();
    }


    public void scanSensitiveWorld(CommentArea yourCommentArea, String comment) {

    }

    public interface ScanSensitiveWorldCallBack extends NetworkCallBack {
        void onCommentSent(SpannableStringBuilder comment);
        
    }

    private void sleep(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void insertBannedComment(BannedCommentBean bannedCommentBean) {
        if (enableStatistics) {
            statisticsDBOpenHelper.insertBannedComment(bannedCommentBean);
        }
    }

    private void updateCheckedArea(long rpid, int checkedType) {
        if (enableStatistics) {
            statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), checkedType);
        }
    }


}
