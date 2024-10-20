package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class CommentMonitoringTask extends CommentOperateTask<CommentMonitoringTask.EventHandler> {
    Context context;
    public final HistoryComment comment;
    public final int timeoutMinute;
    private boolean isCanceled = false;

    public CommentMonitoringTask(Context context, HistoryComment comment, int timeoutMinute, EventHandler handle) {
        super(handle, context);
        this.context = context;
        this.comment = comment;
        this.timeoutMinute = timeoutMinute;
    }

    @SuppressWarnings("BusyWait")
    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        handler.onInit(this);
        String lastState = comment.lastState;
        for (int j = 0; j < timeoutMinute; j++) {
            for (int i = 0; i < 60; i++) {
                Thread.sleep(1000);
                if (isCanceled) {
                    handler.onCanceled();
                    return;
                }
                handler.onWaiting(60, i, j);
            }
            handler.onStartCheck();
            Account account = accountManger.getAccount(comment.uid);
            if (account == null) {
                handler.onNoAccount(comment.uid);
                return;
            }
            if (comment.root == 0) {
                HistoryComment checkedComment = commentManipulator.recheckRootCommentStateByFast(comment, account);
                if (checkedComment == null) {
                    handler.onAreaDead(comment);
                    return;
                }
                updateHistoryComment(checkedComment);
                if (!checkedComment.lastState.equals(lastState)) {
                    handler.onStateChange(checkedComment, j);
                    return;
                } else {
                    handler.onStateNotChange();
                }
            } else {
                //TODO 补全
                try {
                    HistoryComment checkedComment = commentManipulator.recheckReplyCommentState(comment, account);
                    if (checkedComment == null) {
                        handler.onAreaDead(comment);
                        return;
                    }
                    updateHistoryComment(checkedComment);
                    if (!checkedComment.lastState.equals(lastState)) {
                        handler.onStateChange(checkedComment, j);
                        return;
                    } else {
                        handler.onStateNotChange();
                    }
                } catch (CommentManipulator.RootCommentDeadException e){
                    handler.onRootDead(comment);
                }
            }
        }
        //超过超时
        handler.onTimeout(comment, timeoutMinute);
    }

    public void cancel() {
        isCanceled = true;
    }

    public interface EventHandler extends BaseEventHandler {
        void onInit(CommentMonitoringTask task);

        void onNoAccount(long uid);

        void onTimeout(HistoryComment historyComment, int minute);

        void onWaiting(int max, int progressSecond, int minute);

        void onStartCheck();

        void onStateNotChange();

        void onStateChange(HistoryComment historyComment, int minute);

        void onAreaDead(HistoryComment comment);
        void onRootDead(HistoryComment comment);

        void onCanceled();
    }
}
