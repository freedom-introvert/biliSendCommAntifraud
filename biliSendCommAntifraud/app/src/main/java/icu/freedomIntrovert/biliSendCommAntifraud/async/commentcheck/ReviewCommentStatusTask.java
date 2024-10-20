package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
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
        //如果指定账号
        Account account = specifyAccount;
        if (account == null) {
            account = accountManger.getAccount(historyComment.uid);
        }
        handler.onStartCheck(historyComment,index);
        if (historyComment.root != 0) {
            try {
                HistoryComment checkedComment = commentManipulator.recheckReplyCommentState(historyComment, account);
                if (checkedComment == null){
                    handler.onAreaDead(historyComment,index);
                } else {
                    result(checkedComment,index);
                }
            } catch (CommentManipulator.RootCommentDeadException e) {
                handler.onRootCommentFailed(historyComment,index);
            }
            return;
        }
        //根评论的检查
        HistoryComment checkedComment = commentManipulator.recheckRootCommentStateByFast(historyComment, account);
        if (checkedComment == null){
            handler.onAreaDead(historyComment,index);
        } else {
            result(checkedComment,index);
        }
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
