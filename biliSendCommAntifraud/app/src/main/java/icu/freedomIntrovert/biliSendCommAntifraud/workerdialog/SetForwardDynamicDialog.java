package icu.freedomIntrovert.biliSendCommAntifraud.workerdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.ForwardDynamic;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class SetForwardDynamicDialog {

    public static void show(Context context) {
        Config config = Config.getInstance(context);
        CommentManipulator commentManipulator = CommentManipulator.getInstance();
        ForwardDynamic forwardDynamic = config.getForwardDynamic();
        View dialogView = View.inflate(context, R.layout.edit_text, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(forwardDynamic != null ? forwardDynamic.forwardDynamicUrl : "");
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("被转发动态")
                .setMessage("填动态链接，建议填抽奖动态，用于扫描敏感词时选择非主号转发来创建新评论区")
                .setView(dialogView)
                .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                .setPositiveButton("设置", null).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> commentManipulator.matchCommentAreaInUi(editText.getText().toString(),
                AccountManger.getInstance(context).random(),
                new CommentManipulator.MatchCommentAreaCallBack() {
                    @Override
                    public void onNetworkError(IOException e) {
                        Toast.makeText(context, "网络错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onMatchedArea(CommentArea commentArea) {
                        if (commentArea != null) {
                            if (commentArea.type == CommentArea.AREA_TYPE_DYNAMIC17
                                    || commentArea.type == CommentArea.AREA_TYPE_DYNAMIC11) {
                                config.setForwardDynamic(new ForwardDynamic(editText.getText().toString(), commentArea.sourceId));
                                dialog.dismiss();
                                Toast.makeText(context, "设置成功！", Toast.LENGTH_SHORT).show();
                            } else {
                                editText.setError("这不是动态的链接！");
                            }
                        } else {
                            editText.setError("输入的内容未解析到评论区！");
                        }
                    }

                    @Override
                    public void onApiError(BiliBiliApiException e) {
                        DialogUtil.dialogMessage(context,"API错误",e.getMessage());
                    }
                }));
    }
}
