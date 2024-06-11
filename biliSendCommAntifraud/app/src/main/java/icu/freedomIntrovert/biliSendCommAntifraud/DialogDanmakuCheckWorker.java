package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuPresenter;

public class DialogDanmakuCheckWorker {
    private Context context;
    private Handler handler;
    private DanmakuPresenter danmakuPresenter;
    private OnExitListener exitListener;

    public DialogDanmakuCheckWorker(Context context, Handler handler, DanmakuPresenter danmakuPresenter,OnExitListener exitListener) {
        this.context = context;
        this.handler = handler;
        this.danmakuPresenter = danmakuPresenter;
        this.exitListener = exitListener;
    }

    public void startCheckDanmaku(long oid, long dmid, String content, String accessKey, long avid){
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "检查中", "准备检查弹幕……");
        progressDialog.setCancelable(false);
        progressDialog.show();
        danmakuPresenter.checkDanmaku(oid, dmid, content, accessKey, avid, new DanmakuPresenter.CheckDanmakuCallBack() {
            @Override
            public void onSleeping(long waitTime) {
                progressDialog.setMessage("等待"+waitTime+"ms后检查弹幕……");
            }

            @Override
            public void onGettingHasAccountDMList() {
                progressDialog.setMessage("未登录弹幕列表没有找到弹幕，正在获取登录状态弹幕列表……");
            }

            @Override
            public void onGettingNoAccountDMList() {
                progressDialog.setMessage("正在获取弹幕列表（未登录）……");
            }

            @Override
            public void thenOk() {
                progressDialog.dismiss();
                showCheckResult("你的弹幕“"+ CommentUtil.subComment(content,50)+"”正常显示！");
            }

            @Override
            public void thenDeleted() {
                progressDialog.dismiss();
                showCheckResult("你的弹幕“"+ CommentUtil.subComment(content,50)+"”被系统删除！");
            }

            @Override
            public void thenShadowBan() {
                progressDialog.dismiss();
                showCheckResult("你的弹幕“"+ CommentUtil.subComment(content,50)+"”仅自己可见！");
            }

            @Override
            public void onNetworkError(Throwable th) {
                progressDialog.dismiss();
            }
        });
    }

    private void showCheckResult(String message){
        new AlertDialog.Builder(context)
                .setTitle("检查结果")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitListener.exit();
                    }
                })
                .show();
    }


}
