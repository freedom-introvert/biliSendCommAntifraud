package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;

import icu.freedomIntrovert.biliSendCommAntifraud.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.async.CommentAppealTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;

public class AppealCommentDialog {

    public static void show(Context context, Comment comment, ResultCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("警告")
                .setMessage("申诉前请不要在此评论区进行敏感词扫描、戒严检测等重复发送再删除评论的操作，会污染评论区影响申诉（比如恢复了测试评论）！\n" +
                        "申诉依赖于:https://www.bilibili.com/h5/comment/appeal")
                .setNegativeButton("还是算了", null)
                .setNeutralButton("官方申诉网址", (dialog23, which1) -> {
                    Uri uri = Uri.parse("https://www.bilibili.com/h5/comment/appeal");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    context.startActivity(intent);
                })
                .setPositiveButton("去申诉", (dialog, which) -> {
                    toAppeal(context, comment, callback);
                }).show();
    }

    @SuppressLint("SetTextI18n")
    public static void toAppeal(Context context, Comment comment, ResultCallback callback) {
        View dialogView = View.inflate(context, R.layout.dialog_appeal_comment, null);
        EditText edt_appeal_area_location = dialogView.findViewById(R.id.edt_appeal_area_location);
        EditText edt_reason = dialogView.findViewById(R.id.edt_reason);
        edt_appeal_area_location.setText(comment.commentArea.toSourceUrl());
        //评论区地址不能更改
        edt_appeal_area_location.setEnabled(false);
        edt_reason.setText("评论内容:" + comment.omitCommentText(93));
        AlertDialog editAppealInfoDialog = new AlertDialog.Builder(context)
                .setTitle("填写申诉信息")
                .setView(dialogView)
                .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                .setPositiveButton("确定", null)
                .show();
        editAppealInfoDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edt_appeal_area_location.getText().toString().equals("")) {
                    edt_appeal_area_location.setError("请输入所在稿件BV号或位置链接");
                } else if (edt_reason.getText().toString().length() < 10) {
                    edt_reason.setError("申诉理由要大于10个字");
                } else if (edt_reason.getText().toString().length() > 99) {
                    edt_reason.setError("申诉理由不能超过99个字");
                } else {
                    editAppealInfoDialog.dismiss();
                    new CommentAppealTask(context, comment,
                            edt_appeal_area_location.getText().toString(),
                            edt_reason.getText().toString(), callback).execute();
                }
            }
        });

    }

    public static abstract class ResultCallback implements CommentAppealTask.EventHandler {
        Context context;
        public ResultCallback(Context context) {
            this.context = context;
        }
        @Override
        public void onSuccess(String successToast) {
            DialogUtil.dialogMessage(context, "申诉结果", successToast);
        }

        @Override
        public void onNoCommentToAppeal(String successToast) {
            DialogUtil.dialogMessage(context, "申诉结果", successToast + "\n可能因为检查评论时误判了或评论在某种处理或审核状态，等待一段时间后应该可以显示");
        }

        @Override
        public void onError(Throwable th) {
            DialogUtil.dialogMessage(context, "错误", th.toString());
        }
    }
}
