package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import icu.freedomIntrovert.async.BackstageTask;
import icu.freedomIntrovert.async.EventHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public abstract class CommentOperateTask<T extends EventHandler> extends BackstageTask<T> {
    protected final CommentManipulator commentManipulator;
    protected final Comment comment;
    protected final Config config;
    protected final StatisticsDBOpenHelper statisticsDB;

    public CommentOperateTask(T handle, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper statisticsDB,Comment comment) {
        super(handle);
        this.commentManipulator = commentManipulator;
        this.comment = comment;
        this.config = config;
        this.statisticsDB = statisticsDB;
    }

    public void insertHistoryComment(HistoryComment historyComment){
        if (config.getRecordeHistoryIsEnable()){
            statisticsDB.insertHistoryComment(historyComment);
        }
    }
}
