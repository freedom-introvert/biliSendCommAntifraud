package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import icu.freedomIntrovert.async.EventMessage;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiRequestHandler;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.AreaMartialLawCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.BannedOnlyInThisAreaCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.SensitiveScannerTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.AppealDialogPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;

public class DialogCommCheckWorker implements BiliBiliApiRequestHandler.DialogErrorHandle.OnDialogMessageListener {
    public static final int BANNED_TYPE_SHADOW_BAN = 1;
    public static final int BANNED_TYPE_QUICK_DELETE = 2;
    public static final int BANNED_TYPE_UNDER_REVIEW = 3;
    public static final int BANNED_TYPE_INVISIBLE = 4;

    private Context context;
    private Config config;
    private StatisticsDBOpenHelper statDB;
    private Handler handler;
    private CommentManipulator commentManipulator;
    private CommentUtil commentUtil;
    private OnExitListener exitListener;

    public DialogCommCheckWorker(Context context, Config config, StatisticsDBOpenHelper statDB, CommentManipulator commentManipulator, CommentUtil commentUtil) {
        this.context = context;
        this.config = config;
        this.statDB = statDB;
        this.commentManipulator = commentManipulator;
        this.commentUtil = commentUtil;
        this.handler = new Handler();
    }

    public void setExitListener(OnExitListener exitListener) {
        this.exitListener = exitListener;
    }

    public void checkComment(Comment comment, ProgressBarDialog dialog) {
        if (commentManipulator.cookieAreSet()) {
            DialogCommentCheckEventHandler handle = new DialogCommentCheckEventHandler(dialog, comment, this);
            new CommentCheckTask(handle, commentManipulator, config, statDB, comment, commentUtil.getRandomComment(comment.commentArea)).execute();
        } else {
            dialog.dismiss();
            DialogUtil.dialogMessage(context, "未登录", "请先设置cookie！");
        }
    }

    private static class DialogCommentCheckEventHandler extends CommentCheckTask.EventHandler {
        private final ProgressBarDialog dialog;
        private final Comment comment;
        private final DialogCommCheckWorker worker;

        public DialogCommentCheckEventHandler(ProgressBarDialog dialog, Comment comment, DialogCommCheckWorker worker) {
            super(new DialogErrorHandle(dialog, worker));
            this.dialog = dialog;
            this.comment = comment;
            this.worker = worker;
        }

        @Override
        protected void handleEvent(EventMessage message) {
            switch (message.getWhat()) {
                case WHAT_ON_START_COMMENT_CHECK:
                    dialog.setMessage("检查评论中……");
                    break;
                case WHAT_ON_COMMENT_NOT_FOUND:
                    dialog.setMessage("评论列表未找到该评论，判断状态中……");
                    break;
                case WHAT_ON_PAGE_TURN_FOR_HAS_ACC_REPLY:
                    dialog.setMessage("正在有账号条件下查找评论回复列表，第" +
                            message.getObject(0, Integer.class) + "页");
                    break;
                case WHAT_THEN_COMMENT_OK:
                    dialog.dismiss();
                    worker.showCommentIsOkResult(comment.comment, comment.rpid);
                    break;
                case WHAT_THEN_SHADOW_BAN:
                    dialog.dismiss();
                    worker.showCommentBannedResult(BANNED_TYPE_SHADOW_BAN, comment);
                    break;
                case WHAT_THEN_DELETED:
                    dialog.dismiss();
                    worker.showCommentBannedResult(BANNED_TYPE_QUICK_DELETE, comment);
                    break;
                case WHAT_THEN_UNDER_REVIEW:
                    dialog.dismiss();
                    worker.showCommentBannedResult(BANNED_TYPE_UNDER_REVIEW, comment);
                    break;
                case WHAT_THEN_INVISIBLE:
                    dialog.dismiss();
                    worker.showCommentBannedResult(BANNED_TYPE_INVISIBLE, comment);
                    break;
            }
        }
    }

    private void showCommentIsOkResult(String comment, long rpid) {
        onNewCommentRpid(rpid);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("检查结果")
                .setMessage("你的评论：“" + comment + "”正常显示！")
                .setCancelable(false)
                .setOnDismissListener(dialog13 -> exit())
                .setPositiveButton("关闭",null)
                .show();
    }


