package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReplyPage;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class ReviewCommentStatusTask extends CommentOperateTask<ReviewCommentStatusTask.EventHandler> {
    HistoryComment[] historyComments;
    EventHandler handler;
    Account specifyAccount;
    boolean check = true;


    public ReviewCommentStatusTask(Context context, HistoryComment[] historyComments, @Nullable Account account, EventHandler handle) {
        super(handle, context);
        this.historyComments = historyComments;
        this.specifyAccount = account;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        this.handler = handler;
        //检查所有评论的UID是否有对应的账号对象、cookie是否失效
        if (!checkAccounts()){
            return;
        }
        System.out.println(Arrays.toString(historyComments));
        for (int i = 0; i < historyComments.length; i++) {
            if (!check){
                return;
            }
            checkOne(historyComments[i],i);
        }

    }

    private boolean checkAccounts() throws IOException {
        if (specifyAccount != null){
            boolean notFailed = AccountManger.checkCookieNotFailed(specifyAccount);
            if (!notFailed){
                handler.onCookieFailed(specifyAccount);
            }
            return notFailed;

        }
        HashSet<Account> accounts = new HashSet<>();
        for (HistoryComment historyComment : historyComments) {
            Account account = accountManger.getAccount(historyComment.uid);
            if (account == null){
                handler.onNoAccount(historyComment.uid);
                return false;
            }
            accounts.add(account);
        }
        for (Account account : accounts) {
            if (!AccountManger.checkCookieNotFailed(account)) {
                handler.onCookieFailed(account);
                return false;
            }
        }
        return true;
    }

    private void checkOne(HistoryComment historyComment,int index) throws IOException, BiliBiliApiException {
        long rpid = historyComment.rpid;
        CommentArea commentArea = historyComment.commentArea;
        //如果指定账号
        Account account = specifyAccount;
        if (account == null) {
            account = accountManger.getAccount(historyComment.uid);
        }
        handler.onStartCheck(historyComment,index);
        if (historyComment.root != 0) {
            GeneralResponse<CommentReplyPage> gr = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1);
            if (gr.isSuccess()){
                //不登录seek_rpid查找评论
                BiliComment foundReply = commentManipulator.findCommentFromCommentReplyArea(historyComment,account,false);
                if (foundReply != null) {
                    if (foundReply.invisible){
                        //回复评论invisible
                        result(foundReply,HistoryComment.STATE_INVISIBLE,historyComment,index);
                    } else {
                        //回复评论正常
                        result(foundReply,HistoryComment.STATE_NORMAL,historyComment,index);
                    }
                } else {
                    //登录seek_rpid查找评论
                    BiliComment foundReplyHasAcc = commentManipulator.findCommentFromCommentReplyArea(historyComment, account,true);
                    if (foundReplyHasAcc != null) {
                        //回复评论ShadowBan
                        result(foundReplyHasAcc,HistoryComment.STATE_SHADOW_BAN,historyComment,index);
                    } else {
                        //回复评论被删除
                        result(null,HistoryComment.STATE_DELETED,historyComment,index);
                    }
                }
            } else if (gr.code == GeneralResponse.CODE_COMMENT_DELETED){//根评论挂了
                updateHistoryComment(historyComment);
                handler.onRootCommentFailed(historyComment,index);
            } else if (gr.code == GeneralResponse.CODE_COMMENT_AREA_CLOSED) {
                handler.onAreaDead(historyComment,index);
            } else {
                throw new BiliBiliApiException(gr,"获取评论回复页失败");
            }
            return;
        }
        //根评论的检查
        HistoryComment checkedComment = commentManipulator.checkRootCommentStateByFast(historyComment, account);
        if (checkedComment == null){
            handler.onAreaDead(historyComment,index);
        } else {
            result(checkedComment,index);
        }
    }

    private void result(@Nullable BiliComment biliComment,String state,HistoryComment historyComment,int index){
        //当前面申诉提示无评论可申诉时，后面再检测到疑似审核就不改变状态
        if (!(historyComment.lastState.equals(HistoryComment.STATE_SUSPECTED_NO_PROBLEM) && state.equals(HistoryComment.STATE_UNDER_REVIEW))){
            historyComment.lastState = state;
        }
        if (biliComment != null) {
            historyComment.like = biliComment.like;
            historyComment.replyCount = biliComment.rcount;
        }
        result(historyComment,index);
    }

    private void result(HistoryComment historyComment,int index){
        updateHistoryComment(historyComment);
        handler.onCheckResult(historyComment,index);
    }

    public void breakRun() {
        check = false;
    }

    public synchronized HistoryComment[] getResults(){
        if (!isComplete()){
            throw new IllegalStateException("Need to be called when the execution is completed");
        }
        return historyComments;
    }

    public interface EventHandler extends BaseEventHandler{
        void onCookieFailed(Account account);
        void onNoAccount(long uid);
        //评论区失效，暂不改变评论状态
        default void onStartCheck(HistoryComment checkingComment,int index){};
        void onAreaDead(HistoryComment historyComment,int index);
        //根评论被删除或shadowBan，
        void onRootCommentFailed(HistoryComment historyComment,int index);
        void onCheckResult(HistoryComment historyComment,int index);
    }
}
