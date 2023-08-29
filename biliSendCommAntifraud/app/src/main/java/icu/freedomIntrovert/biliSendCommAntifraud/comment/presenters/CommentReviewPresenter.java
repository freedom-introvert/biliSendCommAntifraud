package icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters;

import android.os.Handler;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import icu.freedomIntrovert.biliSendCommAntifraud.NetworkCallBack;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentReply;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class CommentReviewPresenter {
    private Handler handler;
    CommentManipulator commentManipulator;
    Executor executor;

    public CommentReviewPresenter(Handler handler, CommentManipulator commentManipulator) {
        this.handler = handler;
        this.commentManipulator = commentManipulator;
        executor = Executors.newSingleThreadExecutor();
    }

    public void reviewStatus(CommentArea commentArea, long rpid, ReviewStatusCallBack callBack){
        executor.execute(() -> {
            try {
                GeneralResponse<CommentReply> resp = commentManipulator.getCommentReplyHasAccount(commentArea, rpid, 1).execute().body();
                respNotNull(resp);
                if (resp.isSuccess()){
                    GeneralResponse<CommentReply> resp1 = commentManipulator.getCommentReplyNoAccount(commentArea, rpid, 1).execute().body();
                    respNotNull(resp1);
                    if (resp1.isSuccess()){
                        handler.post(callBack::ok);
                    } else {
                        handler.post(callBack::shadowBanned);
                    }
                } else {
                    handler.post(callBack::deleted);
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callBack.onNetworkError(e));
            }
        });
    }

    public interface ReviewStatusCallBack extends NetworkCallBack {
        void deleted();
        void shadowBanned();
        void ok();
    }

    private void respNotNull(GeneralResponse<?> generalResponse) throws IOException {
        if (generalResponse == null){
            throw new IOException("response is null!");
        }
    }
}
