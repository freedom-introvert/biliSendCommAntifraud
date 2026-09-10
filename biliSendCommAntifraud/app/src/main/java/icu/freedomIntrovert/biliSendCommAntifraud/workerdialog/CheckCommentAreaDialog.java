package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.app.AlertDialog;
import android.content.Context;

import java.util.concurrent.atomic.AtomicInteger;

import icu.freedomIntrovert.biliSendCommAntifraud.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.AreaMartialLawCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.BannedOnlyInThisAreaCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;

public class CheckCommentAreaDialog {
    Context context;
    AccountManger accountManger;

    public CheckCommentAreaDialog(Context context) {
        this.context = context;
        accountManger = AccountManger.getInstance(context);
    }

    public void show(Comment comment){
        Account[] accounts = accountManger.getAccounts().toArray(new Account[0]);
        String[] accountDescriptions = new String[accounts.length];
        AtomicInteger selected = new AtomicInteger();
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] == accountManger.getAccount(comment.uid)){
                selected.set(i);
                accountDescriptions[i] = accounts[i].toString() + " [当前]";
            } else {
                accountDescriptions[i] = accounts[i].toString();
            }
        }
        new AlertDialog.Builder(context)
                .setTitle("请选择检查评论区的账号")
                .setSingleChoiceItems(accountDescriptions, selected.get(),
                        (dialog, which) -> selected.set(which))
                .setPositiveButton(R.string.ok, (dialog, which) ->
                        checkML(comment,accounts[selected.get()]))
                .setNegativeButton(R.string.cancel, new VoidDialogInterfaceOnClickListener())
                .show();
    }

    private void checkML(Comment comment, Account account){
        ProgressBarDialog dialog = new ProgressBarDialog.Builder(context)
                .setTitle("检查评论是否戒严区中")
                .setMessage("等待检查")
                .setIndeterminate(true)
                .setCancelable(false)
                .show();
        new AreaMartialLawCheckTask(context, account, comment, new AreaMartialLawCheckTask.EventHandler() {
            @Override
            public void onTestCommentSent(String testComment, int maxProgress) {
                dialog.setMessage(String.format("已发送测试评论『%s』到当前评论区，等待%sms后检查",testComment,maxProgress));
                dialog.setMax(maxProgress);
                dialog.setIndeterminate(false);
            }

            @Override
            public void onWaitProgress(int progress) {
                dialog.setProgress(progress);
            }

            @Override
            public void onStartCheck() {
                dialog.setMessage("检查评论中");
            }

            @Override
            public void onAreaOk() {
                dialog.dismiss();
                new AlertDialog.Builder(context)
                        .setTitle("评论区检查结果")
                        .setMessage("评论区没有戒严，是否继续检查该评论是否仅在此评论区被ban？")
                        .setPositiveButton("检查", (dialog1, which) -> {
                            checkBannedOnlyInThisArea(comment,account);
                        })
                        .setNegativeButton("不了",null)
                        .show();
            }

            @Override
            public void onMartialLaw() {
                dialog.dismiss();
                DialogUtil.dialogMessage(context,"评论区检查结果","评论区被戒严（仅供参考，请考虑『账号-内容域』，建议使用多账号证明）");
            }

            @Override
            public void onError(Throwable th) {
                dialog.dismiss();
                DialogUtil.dialogError(context,th);
            }
        }).execute();
    }

    private void checkBannedOnlyInThisArea(Comment comment, Account account){
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("检测评论是否仅在该评论区被ban")
                .setMessage("等待检查中……")
                .setCancelable(false)
                .show();
        new BannedOnlyInThisAreaCheckTask(context, comment, account, new BannedOnlyInThisAreaCheckTask.EventHandler() {
            @Override
            public void onAccountCommentAreaNotSet(Account account) {
                dialog.dismiss();
                DialogUtil.dialogMessage(context,"错误","你的账号 "+account+"未设置评论区，请到账号管理设置评论区后回到历史评论记录来检查");
            }

            @Override
            public void onCommentSentToYourArea(CommentArea commentArea, int waitTime) {
                dialog.setMessage("已将评论发送至你的评论区：" + commentArea.sourceId+"\n等待"+waitTime+"ms后检查评论……");
            }

            @Override
            public void onStartCheck() {
                dialog.setMessage("检查中……");
            }

            @Override
            public void thenOnlyBannedInThisArea() {
                showResult("该评论仅在此评论区被ban，因为发送在你的评论区能正常显示");
            }

            @Override
            public void thenBannedInYourArea() {
                showResult("该评论不仅在此评论区被ban，因为发送在你的评论区也不能正常显示");
            }

            @Override
            public void onError(Throwable th) {
                dialog.dismiss();
                DialogUtil.dialogError(context,th);
            }

            private void showResult(String message) {
                dialog.dismiss();
                new AlertDialog.Builder(context)
                        .setTitle("检查结果")
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        }).execute();
    }
}
