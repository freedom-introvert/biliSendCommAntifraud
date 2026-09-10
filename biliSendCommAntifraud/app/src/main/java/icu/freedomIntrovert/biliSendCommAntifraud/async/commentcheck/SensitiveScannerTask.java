package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.ForwardDynamic;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;

public class SensitiveScannerTask extends CommentOperateTask<SensitiveScannerTask.EventHandler> {
    Comment comment;
    CommentArea commentArea;
    Account forwardDynamicAccount;

    /**
     * 自己评论区检查 | 小号转发动态生成评论区检查 | 当前评论区检查
     *
     * @param context
     * @param comment 评论
     * @param forwardDynamicAccount 转发动态的账号：null | 除你之外 | null
     * @param commentArea 指定评论区：你的 | null | 当前
     * @param handle
     */
    public SensitiveScannerTask(Context context, Comment comment, Account forwardDynamicAccount, CommentArea commentArea, EventHandler handle) {
        super(handle, context);
        this.comment = comment;
        this.commentArea = commentArea;
        this.forwardDynamicAccount = forwardDynamicAccount;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        String commentText = comment.comment;
        long waitTime = config.getWaitTime();
            /*
            计算扫描要多少次？你只需要高中知识
            例如最小块为4，评论长度为256，要经过这扫描过程：256/2/2/2/2/2/2,直到  4<结果大小<8  ,扫描次数为6
            分析可得公式 设扫描次数为x
            最小块大小*2^x=256
            最小块大小=4
            4*2^x=256
            等式两边同时/4
            2^x=64
            x=log2(64)
            x=6

            公式推理过程：
            扫描次数：x，评论长度：y，最小块大小：a
            $$
            \begin{array}{c}
            a\times 2^x=y
            \\
            2^x=\frac{y}{a}
            \\
            x=log_{2}{\frac{y}{a}}
            \\
            x=\frac{lg_{}{\frac{y}{a}}}{lg_{}{2}}
            \end{array}
            $$

            时间复杂度：O(log(y/a))，随着评论长度 y 增加，扫描次数会按对数增长
             */

        Account account = accountManger.getAccount(comment.uid);
        if (account == null) {
            handler.onCommentAccountNotFound(comment.uid);
            return;
        }
        //当前评论区全文检查会导致“重复评论，请勿刷屏”。全文检查仅限其他评论区，用处是判断当前评论区自身是否存在问题（仅在此评论区被Ban）
        boolean needFullTextCheck = false;
        //首先依据评论区判断3个选项 自己评论区检查 | 小号转发动态生成评论区检查 | 当前评论区检查
        if (commentArea == null){
            //小号转发动态生成评论区检查
            if (forwardDynamicAccount == null){
                throw new IllegalArgumentException("使用小号转发动态生成评论区检查，forwardDynamicAccount不能为空！");
            }
            needFullTextCheck = true;
            ForwardDynamic forwardDynamic = config.getForwardDynamic();
            if (forwardDynamic == null){
                handler.onNotSetForwardDynamic();
                return;
            }
            commentArea = forwardDynamicToCreateNewCommentArea(handler,forwardDynamic.forwardDynamicId,forwardDynamicAccount);
        } else if (!commentArea.equals(comment.commentArea)){
            needFullTextCheck = true;
        }

        //发送完整全文，如果全文正常则说明之前的评论区有问题
        if (needFullTextCheck) {
            handler.onCheckingCommentFullText(waitTime);
            BiliComment fulltextComment = commentManipulator.sendComment(commentText, 0, 0, commentArea, account).reply;
            sleepAndSendProgress(waitTime, handler);
            BiliComment foundComment = commentManipulator.findComment(fulltextComment,account);
            commentManipulator.deleteComment(commentArea, fulltextComment.rpid, account);
            if (foundComment != null) {
                //评论全文正常
                deleteForwardedDynamicIfNeed(handler, commentArea.sourceId);
                handler.onCommentFullTextIsNormal(commentArea);
                return;
            }
        }

        int max = (int) (Math.log((double) commentText.length() / 4) / Math.log(2));//根据换底公式，logx(y)=lgy/lgx-
        int currProg = 1;
        Log.i("comment.length", String.valueOf(commentText.length()));
        Log.i("max:", String.valueOf(max));
        String passText = "";
        String[] split = splitFromTheMiddle(commentText);
        SensitiveScanResult result = new SensitiveScanResult();
        while (split != null) {
            System.out.println(Arrays.toString(split));
            String[] finalSplit = split;
            int normalPosition = passText.length();
            int splitLeftPosition = passText.length() + finalSplit[0].length();
            int splitRightPosition = passText.length() + finalSplit[0].length() + finalSplit[1].length();

            handler.onSendNextCommentAndWait(normalPosition, splitLeftPosition, splitRightPosition, waitTime);
            CommentAddResult commentAddResult = commentManipulator.sendComment(passText + split[0], 0, 0,commentArea, account);
            sleepAndSendProgress(waitTime, handler);
            handler.onCheckingComment(currProg, max);
            if (commentManipulator.findComment(commentAddResult.reply,account) != null) {
                result.normalPosition = passText.length() + finalSplit[0].length();
                result.unusualPosition = passText.length() + finalSplit[0].length() + finalSplit[1].length();
                passText += split[0];
                split = splitFromTheMiddle(split[1]);
            } else {
                result.normalPosition = passText.length();
                result.unusualPosition = passText.length() + finalSplit[0].length();
                split = splitFromTheMiddle(split[0]);
            }
            handler.onCheckResult(result);
            commentManipulator.deleteComment(commentArea, commentAddResult.rpid, account);
            System.out.println(passText);
            sleep(1000);
            currProg++;
        }
        statisticsDB.addSensitiveScanResultToHistoryComment(comment.rpid, result);
        if (forwardDynamicAccount != null){
            deleteForwardedDynamicIfNeed(handler, commentArea.sourceId);
        }
        handler.onScanComplete();
    }

