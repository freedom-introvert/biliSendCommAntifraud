package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import icu.freedomIntrovert.biliSendCommAntifraud.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.SensitiveScannerTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;

public class ScanSensitiveWordDialog {

    public static void show(Context context, Comment comment) {
        show(context, comment, null);
    }

    public static void show(Context context, Comment comment, @Nullable CallBack callBack) {
        if (comment.comment.length() < 8) {
            Toast.makeText(context, "您要扫描的评论太短！至少8个字符", Toast.LENGTH_LONG).show();
            return;
        }
        AtomicInteger selected = new AtomicInteger(0);
        new AlertDialog.Builder(context)
                .setTitle("请选择进行敏感扫描的评论区")
                .setSingleChoiceItems(new String[]{"自己的评论区",
                                "使用小号转发动态生成新评论区（用完删除转发）",
                                "当前评论区（不推荐，除非1,2选项全文通过且评论区未戒严）"},
                        0, (dialog, which) -> selected.set(which))
                .setNegativeButton(R.string.cancel, new VoidDialogInterfaceOnClickListener())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    switch (selected.get()) {
                        case 0:
                            AccountManger accountManger = AccountManger.getInstance(context);
                            Account account = accountManger.getAccount(comment.uid);
                            if (account == null) {
                                Toast.makeText(context, "未找到该评论所属的用户UID：" + comment.uid, Toast.LENGTH_LONG).show();
                                return;
                            } else if (account.accountCommentArea == null) {
                                Toast.makeText(context, String.format("用户 %s（%s） 评论区未设置！请设置后回到历史评论来扫描", account.uname, account.uid), Toast.LENGTH_LONG).show();
                                return;
                            }
                            toScan(context, comment, null, account.accountCommentArea, callBack);
                            break;
                        case 1:
                            showSelectForwardAccountDialog(context, comment, callBack);
                            break;
                        case 2:
                            toScan(context, comment, null, comment.commentArea, callBack);
                    }
                }).show();
    }

    private static void showSelectForwardAccountDialog(Context context, Comment comment, CallBack callBack) {
        AccountSelectionDialog.show(context, "选择转发动态的账号", comment.uid, account ->
                toScan(context, comment, account, null, callBack));
    }

    private static void toScan(Context context, Comment comment, Account forwardDynamicAccount, CommentArea commentArea, CallBack callBack) {
        SensitiveScannerTask sensitiveScannerTask = new SensitiveScannerTask(context, comment,
                forwardDynamicAccount, commentArea, new ScanEventHandler(context, comment, callBack));
        sensitiveScannerTask.execute();
    }

    private static class ScanEventHandler implements SensitiveScannerTask.EventHandler {
        private final AlertDialog dialog;
        private final Comment comment;
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
        private final CallBack callBack;
        private final Context context;

        public ScanEventHandler(Context context, Comment comment, CallBack callBack) {
            this.comment = comment;
            this.callBack = callBack;
            this.context = context;
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
            commentText = this.comment.comment;
            txv_comment_content.setText(commentText);

            greenSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.green));
            redSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.red));
            yellowSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.yellow));
            blueSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.blue));
        }

        @Override
        public void onCommentAccountNotFound(long uid) {
            dialog.dismiss();
            Toast.makeText(context, "评论用户不存在，UID：" + uid, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNotSetForwardDynamic() {
            dialog.dismiss();
            Toast.makeText(context, "你未设置被转发动态，请到主页设置（返回历史评论来扫描）", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCheckingCommentFullText(long waitTime) {
            txv_scanning_status.setText(String.format("正在检查全文(等待%sms后检查评论)……", waitTime));
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(commentText);
            stringBuilder.setSpan(yellowSpan, 0, commentText.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
            txv_comment_content.setText(stringBuilder);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onCommentFullTextIsNormal(CommentArea commentArea) {
            SpannableStringBuilder builder = new SpannableStringBuilder(commentText);
            builder.setSpan(greenSpan, 0, commentText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            txv_comment_content.setText(builder);
            txv_scanning_status.setText("评论全文正常（在评论区:" + commentArea.sourceId + ")，请检查评论区是否戒严或者评论是否仅在那个评论区被ban？");
            dialog.setTitle("扫描终止");
            pb_scanning_ssw.setMax(1);
            pb_scanning_ssw.setProgress(1);
            pb_wait.setIndeterminate(false);
            buttonClose.setEnabled(true);
        }

        @Override
        public void onNewSleepProgressMax(int max) {
            if (max >= 0) {
                pb_wait.setIndeterminate(false);
                pb_wait.setMax(max);
            } else {
                pb_wait.setIndeterminate(true);
            }
        }

        @Override
        public void onNewSleepProgress(int progress) {
            pb_wait.setProgress(progress);
        }

        @Override
        public void onForwardDynamic() {
            txv_scanning_status.setText("正在使用小号转发动态以创建新的干净评论区……");
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onForwardedDynamic(long dynRid) {
            txv_scanning_status.setText("动态转发成功，动态ID：" + dynRid + ",检查将在5秒后开始");
        }

        @Override
        public void onDeleteForwardedDynamic(String dynRid) {
            txv_scanning_status.setText("正在删除小号转发的动态……");
        }

        @Override
        public void onSendNextCommentAndWait(int normalPosition, int splitLeftPosition, int splitRightPosition, long waitTime) {
            SpannableStringBuilder builder = new SpannableStringBuilder(commentText);
            builder.setSpan(greenSpan, 0, normalPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            builder.setSpan(yellowSpan, normalPosition, splitLeftPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            builder.setSpan(blueSpan, splitLeftPosition, splitRightPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            txv_comment_content.setText(builder);
            txv_scanning_status.setText(String.format("发送评论&等待%sms……", waitTime));
        }

        @Override
        public void onCheckingComment(int currProgress, int max) {
            pb_scanning_ssw.setMax(max);
            pb_scanning_ssw.setProgress(currProgress);
            txv_scanning_progress.setText(String.format("%s/%s", currProgress, max));
            txv_scanning_status.setText("检查评论……");
        }

        @Override
        public void onCheckResult(SensitiveScanResult result) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(commentText);
            spannableStringBuilder.setSpan(greenSpan, 0, result.normalPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            System.out.println(result.unusualPosition);
            spannableStringBuilder.setSpan(redSpan, result.normalPosition, result.unusualPosition, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            txv_comment_content.setText(spannableStringBuilder);
            txv_scanning_status.setText("检查完成，即将进行下一检查");
        }

        @Override
        public void onScanComplete() {
            dialog.setTitle("扫描已完成");
            txv_scanning_status.setText("扫描结束！（红色部分不是敏感词，只是粗略位置！）");
            buttonClose.setEnabled(true);
            buttonClose.setOnClickListener(v -> dialog.dismiss());
            pb_wait.setIndeterminate(false);
            if (callBack != null) {
                callBack.onScanComplete(comment);
            }
        }

        @Override
        public void onError(Throwable th) {
            dialog.dismiss();
            DialogUtil.dialogError(context, th);
        }
    }

    public interface CallBack {
        void onScanComplete(Comment comment);
    }


}