    private void showCommentBannedResult(int bannedType, Comment comment) {
        onNewCommentRpid(comment.rpid);
        CommentArea commentArea = comment.commentArea;
        long rpid = comment.rpid;
        String commentText = comment.comment;
        AlertDialog.Builder resultDialogBuilder = new AlertDialog.Builder(context).setTitle("检查结果");
        switch (bannedType) {
            case BANNED_TYPE_SHADOW_BAN:
                resultDialogBuilder.setIcon(R.drawable.hide_black);
                if (comment.root == 0) {
                    resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(commentText, 100) + "”在无账号环境下未找到，自己账号下获取该评论的回复列表成功，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严");
                } else {
                    resultDialogBuilder.setMessage("您的楼中楼评论“" + CommentUtil.subComment(commentText, 100) + "”在无账号环境下未找到，自己账号下成功找到，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严");
                }
                break;
            case BANNED_TYPE_UNDER_REVIEW:
                resultDialogBuilder.setIcon(R.drawable.i_black);
                resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(commentText, 100) + "”在无账号环境下无法找到，自己账号下获取该评论的回复列表成功，接着又能在无账号下获取回复，疑似审核中。建议你直接去申诉，因为它很可能回复“无可申诉评论”，请后续几十分钟来历史记录复查！");
                break;
            case BANNED_TYPE_QUICK_DELETE:
                resultDialogBuilder.setIcon(R.drawable.deleted_black);
                if (comment.root == 0) {
                    resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(commentText, 100) + "”在自己账号下获取该评论的回复列表和对该评论发送回复时均收到提示：“已经被删除了”，判定改评论被系统速删，请检查评论内容或者检查评论区是否被戒严");
                } else {
                    resultDialogBuilder.setMessage("您的楼中楼评论“" + CommentUtil.subComment(commentText, 100) + "”在无账号和自己账号下都没找到，判定该评论被系统速删，请检查评论内容或者检查评论区是否被戒严");
                }
                break;
            case BANNED_TYPE_INVISIBLE:
                resultDialogBuilder.setIcon(R.drawable.ghost_black);
                resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(commentText, 100) + "”在无账号环境下成功找到，但是被标记invisible，也就是隐身（在前端被隐藏）！这是非常罕见的情况……通常在评论发送很久时间后才会出现。可以的话把评论信息发给开发者，以分析触发条件");
                break;
        }
        resultDialogBuilder.setOnDismissListener(dialog -> exit());
        resultDialogBuilder.setPositiveButton("关闭",new VoidDialogInterfaceOnClickListener());
        resultDialogBuilder.setNeutralButton("检查评论区", null);
        resultDialogBuilder.setNegativeButton("更多评论选项", null);
        AlertDialog resultDialog = resultDialogBuilder.show();
        //检查评论区
        resultDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("选择模式")
                .setItems(new String[]{"主号cookie检查", "小号cookie检查"}, (dialog, which) -> {
                    boolean isDeputyAccount = which == 1;
                    checkAreaMartialLaw(comment, null,isDeputyAccount);
                }).show());

        //更多评论选项
        resultDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("更多选项").setItems(new String[]{"扫描敏感词", "申诉", "删除发布的评论", "复制rpid、oid、type"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: //扫描敏感词
                            scanSensitiveWord(comment);
                            break;
                        case 1: //申诉
                            AlertDialog dialog1 = new AlertDialog.Builder(context)
                                    .setTitle("警告")
                                    .setMessage("评论不能正常显示时判断评论状态会发送测试回复评论、测试戒严的评论区会发送测试评论，请注意：申诉后，这些自己删掉的测试评论可能会被恢复，如果通知是“无法恢复”，那么不用管他，如果是“无违规”，请注意去删除测试评论！")
                                    .setNegativeButton("还是算了", new VoidDialogInterfaceOnClickListener())
                                    .setNeutralButton("官方申诉网址", (dialog23, which1) -> {
                                        Uri uri = Uri.parse("https://www.bilibili.com/h5/comment/appeal");
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        context.startActivity(intent);
                                    })
                                    .setPositiveButton("去申诉", (dialog24, which1) -> {
                                        AppealDialogPresenter appealDialogPresenter = new AppealDialogPresenter(context, handler, commentManipulator);
                                        appealDialogPresenter.appeal(CommentUtil.sourceIdToUrl(commentArea), commentText, new AppealDialogPresenter.CallBack() {

                                            @Override
                                            public void onRespInUI(int code, String toastText) {
                                                //如果这个时候还出现“无可申述评论”那么可能把评论状态误判了或者在某种审核中
                                                if (code == 12082) {
                                                    statDB.updateHistoryCommentLastState(rpid, HistoryComment.STATE_SUSPECTED_NO_PROBLEM);
                                                    toastLong(toastText + "\n可能因为检查评论时误判了或评论在某种处理或审核状态，等待一段时间后应该可以显示");
                                                } else {
                                                    toastLong(toastText);
                                                }
                                            }

                                            @Override
                                            public void onNetErrInUI(String msg) {
                                                toastNetError(msg);
                                            }
                                        });
                                    })
                                    .show();
                            break;
                        case 2: //删除发布的评论
                            commentManipulator.createDeleteCommentCall(commentArea, rpid).enqueue(new BiliApiCallback<GeneralResponse<Object>>() {
                                @Override
                                public void onError(Throwable th) {
                                    toastNetError(th.getMessage());
                                }

                                @Override
                                public void onSuccess(GeneralResponse<Object> unused) {
                                    resultDialog.dismiss();
                                    if (unused.isSuccess()) {
                                        toastLong("删除成功！");
                                    } else {
                                        toastLong("删除失败！");
                                    }
                                }
                            });
                            break;
                        case 3://复制rpid等评论信息
                            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData mClipData = ClipData.newPlainText("Label", "rpid:" + rpid + "\noid:" + commentArea.oid + "\ntype:" + commentArea.type);
                            cm.setPrimaryClip(mClipData);
                            toastShort("已复制");
                    }
                }
            }).show();
        });
    }

    public void checkAreaMartialLaw(Comment comment, DialogInterface.OnDismissListener onDismissListener, boolean isDeputyAccount) {
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context,
                "检测评论区是否被戒严", "发布测试评论中……");
        progressDialog.setCancelable(false);
        progressDialog.show();
        if (!isDeputyAccount) {
            progressDialog.setMessage("发布测试评论中（使用主号）……");
        } else {
            if (commentManipulator.deputyCookieAreSet()) {
                progressDialog.setMessage("发布测试评论中（使用小号）……");
            } else {
                progressDialog.dismiss();
                DialogUtil.dialogMessage(context, "错误",
                        "你没有设置小号的cookie，请先设置小号的cookie！");
            }
        }
        AreaCheckHandler handle = new AreaCheckHandler(progressDialog,onDismissListener, comment, this);
        new AreaMartialLawCheckTask(handle, commentManipulator, config, statDB,
                comment, commentUtil, isDeputyAccount).execute();
    }


    private static class AreaCheckHandler extends AreaMartialLawCheckTask.EventHandler {

        ProgressDialog progressDialog;
        Comment comment;
        DialogCommCheckWorker worker;
        DialogInterface.OnDismissListener onDismissListener;

        public AreaCheckHandler(ProgressDialog progressDialog, DialogInterface.OnDismissListener onDismissListener, Comment comment, DialogCommCheckWorker worker) {
            super(new DialogErrorHandle(progressDialog, worker));
            this.progressDialog = progressDialog;
            this.comment = comment;
            this.worker = worker;
            this.onDismissListener = onDismissListener;
        }

        @Override
        protected void handleEvent(EventMessage message) {
            switch (message.getWhat()) {
                case WHAT_ON_TEST_COMMENT_SENT:
                    progressDialog.setMessage("已发送测评论：“" +
                            message.getObject(0, String.class) +
                            "”，等待设置好的时间后检查评论……");
                    break;
                case WHAT_ON_START_CHECK:
                    progressDialog.setMessage("检查中……");
                    break;
                case WHAT_THEN_AREA_OK:
                    progressDialog.dismiss();
                    new AlertDialog.Builder(worker.context)
                            .setTitle("评论区检查结果")
                            .setMessage("评论区没有戒严，是否继续检查该评论是否仅在此评论区被ban？")
                            .setPositiveButton("检查", (dialog1, which) -> {
                                CommentArea yourCommentArea = worker.commentUtil.getYourCommentArea();
                                if (yourCommentArea == null) {
                                    worker.commentUtil.setYourCommentArea(worker.context, worker.commentManipulator);
                                } else {
                                    worker.checkIfBannedOnlyInThisArea(comment,onDismissListener, yourCommentArea);
                                }
                            })
                            .setNegativeButton("不了", new VoidDialogInterfaceOnClickListener())
                            .setOnDismissListener(onDismissListener)
                            .show();
                    break;
                case WHAT_THEN_MARTIAL_LAW:
                    progressDialog.dismiss();
                    new AlertDialog.Builder(worker.context)
                            .setTitle("检查结果")
                            .setMessage("评论区被戒严！")
                            .setPositiveButton(R.string.ok, new VoidDialogInterfaceOnClickListener())
                            .setOnDismissListener(onDismissListener)
                            .show();
                    break;
            }
        }
    }


    private void checkIfBannedOnlyInThisArea(Comment comment, DialogInterface.OnDismissListener onDismissListener, CommentArea yourCommentArea) {
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "检测评论是否仅在该评论区被ban", "等待设置好的时间后发送评论到你的评论区进行测试……");
        progressDialog.setCancelable(false);
        progressDialog.show();
        BannedOnlyInThisAreaCheckHandler handler = new BannedOnlyInThisAreaCheckHandler(this,onDismissListener, progressDialog);
        new BannedOnlyInThisAreaCheckTask(handler, commentManipulator, config, statDB, comment, yourCommentArea).execute();
    }

    private static class BannedOnlyInThisAreaCheckHandler extends BannedOnlyInThisAreaCheckTask.EventHandler {
        DialogCommCheckWorker worker;
        ProgressDialog progressDialog;
        DialogInterface.OnDismissListener onDismissListener;

        public BannedOnlyInThisAreaCheckHandler(DialogCommCheckWorker worker, DialogInterface.OnDismissListener onDismissListener, ProgressDialog progressDialog) {
            super(new DialogErrorHandle(progressDialog, worker));
            this.worker = worker;
            this.progressDialog = progressDialog;
            this.onDismissListener = onDismissListener;
        }

        @Override
        protected void handleEvent(EventMessage message) {
            switch (message.getWhat()) {
                case WHAT_ON_COMMENT_SENT_TO_YOUR_AREA:
                    progressDialog.setMessage("已将评论发送至你的评论区：" + message.getObject(0, String.class));
                    break;
                case WHAT_ON_START_CHECK:
                    progressDialog.setMessage("检查中……");
                    break;
                case WHAT_THEN_ONLY_BANNED_IN_THIS_AREA:
                    progressDialog.dismiss();
                    showResult("该评论仅在此评论区被ban，因为发送在你的评论区能正常显示");
                    break;
                case WHAT_THEN_BANNED_IN_YOUR_AREA:
                    showResult("该评论不仅在此评论区被ban，因为发送在你的评论区也不能正常显示");
                    break;
            }
        }

        private void showResult(String message) {
            progressDialog.dismiss();
            new AlertDialog.Builder(worker.context)
                    .setTitle("检查结果")
                    .setMessage(message)
                    .setOnDismissListener(onDismissListener)
                    .setPositiveButton(R.string.ok, new VoidDialogInterfaceOnClickListener())
                    .show();
        }
    }

    public void scanSensitiveWord(Comment mainComment) {
        if (mainComment.comment.length() < 8) {
            toastShort("您要扫描的评论太短！至少8个字符");
            return;
        }

        final int[] commentAreaSelect = new int[]{0};
        new AlertDialog.Builder(context)
                .setTitle("请选择进行敏感扫描的评论区")
                .setSingleChoiceItems(new String[]{"自己的评论区", "使用小号转发动态生成新评论区（用完删除转发）", "当前评论区（不推荐，除非1,2选项全文通过且评论区未戒严）",}, commentAreaSelect[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        commentAreaSelect[0] = which;
                    }
                }).setNegativeButton(R.string.cancel, new VoidDialogInterfaceOnClickListener())
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                    switch (commentAreaSelect[0]) {
                        case 0:
                            CommentArea yourCommentArea = commentUtil.getYourCommentArea();
                            if (yourCommentArea == null) {
                                Toast.makeText(context, "你的评论区未设置！若关闭弹窗可从历史评论记录中选择评论扫描", Toast.LENGTH_LONG).show();
                                commentUtil.setYourCommentArea(context, commentManipulator);
                            } else {
                                SensitiveScannerHandler scannerHandler = new SensitiveScannerHandler(this, mainComment);
                                new SensitiveScannerTask(scannerHandler, mainComment, yourCommentArea,
                                        commentManipulator, config, statDB).execute();
                            }
                            break;
                        case 1:
                            if (!commentManipulator.deputyCookieAreSet()) {
                                DialogUtil.dialogMessage(context, "错误", "你没有设置小号cookie！请回主页设置（一起把被转发的动态设置了），关闭弹窗后可从历史评论记录中选择评论扫描");
                                return;
                            }
                            String dynamicId = commentUtil.getForwardDynamicId();
                            if (dynamicId != null) {
                                SensitiveScannerHandler scannerHandler = new SensitiveScannerHandler(this, mainComment);
                                new SensitiveScannerTask(scannerHandler, mainComment, dynamicId,
                                        commentManipulator, config, statDB).execute();
                            } else {
                                Toast.makeText(context, "你没有设置被转发动态！若关闭弹窗可从历史评论记录中选择评论扫描", Toast.LENGTH_LONG).show();
                                commentUtil.setDynamicIdToBeForward(context, commentManipulator);
                            }
                            break;
                        case 2:
                            SensitiveScannerHandler scannerHandler = new SensitiveScannerHandler(this, mainComment);
                            new SensitiveScannerTask(scannerHandler, mainComment, mainComment.commentArea,
                                    commentManipulator, config, statDB).execute();
                            break;
                    }
                }).show();
    }


    private static class SensitiveScannerHandler extends SensitiveScannerTask.EventHandler {
        private final DialogCommCheckWorker worker;
        private final AlertDialog dialog;
        private final Comment mainComment;
        private final String commentText;
        private final TextView txv_comment_content;
        private final ProgressBar pb_scanning_ssw;
        private final ProgressBar pb_wait;
        private final TextView txv_scanning_status;
        private final TextView txv_scanning_progress;
        Button buttonClose;
        private final ForegroundColorSpan greenSpan;
        private final ForegroundColorSpan redSpan;
        private final ForegroundColorSpan yellowSpan;
        private final ForegroundColorSpan blueSpan;

        public SensitiveScannerHandler(DialogCommCheckWorker worker, Comment mainComment) {
            super(null);
            this.worker = worker;
            this.mainComment = mainComment;
            Context context = worker.context;
            View dialogView = View.inflate(context, R.layout.dialog_scanning_sensitive_word, null);
            txv_comment_content = dialogView.findViewById(R.id.txv_scanning_result_of_sensitive_world);
            pb_scanning_ssw = dialogView.findViewById(R.id.prog_scanning_ssw);
            pb_wait = dialogView.findViewById(R.id.prog_wait);
            txv_scanning_status = dialogView.findViewById(R.id.txv_scanning_status);
            txv_scanning_progress = dialogView.findViewById(R.id.txv_scanning_progress);
            this.dialog = new AlertDialog.Builder(context)
                    .setTitle("正在扫描敏感词……")
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("关闭", null)
                    .show();
            buttonClose = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            buttonClose.setEnabled(false);
            commentText = mainComment.comment;
            txv_comment_content.setText(commentText);

            greenSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.green));
            redSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.red));
            yellowSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.yellow));
            blueSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.blue));
            setErrorHandle(new DialogErrorHandle(dialog, worker));
        }


        @SuppressLint("SetTextI18n")
        @Override
        protected void handleEvent(EventMessage message) {
            SpannableStringBuilder builder;
            switch (message.getWhat()) {
                case WHAT_COMMENT_FULL_TEXT_SENT:
                    txv_scanning_status.setText(String.format("正在检查全文(等待%sms后检查评论)……", message.getObject(0, Long.class)));
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder(commentText);
                    stringBuilder.setSpan(blueSpan, 0, commentText.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
                    txv_comment_content.setText(stringBuilder);
                    break;
                case WHAT_COMMENT_FULL_TEXT_IS_NORMAL:
                    builder = new SpannableStringBuilder(commentText);
                    builder.setSpan(greenSpan, 0, commentText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    txv_comment_content.setText(builder);
                    CommentArea commentArea = message.getObject(0, CommentArea.class);
                    if (commentArea == mainComment.commentArea) {
                        txv_scanning_status.setText("评论全文正常（在评论区:" + commentArea.sourceId + ")，请检查评论区是否戒严或者评论是否仅在那个评论区被ban？");
                    } else {
                        txv_scanning_status.setText("评论全文正常（在你的评论区)，请检查评论区是否戒严或者评论是否仅在那个评论区被ban？");
                    }
                    dialog.setTitle("扫描终止");
                    pb_scanning_ssw.setMax(1);
                    pb_scanning_ssw.setProgress(1);
                    pb_wait.setIndeterminate(false);
                    buttonClose.setEnabled(true);
                    break;
                case WHAT_ON_SEND_NEXT_COMMENT_AND_WAIT:
                    int normalPosition = message.getObject(0, Integer.class);
                    int splitLeftPosition = message.getObject(1, Integer.class);
                    int splitRightPosition = message.getObject(2, Integer.class);
                    long waitTime = message.getObject(3, Long.class);
                    builder = new SpannableStringBuilder(commentText);
                    builder.setSpan(greenSpan, 0, normalPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    builder.setSpan(yellowSpan, normalPosition, splitLeftPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    builder.setSpan(blueSpan, splitLeftPosition, splitRightPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    txv_comment_content.setText(builder);
                    txv_scanning_status.setText(String.format("发送评论&等待%sms……", waitTime));
                    break;
                case WHAT_ON_CHECKING_COMMENT:
                    int currProgress = message.getObject(0, Integer.class);
                    int max = message.getObject(1, Integer.class);
                    pb_scanning_ssw.setMax(max);
                    pb_scanning_ssw.setProgress(currProgress);
                    txv_scanning_progress.setText(String.format("%s/%s", currProgress, max));
                    txv_scanning_status.setText("检查评论……");
                    break;
                case WHAT_ON_CHECK_RESULT:
                    SensitiveScanResult result = message.getObject(0, SensitiveScanResult.class);
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(commentText);
                    spannableStringBuilder.setSpan(greenSpan, 0, result.normalPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    System.out.println(result.unusualPosition);
                    spannableStringBuilder.setSpan(redSpan, result.normalPosition, result.unusualPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    txv_comment_content.setText(spannableStringBuilder);
                    txv_scanning_status.setText("检查完成，即将进行下一检查");
                    break;
                case WHAT_ON_SCAN_COMPLETE:
                    dialog.setTitle("扫描已完成");
                    txv_scanning_status.setText("扫描结束！");
                    buttonClose.setEnabled(true);
                    buttonClose.setOnClickListener(v -> {
                        dialog.dismiss();
                    });
                    pb_wait.setIndeterminate(false);
                    worker.onCommentStatusUpdated(mainComment.rpid);
                    break;
                case WHAT_NEW_SLEEP_PROGRESS_MAX:
                    int progressMax = message.getObject(0, Integer.class);
                    if (progressMax >= 0) {
                        pb_wait.setIndeterminate(false);
                        pb_wait.setMax(progressMax);
                    } else {
                        pb_wait.setIndeterminate(true);
                    }
                    break;
                case WHAT_NEW_SLEEP_PROGRESS:
                    pb_wait.setProgress(message.getObject(0, Integer.class));
                    break;
                case WHAT_FORWARD_DYNAMIC:
                    txv_scanning_status.setText("正在使用小号转发动态以创建新的干净评论区……");
                    break;
                case WHAT_FORWARDED_DYNAMIC:
                    txv_scanning_status.setText("动态转发成功，动态ID：" + message.getObject(0, Long.class) + ",检查将在5秒后开始");
                    break;
                case WHAT_DELETE_FORWARDED_DYNAMIC:
                    txv_scanning_status.setText("正在删除小号转发的动态……");

            }
        }

    }


    public void toAppeal(String comment, CommentArea commentArea, String parent, String root, String areaIdentifier) {

    }

    private void toastShort(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void toastLong(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    private void toastNetError(String msg) {
        toastShort("网络错误：" + msg);
    }

    public void dialogMessage(String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(dialog -> exit())
                .show();
    }

    private void exit() {
        if (exitListener != null) {
            exitListener.exit();
        }
    }

    private void onNewCommentRpid(long rpid) {
        if (exitListener != null) {
            exitListener.onNewCommentRpid(rpid);
        }
    }

    private void onCommentStatusUpdated(long rpid) {
        if (exitListener != null) {
            exitListener.onCommentStatusUpdated(rpid);
        }
    }


}
