package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import icu.freedomIntrovert.biliSendCommAntifraud.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.RandomCommentGenerateTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.RandomComments;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;

public class SetRandomCommentsDialog {
    public static void show(Context context){
        RandomComments randomComments = RandomComments.getInstance(context);
        View edtView = View.inflate(context, R.layout.edit_text, null);
        EditText editText = edtView.findViewById(R.id.edit_text);
        editText.setHint("用换行符分割，一行一个，越多越好");
        editText.setText(randomComments.getSourceRandomComments());
        AlertDialog setRandomDialog = new AlertDialog.Builder(context)
                .setTitle("随机测试评论池（测试评论区是否戒严等功能所使用）")
                .setView(edtView).setPositiveButton("设置", null)
                .setNeutralButton("随机生成", null)
                .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                .show();
        setRandomDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            if (editText.getText().toString().split("\n").length >= 3) {
                randomComments.updateRandomComments(editText.getText().toString());
                setRandomDialog.dismiss();
            } else {
                editText.setError("最少输入4条测试评论！");
            }
        });

        setRandomDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v12 -> {
            View dialogView = View.inflate(context,R.layout.dialog_generate_random_comment,null);
            EditText editCommentAreaLocation = dialogView.findViewById(R.id.edit_comment_area_location);
            EditText editMinLength = dialogView.findViewById(R.id.edit_length_min);
            EditText editMaxLength = dialogView.findViewById(R.id.edit_length_max);
            EditText editQuantity = dialogView.findViewById(R.id.edit_generate_quantity);

            Spinner spinner = dialogView.findViewById(R.id.account_spinner);
            spinner.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item, AccountManger.getInstance(context).getAccounts()));
            AlertDialog dialog= new AlertDialog.Builder(context)
                    .setTitle("随机生成评论")
                    .setView(dialogView)
                    .setPositiveButton("开始生成", null)
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .show();
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String commentAreaLocation = editCommentAreaLocation.getText().toString();
                String minLength =  editMinLength.getText().toString();
                String maxLength = editMaxLength.getText().toString();
                String quantityStr = editQuantity.getText().toString();
                if (TextUtils.isEmpty(commentAreaLocation)){
                    editCommentAreaLocation.setError("请填写评论区地址");
                    return;
                }
                if (TextUtils.isEmpty(minLength)){
                    editMinLength.setError("请填写最小长度");
                    return;
                }
                if (TextUtils.isEmpty(maxLength)){
                    editMaxLength.setError("请填写最大长度");
                    return;
                }

                if (TextUtils.isEmpty(quantityStr)){
                    editQuantity.setError("请填写生成数量");
                    return;
                }
                int min = Integer.parseInt(minLength);
                int max = Integer.parseInt(maxLength);
                int quantity = Integer.parseInt(quantityStr);
                if (min < 5){
                    editMinLength.setError("最小长度不能小于5");
                    return;
                }
                if (max > 20){
                    editMaxLength.setError("最大长度不能大于20");
                }
                if (max < min){
                    Toast.makeText(context, "最大长度不能小于最小长度", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (quantity < 1){
                    editQuantity.setError("生成数量不能小于1");
                    return;
                }
                if (quantity > 30){
                    editQuantity.setError("最多生成30个");
                    return;
                }
                dialog.dismiss();
                new RandomCommentGenerateTask(context, (Account) spinner.getSelectedItem(),
                        commentAreaLocation,min, max, quantity, new RCGHandler(editText)).execute();
            });

        });
    }

    private static class RCGHandler implements RandomCommentGenerateTask.EventHandler{

        EditText editText;
        ProgressBarDialog dialog;
        Context context;

        public RCGHandler(EditText editText) {
            this.editText = editText;
            this.context = editText.getContext();
            dialog = new ProgressBarDialog.Builder(context)
                    .setTitle("随机评论生成")
                    .setMessage("准备生成评论……")
                    .setCancelable(false)
                    .setMax(5000)
                    .setIndeterminate(true)
                    .show();
        }

        @Override
        public void onCommentAreaNoMatch() {
            dialog.dismiss();
            Toast.makeText(editText.getContext(), "你输入的评论区地址未匹配到评论区", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onGeneratedAndSentComment(String comment, int quantity, int index) {
            dialog.setMessage(String.format("已生成评论并发送:[%s]\n等待5秒后检查是否被Ban\n进度: %s/%s", comment, index, quantity));
            dialog.setIndeterminate(false);
        }

        @Override
        public void onWaitProgress(int time) {
            dialog.setProgress(time);
        }

        @Override
        public void onCheckingComment() {
            dialog.setMessage("正在检查评论是否被Ban");
            dialog.setIndeterminate(true);
        }

        @Override
        public void onCommentBanned(BiliComment reply) {
            dialog.setMessage("评论被Ban了，撤回进度，从新生成");
        }

        @Override
        public void onCommentOk(BiliComment reply) {
            dialog.setMessage("评论正常");
        }

        @Override
        public void onResult(ArrayList<String> result) {
            dialog.dismiss();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < result.size() - 1; i++) {
                sb.append(result.get(i)).append("\n");
            }
            sb.append(result.get(result.size() - 1));
            editText.setText(sb);
        }

        @Override
        public void onError(Throwable th) {
            dialog.dismiss();
            DialogUtil.dialogError(context, th);
        }
    }
}
