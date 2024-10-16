package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import icu.freedomIntrovert.biliSendCommAntifraud.CommentMonitoringService;
import icu.freedomIntrovert.biliSendCommAntifraud.Config;
import icu.freedomIntrovert.biliSendCommAntifraud.NotificationService;
import icu.freedomIntrovert.biliSendCommAntifraud.R;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class CommentUtil {

    public static String sourceIdToUrl(CommentArea area) {
        String url = null;
        if (area.type == CommentArea.AREA_TYPE_VIDEO) {
            url = "https://www.bilibili.com/video/" + area.sourceId;
        } else if (area.type == CommentArea.AREA_TYPE_ARTICLE) {
            url = "https://www.bilibili.com/read/" + area.sourceId;
        } else if (area.type == CommentArea.AREA_TYPE_DYNAMIC11 || area.type == CommentArea.AREA_TYPE_DYNAMIC17) {
            url = "https://t.bilibili.com/" + area.sourceId;
        }
        return url;
    }

    public static String omitComment(String comment, int length) {
        if (comment.length() > length) {
            return comment.substring(0, length - 2) + "……";
        } else {
            return comment;
        }
    }

    public static void toMonitoringComment(Context context, HistoryComment historyComment){
        Config config = Config.getInstance(context);
        StatisticsDBOpenHelper db =StatisticsDBOpenHelper.getInstance(context);
        if (!NotificationService.checkOrRequestNotificationPermission(context)) {
            return;
        }
        if (!config.getRecordeHistory()){
            new AlertDialog.Builder(context)
                    .setTitle("需要开启历史评论记录")
                    .setMessage("点击确定开启历史评论记录，并开始监控评论")
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        //由于之前没有启用历史评论，现在将传来的评论插入数据库
                        db.insertHistoryComment(historyComment);
                        startMonitoringComment(context,historyComment.rpid);
                    }).show();
            return;
        }
        startMonitoringComment(context, historyComment.rpid);
    }

    @SuppressLint("SetTextI18n")
    private static void startMonitoringComment(Context context, long rpid){
        View dialogView = View.inflate(context,R.layout.edit_text,null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        editText.setText("30");
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("监控超时限制（分钟）")
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String input = editText.getText().toString();
            if (TextUtils.isEmpty(input)){
                editText.setError("请输入时间！");
                return;
            }
            Intent serviceIntent = new Intent(context, CommentMonitoringService.class);
            serviceIntent.putExtra("rpid",rpid);
            serviceIntent.putExtra("timeout",Integer.parseInt(input));
            ContextCompat.startForegroundService(context, serviceIntent);
            Toast.makeText(context, "监控已开始，状态改变时通知您，下拉通知栏可查看进度", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

    }


}
