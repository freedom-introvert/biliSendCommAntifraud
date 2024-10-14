package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;

import java.util.ArrayList;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.RandomChineseStringGenerator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class RandomCommentGenerateTask extends CommentOperateTask<RandomCommentGenerateTask.EventHandler> {
    Account account;
    String commentAreaLocation;
    int minLength;
    int maxLength;
    int quantity;

    public RandomCommentGenerateTask(Context context, Account account, String commentAreaLocation, int minLength, int maxLength,int quantity,EventHandler handle) {
        super(handle, context);
        this.account = account;
        this.commentAreaLocation = commentAreaLocation;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.quantity = quantity;
    }
    @SuppressWarnings("BusyWait")
    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        CommentArea commentArea = commentManipulator.matchCommentArea(commentAreaLocation,account);
        if (commentArea == null){
            handler.onCommentAreaNoMatch();
            return;
        }
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String randomChineseString = RandomChineseStringGenerator.generateRandomLengthChineseString(minLength, maxLength);
            CommentAddResult commentAddResult = commentManipulator.sendComment(randomChineseString, 0, 0, commentArea, account);
            handler.onGeneratedAndSentComment(randomChineseString,quantity,i);
            for (int i1 = 0; i1 < 5000; i1+=10) {
                Thread.sleep(10);
                handler.onWaitProgress(i1);
            }
            handler.onCheckingComment();
            BiliComment foundComment = commentManipulator.findComment(commentAddResult.reply, account);
            if (foundComment == null){
                handler.onCommentBanned(commentAddResult.reply);
                i--;
            } else {
                handler.onCommentOk(commentAddResult.reply);
                result.add(randomChineseString);
            }
            //删除发布的评论
            commentManipulator.deleteComment(commentArea,commentAddResult.rpid, account);
            //等待1秒，好让你看清结果而不至于一闪而过
            Thread.sleep(1000);
        }
        handler.onResult(result);
    }

    public interface EventHandler extends BaseEventHandler{
        void onCommentAreaNoMatch();
        void onGeneratedAndSentComment(String comment, int quantity,int index);
        /**
         * 等待时间固定为5秒，不使用自定义配置
         */
        void onWaitProgress(int time);
        void onCheckingComment();
        void onCommentBanned(BiliComment reply);
        void onCommentOk(BiliComment reply);

        void onResult(ArrayList<String> result);
    }
}
