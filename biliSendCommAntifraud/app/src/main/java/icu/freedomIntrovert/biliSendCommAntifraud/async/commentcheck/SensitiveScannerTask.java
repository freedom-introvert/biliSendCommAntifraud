package icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck;

import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import icu.freedomIntrovert.async.BackstageTask;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.ForwardDynamicResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.OkHttpUtil;

public class SensitiveScannerTask extends BackstageTask<SensitiveScannerTask.EventHandler> {

    private final Comment mainComment;
    private CommentArea commentAreaForTest;
    private String dynamicIdToBeForward;
    private final CommentManipulator commentManipulator;
    private final Config config;
    private final StatisticsDBOpenHelper helper;


    public SensitiveScannerTask(EventHandler handle, Comment mainComment, CommentArea yourCommentArea, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper helper) {
        super(handle);
        this.mainComment = mainComment;
        this.commentAreaForTest = yourCommentArea;
        this.commentManipulator = commentManipulator;
        this.config = config;
        this.helper = helper;
    }

    public SensitiveScannerTask(EventHandler handle, Comment mainComment, String dynamicIdToBeForward, CommentManipulator commentManipulator, Config config, StatisticsDBOpenHelper helper) {
        super(handle);
        this.mainComment = mainComment;
        this.dynamicIdToBeForward = dynamicIdToBeForward;
        this.commentManipulator = commentManipulator;
        this.config = config;
        this.helper = helper;
    }

    @Override
    protected void onStart(EventHandler eventHandler) throws Throwable {
        String commentText = mainComment.comment;
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
             */

        //如果有动态ID就使用小号转发动态来生成新的评论区
        if (dynamicIdToBeForward != null) {
            commentAreaForTest = forwardDynamicToCreateNewCommentArea(eventHandler);
        }
        //发送完整全文，如果全文正常则说明之前的评论区有问题
        GeneralResponse<CommentAddResult> body = commentManipulator.sendComment(commentText, 0, 0, commentAreaForTest, false).execute().body();
        OkHttpUtil.respNotNull(body);
        eventHandler.sendEventMessage(EventHandler.WHAT_COMMENT_FULL_TEXT_SENT, waitTime);
        sleepAndSendProgress(waitTime, eventHandler);
        long fulltextRpid = body.data.rpid;
        BiliComment foundComment = commentManipulator.findComment(commentAreaForTest, fulltextRpid, 0);
        commentManipulator.deleteComment(commentAreaForTest, fulltextRpid, false);
        if (foundComment != null) {
            //评论全文正常
            deleteForwardedDynamic(eventHandler,commentAreaForTest.sourceId);
            eventHandler.sendEventMessage(EventHandler.WHAT_COMMENT_FULL_TEXT_IS_NORMAL, commentAreaForTest);
            return;
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

            eventHandler.sendEventMessage(EventHandler.WHAT_ON_SEND_NEXT_COMMENT_AND_WAIT, normalPosition, splitLeftPosition, splitRightPosition, waitTime);
            GeneralResponse<CommentAddResult> resp = commentManipulator.sendComment(passText + split[0], 0, 0, commentAreaForTest, false).execute().body();
            OkHttpUtil.respNotNull(resp);
            long rpid = resp.data.rpid;
            sleepAndSendProgress(waitTime, eventHandler);
            eventHandler.sendEventMessage(EventHandler.WHAT_ON_CHECKING_COMMENT, currProg, max);
            if (commentManipulator.findComment(commentAreaForTest, rpid, 0) != null) {
                result.normalPosition = passText.length() + finalSplit[0].length();
                result.unusualPosition = passText.length() + finalSplit[0].length() + finalSplit[1].length();
                passText += split[0];
                split = splitFromTheMiddle(split[1]);
            } else {
                result.normalPosition = passText.length();
                result.unusualPosition = passText.length() + finalSplit[0].length();
                split = splitFromTheMiddle(split[0]);
            }
            eventHandler.sendEventMessage(EventHandler.WHAT_ON_CHECK_RESULT, result);
            commentManipulator.deleteComment(commentAreaForTest, rpid, false);
            System.out.println(passText);
            sleep(1000);
            currProg++;
        }
        helper.addSensitiveScanResultToHistoryComment(mainComment.rpid, result);
        deleteForwardedDynamic(eventHandler,commentAreaForTest.sourceId);
        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_ON_SCAN_COMPLETE);
    }

    private CommentArea forwardDynamicToCreateNewCommentArea(EventHandler eventHandler) throws IOException, BiliBiliApiException {
        eventHandler.sendEmptyEventMessage(EventHandler.WHAT_FORWARD_DYNAMIC);
        ForwardDynamicResult forwardDynamicResult = commentManipulator.forwardDynamicUsingSubAccount(dynamicIdToBeForward);
        long dynRid = forwardDynamicResult.dyn_rid;
        eventHandler.sendEventMessage(EventHandler.WHAT_FORWARDED_DYNAMIC, dynRid);
        sleepAndSendProgress(5000, eventHandler);
        return new CommentArea(dynRid, String.valueOf(dynRid), CommentArea.AREA_TYPE_DYNAMIC17);
    }

    private void deleteForwardedDynamic(EventHandler eventHandler,String dynamicId) throws BiliBiliApiException, IOException {
        if (dynamicIdToBeForward != null) {
            eventHandler.sendEmptyEventMessage(EventHandler.WHAT_DELETE_FORWARDED_DYNAMIC);
            commentManipulator.deleteDynamicUsingSubAccount(dynamicId);
        }
    }

    public static String[] splitFromTheMiddle(String input) {
        if (input.length() >= 8) {
            return new String[]{input.substring(0, input.length() / 2), input.substring(input.length() / 2)};
        } else {
            return null;
        }
    }

    public void sleepAndSendProgress(long time1, EventHandler eventHandler) {
        int time = (int) time1;
        eventHandler.sendEventMessage(EventHandler.WHAT_NEW_SLEEP_PROGRESS_MAX, time);
        int sleepSeg = 10;
        int sleepCount = time / sleepSeg;
        for (int i = 1; i <= sleepCount; i++) {
            sleep(sleepSeg);
            eventHandler.sendEventMessage(EventHandler.WHAT_NEW_SLEEP_PROGRESS, sleepSeg * i);
        }
        eventHandler.sendEventMessage(EventHandler.WHAT_NEW_SLEEP_PROGRESS_MAX, -1);
    }


    public abstract static class EventHandler extends BiliBiliApiRequestHandler {
        public static final int WHAT_COMMENT_FULL_TEXT_SENT = 1;
        public static final int WHAT_ON_SEND_NEXT_COMMENT_AND_WAIT = 2;
        public static final int WHAT_ON_CHECKING_COMMENT = 3;
        public static final int WHAT_ON_CHECK_RESULT = 4;
        public static final int WHAT_COMMENT_FULL_TEXT_IS_NORMAL = 100;
        public static final int WHAT_ON_SCAN_COMPLETE = 101;
        public static final int WHAT_NEW_SLEEP_PROGRESS_MAX = 20;
        public static final int WHAT_NEW_SLEEP_PROGRESS = 21;
        public static final int WHAT_FORWARD_DYNAMIC = 30;
        public static final int WHAT_FORWARDED_DYNAMIC = 31;
        public static final int WHAT_DELETE_FORWARDED_DYNAMIC = 32;


        public EventHandler(ErrorHandle errorHandle) {
            super(errorHandle);
        }
    }
}
