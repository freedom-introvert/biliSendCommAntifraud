package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class URCommentMonitoringTask extends CommentOperateTask<URCommentMonitoringTask.EventHandler> {
    Context context;
    public final HistoryComment comment;

    public URCommentMonitoringTask(Context context, HistoryComment comment,EventHandler handle) {
        super(handle, context);
        this.context = context;
        this.comment = comment;
    }

    @SuppressWarnings("BusyWait")
    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        String lastState = comment.lastState;
        for (int j = 0; j < 60; j++) {
            for (int i = 0; i < 60; i++) {
                Thread.sleep(1000);
                handler.onWaiting(60,i,j);
            }
            handler.onStartCheck();
            Account account = accountManger.getAccount(comment.uid);
            if (account == null){
                handler.onNoAccount(comment.uid,this);
                return;
            }
            try {
                HistoryComment checkedComment = commentManipulator.checkRootCommentStateByFast(comment, account);
                if (checkedComment == null){
                    handler.onAreaDead(this,comment);
                    return;
                }
                updateHistoryComment(checkedComment);
                if (!checkedComment.lastState.equals(lastState)) {
                    handler.onStateChange(checkedComment,j,this);
                    return;
                } else {
                    handler.onStateNotChange();
                }
            } catch (Throwable th){
                handler.onError(th,this);
                return;
            }
        }
        //超过一小时
        handler.onTimeOut1Hour(comment,this);

    }

    public interface EventHandler extends BaseEventHandler{
        void onNoAccount(long uid,URCommentMonitoringTask task);
        void onError(Throwable th,URCommentMonitoringTask task);
        void onTimeOut1Hour(HistoryComment historyComment, URCommentMonitoringTask task);
        void onWaiting(int max, int progressSecond, int minute);
        void onStartCheck();
        void onStateNotChange();
        void onStateChange(HistoryComment historyComment,int minute,URCommentMonitoringTask task);
        void onAreaDead(URCommentMonitoringTask task, HistoryComment comment);

    }
}
