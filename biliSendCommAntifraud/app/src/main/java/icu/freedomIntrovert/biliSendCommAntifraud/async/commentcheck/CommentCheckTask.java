package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;

public class CommentCheckTask extends CommentOperateTask<CommentCheckTask.EventHandler> {
    private final boolean needWait;
    private final ArrayList<String> clientCookies;
    private boolean check = true;
    private final Context context;
    private long remainingWaitTime = 0;
    private final Comment comment;

    public CommentCheckTask(Context context, Comment comment, boolean needWait, ArrayList<String> clientCookies, EventHandler handle) {
        super(handle, context);
        this.needWait = needWait;
        this.clientCookies = clientCookies;
        this.context = context;
        this.comment = comment;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        long rpid = comment.rpid;
        CommentArea commentArea = comment.commentArea;
        Account account = accountManger.getAccount(comment.uid);
        Account clientAccount = null;

        if (clientCookies != null && !clientCookies.isEmpty()) {
            //正在获取客户端cookie对应的账号
            for (int i = 0; i < clientCookies.size(); i++) {
                String clientCookie = clientCookies.get(i);
                handler.onGettingClientAccount();
                clientAccount = AccountManger.cookieToAccount(clientCookie);
                if (clientAccount == null) {
                    //客户端cookie无效！
                    if (i == clientCookies.size() - 1){
                        handler.onClientCookieInvalid(clientCookie);
                        return;
                    }
                } else if (clientAccount.uid != comment.uid) {
                    //客户端cookie对应的账号与评论发布者账号不一致！
                    handler.onClientCookieUidNoMatch(comment.uid, clientAccount.uid);
                    return;
                } else {
                    break;
                }
            }
        }

        //如果原评论UID所对应的账号在本地没有
        if (account == null) {
            //如果传来了客户端cookie且有效，则获取客户端账号保存到本地
            if (clientAccount == null) {
                //错误，没有此UID对应的账号，请添加此UID的账号，或者启用获取哔哩哔哩客户端cookie功能
                handler.onNoAccountAndClientCookie(comment.uid);
                return;
            }
            accountManger.addOrUpdateAccount(clientAccount);
            account = clientAccount;
        } else {
            //如果传来了客户端cookie且有效，则覆盖原账号信息，并且更新到本地
            if (clientAccount != null) {
                account.update(clientAccount);
                accountManger.addOrUpdateAccount(account);
            } else {//否则检查本地账号cookie是否失效
                if (!AccountManger.checkCookieNotFailed(account)) {
                    handler.onLocalAccountCookieFailed(account);
                    return;
                }
            }
        }

        //等待一段时间后检查评

        if (needWait) {
            long commWaitTime = config.getWaitTime();
            long picWaitTime = config.getWaitTimeByHasPictures();
            long totalWaitTime;
            if (comment.hasPictures()) {
                totalWaitTime = commWaitTime + picWaitTime;
                handler.onStartWaitHasPicture(commWaitTime, picWaitTime, totalWaitTime);
            } else {
                totalWaitTime = commWaitTime;
                handler.onStartWait(totalWaitTime);
            }
            for (long waitTime = 0; waitTime < totalWaitTime; waitTime+=10) {
                try {
                    Thread.sleep(10);
                    remainingWaitTime = totalWaitTime - waitTime;
                    handler.onWaitProgress(remainingWaitTime, waitTime);
                } catch (InterruptedException ignored) {
                }
                if (!check) {
                    return;
                }
            }
        }

        List<Comment.PictureInfo> pictureInfoList = comment.getPictureInfoList();
        if (pictureInfoList != null) {
            for (int i = 0; i < pictureInfoList.size(); i++) {
                Comment.PictureInfo pictureInfo = pictureInfoList.get(i);
                handler.onSavingPictures(pictureInfoList.size(), i, pictureInfo);
                PictureStorage.save(context, pictureInfoList.get(i).img_src);
            }
        }

        handler.onStartCheck();
        HistoryComment historyComment = new HistoryComment(comment);
        historyComment.lastCheckDate = new Date();
        //查找无账号下的评论列表或评论回复列表
        BiliComment biliComment = commentManipulator.findComment(comment, account);
        if (biliComment != null) {
            //判断是否被标记为invisible，使其在前端不可见
            if (biliComment.invisible) {
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_INVISIBLE);
                result(historyComment,handler);
            } else {
                //评论正常
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_NORMAL);
                result(historyComment,handler);
            }
            //删除待检查评论
            statisticsDB.deletePendingCheckComment(rpid);
            return;
        }
        //没有找到评论时
        handler.onCommentNotFound();
        if (comment.root == 0) {
            GeneralResponse<CommentReplyPage> response = commentManipulator.getCommentReplyHasAccount(commentArea, comment.rpid, 1, account);
            if (response.isSuccess()) {
                /*
                //为啥要sleep？
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                //评论shadowBan或者疑似审核中
                GeneralResponse<CommentReplyPage> noACResp = commentManipulator.getCommentReplyNoAccount(commentArea, comment.rpid, 0);
                if (noACResp.isSuccess()) {
                    //找不到评论，有账号能获取评论列表，无账号也可以获取评论列表，这种情况大半申诉说无可申诉，除非你是UP发评论被shadowBan
                    //也可能invisible，因为换了main api，这个前后端夹击
                    BiliComment root = noACResp.data.root;
                    if (root.invisible) {
                        //评论invisible
                        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_INVISIBLE);
                        result(historyComment,handler);
                    } else {
                        //评论疑似审核中
                        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_UNDER_REVIEW);
                        result(historyComment,handler);
                    }
                } else if (noACResp.code == GeneralResponse.CODE_COMMENT_DELETED) {
                    //评论shadowBan
                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SHADOW_BAN);
                    result(historyComment,handler);
                } else {
                    throw new BiliBiliApiException(noACResp, "无法获取评论回复页（无账号）");
                }
            } else if (response.code == GeneralResponse.CODE_COMMENT_DELETED) {
                //当有账号都提示已被删除了，则判断为真的被删了
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_DELETED);
                result(historyComment,handler);
                //已移除多虑的判断
                /*//再尝试对评论进行回复，看看是否应session过期导致变成了游客视角
                GeneralResponse<CommentAddResult> response1 = commentManipulator.getSendCommentCall(testCommentText, comment.rpid, comment.root, commentArea, false).execute().body();
                OkHttpUtil.respNotNull(response1);
                if (response1.isSuccess()) {
                    //应该不存在有账号获取评论列表被删除了还能回复的吧:(
                    sleep(config.getWaitTime());
                    commentManipulator.deleteComment(comment.commentArea, comment.rpid,false);
                    eventHandler.sendEmptyEventMessage(CommentCheckTask.EventHandler.WHAT_THEN_SHADOW_BAN);
                } else if (response1.code == CommentAddResult.CODE_DELETED) {
                    //如果获取的评论列表提示被删除和回复评论提示也被删除才算秒删
                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_DELETED);
                    insertHistoryComment(historyComment);
                    eventHandler.sendEmptyEventMessage(CommentCheckTask.EventHandler.WHAT_THEN_DELETED);
                } else {
                    //登录信息过期或其他异常
                    eventHandler.sendError(new BiliBiliApiException(response1.code, response1.message, null));
                }*/
            } else {
                throw new BiliBiliApiException(response, "无法获取评论回复页（有账号）");
            }
        } else {//是评论回复的处理方式
            //有账号**定位**查找回复评论
            BiliComment foundReply = commentManipulator.findCommentFromCommentReplyArea(comment, account, true);
            if (foundReply != null) {
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SHADOW_BAN);
                result(historyComment,handler);
            } else {
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_DELETED);
                result(historyComment,handler);
            }
        }
        //删除待检查评论
        statisticsDB.deletePendingCheckComment(rpid);
    }

    private void result(HistoryComment historyComment,EventHandler handler){
        insertHistoryComment(historyComment);
        handler.onResult(historyComment);
    }

    /**
     * 取消等待，不检查了。需要在等待时调用
     */
    public void cancelCheck() {
        check = false;
    }

    public long getRemainingWaitTime() {
        return remainingWaitTime;
    }

    public interface EventHandler extends BaseEventHandler {

        void onGettingClientAccount();

        void onClientCookieInvalid(String cookie);

        void onLocalAccountCookieFailed(Account account);

        void onClientCookieUidNoMatch(long commentUid, long cookieUid);

        void onNoAccountAndClientCookie(long uid);

        void onStartWait(long totalMs);

        void onStartWaitHasPicture(long totalCommWaitMs, long totalPicWaitMs, long totalMs);

        void onWaitProgress(long remainingMs, long currentMs);

        void onSavingPictures(int total, int index, Comment.PictureInfo pictureInfo);

        void onStartCheck();

        void onCommentNotFound();

        void onResult(HistoryComment historyComment);

/*        void thenInvisible();

        void thenCommentOk();

        void thenUnderReview();

        void thenShadowBan();

        void thenDeleted();*/
    }
}
