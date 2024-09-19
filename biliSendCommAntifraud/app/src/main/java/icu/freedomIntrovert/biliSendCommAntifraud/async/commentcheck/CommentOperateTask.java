package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import icu.freedomIntrovert.async.BackstageTaskByMVP;
import icu.freedomIntrovert.async.BackstageTaskByMVP.BaseEventHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.RandomComments;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public  abstract class CommentOperateTask<T extends BaseEventHandler> extends BackstageTaskByMVP<T>{
    protected final CommentManipulator commentManipulator;
    protected final Config config;
    protected final AccountManger accountManger;
    protected final RandomComments randomComments;
    protected final StatisticsDBOpenHelper statisticsDB;
    public CommentOperateTask(T handle, Context context) {
        super(handle);
        this.commentManipulator = CommentManipulator.getInstance();
        this.config = Config.getInstance(context);
        this.statisticsDB = StatisticsDBOpenHelper.getInstance(context);
        this.accountManger = AccountManger.getInstance(context);
        this.randomComments = RandomComments.getInstance(context);
    }

    protected void insertHistoryComment(HistoryComment historyComment){
        if (config.getRecordeHistoryIsEnable()){
            statisticsDB.insertHistoryComment(historyComment);
        }
    }

    protected void updateHistoryComment(HistoryComment historyComment){
        statisticsDB.updateHistoryCommentStates(historyComment.rpid,
                historyComment.lastState,historyComment.like,
                historyComment.replyCount,historyComment.lastCheckDate);
    }

}
