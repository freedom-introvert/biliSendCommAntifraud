package icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;

public class AppealDialogPresenter {
    Context context;
    Handler handler;
    CommentManipulator commentManipulator;

    public AppealDialogPresenter(Context context, Handler handler, CommentManipulator commentManipulator) {
        this.context = context;
        this.handler = handler;
        this.commentManipulator = commentManipulator;
    }

    public void appeal(String areaIdentifier, String comment,CallBack callBack){
        View dialogView = View.inflate(context, R.layout.dialog_appeal_comment, null);
        EditText edt_appeal_area_location = dialogView.findViewById(R.id.edt_appeal_area_location);
        EditText edt_reason = dialogView.findViewById(R.id.edt_reason);
        edt_appeal_area_location.setText(areaIdentifier);
        edt_reason.setText("评论内容:" + CommentUtil.subComment(comment,93));
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
                } else if (edt_reason.getText().toString().length() < 10){
                    edt_reason.setError("申诉理由要大于10个字");
                } else if (edt_reason.getText().toString().length() > 99){
                    edt_reason.setError("申诉理由不能超过99个字");
                } else {
                    new Thread(() -> {
                            try {
                                JSONObject appealRespJson = commentManipulator.appealComment(edt_appeal_area_location.getText().toString(), edt_reason.getText().toString());
                                int code = appealRespJson.getInteger("code");
                                String respMsg;
                                if (code == 0){
                                    respMsg = appealRespJson.getJSONObject("data").getString("success_toast");
                                } else {
                                    respMsg = appealRespJson.getString("message");
                                }
                                handler.post(() -> {
                                    editAppealInfoDialog.dismiss();
                                    callBack.onRespInUI(code,respMsg);
                                });
                            } catch (IOException e) {
                                handler.post(() -> {
                                    editAppealInfoDialog.dismiss();
                                    callBack.onNetErrInUI(e.getMessage());
                                });
                            }
                    }).start();
                }
            }
        });
    }

    public interface CallBack {
        public void onRespInUI(int code, String toastText);
        public void onNetErrInUI(String msg);
    }

}
