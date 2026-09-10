package icu.freedomIntrovert.biliSendCommAntifraud.async;

import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentOperateTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAppealResp;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;

public class CommentAppealTask extends CommentOperateTask<CommentAppealTask.EventHandler> {
    String areaId;
    String reason;
    Comment comment;

    public CommentAppealTask(Context context, Comment comment, String areaId, String reason, EventHandler handle) {
        super(handle, context);
        this.comment = comment;
        this.areaId = areaId;
        this.reason = reason;
    }

    @Override
    protected void onStart(EventHandler eventHandlerProxy) throws Throwable {
        CommentArea commentArea = comment.commentArea;
        GeneralResponse<CommentAppealResp> response = commentManipulator.appealComment(areaId, reason, accountManger.getAccount(comment.uid));
        if (response.code == 0) {
            eventHandlerProxy.onSuccess(response.data.success_toast);
            statisticsDB.updateCommentAreaAppealState(commentArea.type, commentArea.oid, comment.uid, HistoryComment.APPEAL_STATE_SUCCESS);
        } else if (response.code == 12082) {
            eventHandlerProxy.onNoCommentToAppeal(response.message);
            statisticsDB.updateCommentAreaAppealState(commentArea.type, commentArea.oid, comment.uid, HistoryComment.APPEAL_STATE_NO_COMMENT);
        } else {
            throw new BiliBiliApiException(response, "申诉失败");
        }
    }

    public interface EventHandler extends BaseEventHandler {
        void onSuccess(String successToast);

        void onNoCommentToAppeal(String successToast);
    }

}