    private CommentArea forwardDynamicToCreateNewCommentArea(EventHandler eventHandler,String dynamicIdToBeForward,Account account) throws IOException, BiliBiliApiException {
        eventHandler.onForwardDynamic();
        ForwardDynamicResult forwardDynamicResult = commentManipulator.forwardDynamic(dynamicIdToBeForward,account);
        long dynRid = forwardDynamicResult.dyn_rid;
        eventHandler.onForwardedDynamic(dynRid);
        sleepAndSendProgress(5000, eventHandler);
        return new CommentArea(dynRid, String.valueOf(dynRid), CommentArea.AREA_TYPE_DYNAMIC17);
    }

    private void deleteForwardedDynamicIfNeed(EventHandler eventHandler, String dynamicId) throws BiliBiliApiException, IOException {
        if (forwardDynamicAccount != null) {
            eventHandler.onDeleteForwardedDynamic(dynamicId);
            commentManipulator.deleteDynamic(dynamicId,forwardDynamicAccount);
        }
    }

    public static String[] splitFromTheMiddle(String input) {
        if (input.length() >= 8) {
            return new String[]{input.substring(0, input.length() / 2), input.substring(input.length() / 2)};
        } else {
            return null;
        }
    }

    public void sleepAndSendProgress(long waitTime, EventHandler eventHandler) {
        int time = (int) waitTime;
        eventHandler.onNewSleepProgressMax(time);
        int sleepSeg = 10;
        int sleepCount = time / sleepSeg;
        for (int i = 1; i <= sleepCount; i++) {
            sleep(sleepSeg);
            eventHandler.onNewSleepProgress(sleepSeg * i);
        }
        eventHandler.onNewSleepProgressMax(-1);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {
        }
    }

    public interface EventHandler extends BaseEventHandler{
        void onCommentAccountNotFound(long uid);
        void onNotSetForwardDynamic();
        void onCheckingCommentFullText(long waitTime);
        void onCommentFullTextIsNormal(CommentArea commentArea);
        void onNewSleepProgressMax(int max);
        void onNewSleepProgress(int progress);
        void onForwardDynamic();
        void onForwardedDynamic(long dynRid);
        void onDeleteForwardedDynamic(String dynRid);

        void onSendNextCommentAndWait(int normalPosition, int splitLeftPosition, int splitRightPosition, long waitTime);

        void onCheckingComment(int currProgress, int max);

        void onCheckResult(SensitiveScanResult result);

        void onScanComplete();
    }
}
