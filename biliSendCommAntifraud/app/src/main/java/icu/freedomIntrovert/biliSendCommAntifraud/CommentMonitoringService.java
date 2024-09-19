package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.LinkedList;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.URCommentMonitoringTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class CommentMonitoringService extends Service {
    private Notification fsNotification;
    private NotificationManager notificationManager;
    private NotificationService notificationService;
    private int foregroundServiceId;
    List<URCommentMonitoringTask> tasks = new LinkedList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        notificationService = NotificationService.getInstance(this);
        notificationManager = notificationService.notificationManager;
        foregroundServiceId = notificationService.createNewId();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            if (!extras.containsKey("rpid")) {
                Toast.makeText(this, "需要rpid Extra", Toast.LENGTH_SHORT).show();
                return START_NOT_STICKY;
            }
            long rpid = extras.getLong("rpid");
            //避免重复启动
            for (URCommentMonitoringTask task : tasks) {
                if (task.comment.rpid == rpid) {
                    Toast.makeText(this, "该评论已在监控中", Toast.LENGTH_LONG).show();
                    return START_STICKY;
                }
            }
            StatisticsDBOpenHelper db = StatisticsDBOpenHelper.getInstance(this);
            HistoryComment historyComment = db.getHistoryComment(rpid);
            System.out.println(historyComment);
            if (historyComment != null) {
                startMonitoring(historyComment); // 开始监控
            } else {
                System.err.println("未找到评论rpid：" + rpid);
                Toast.makeText(this, "未找到评论rpid：" + rpid, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "评论监控服务启动参数错误", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 后台线程来监控
    private void startMonitoring(HistoryComment comment) {
        final int nId = notificationService.createNewId();
        postStateNotification(nId, "准备监控评论：" + comment.comment, 0, 0, true);
        URCommentMonitoringTask task = new URCommentMonitoringTask(this, comment, new URCommentMonitoringTask.EventHandler() {
            @Override
            public void onWaiting(int max, int progressSecond, int minute) {
                postStateNotification(nId, String.format("已监控%s分钟，%s秒后更新状态，评论：%s", minute, max - progressSecond, comment.comment),
                        max, progressSecond, false);
            }

            @Override
            public void onStartCheck() {
                postStateNotification(nId, "正在更新评论状态，评论：" + comment.comment,
                        0, 0, true);
            }

            @Override
            public void onStateNotChange() {
                postStateNotification(nId, "状态未改变，评论：" + comment.comment,
                        0, 0, true);
            }

            @Override
            public void onStateChange(HistoryComment historyComment,int minute, URCommentMonitoringTask task) {
                tasks.remove(task);
                postResult(nId, "评论状态改变",
                        String.format("状态变更为：%s，监控用时：%s分钟，评论：%s", HistoryComment.getStateDesc(historyComment.lastState),
                                minute,
                                historyComment.comment));
                updateForeground();
            }

            @Override
            public void onAreaDead(URCommentMonitoringTask task, HistoryComment comment) {
                tasks.remove(task);
                postResult(nId, "评论区寄了",
                        String.format("评论所在评论：%s 已失效，评论：%s", comment.commentArea.sourceId,
                                comment.comment));
                updateForeground();
            }

            @Override
            public void onNoAccount(long uid, URCommentMonitoringTask task) {
                tasks.remove(task);
                postResult(nId,"错误","未找到账号："+uid);
                updateForeground();
            }

            @Override
            public void onError(Throwable th, URCommentMonitoringTask task) {
                tasks.remove(task);
                postResult(nId,"检查时发生异常",th.getMessage());
                updateForeground();
            }

            @Override
            public void onTimeOut1Hour(HistoryComment historyComment, URCommentMonitoringTask task) {
                tasks.remove(task);
                postResult(nId,"检查已超时","已超过一小时，评论状态未发生变化，监控停止。评论："+historyComment.comment);
                updateForeground();
            }
        });
        tasks.add(task);
        updateForeground();
        task.execute();
    }

    private void postResult(int id, String title, String message) {
        notificationManager.cancel(id);//取消进度条的通知
        Intent intent = new Intent(this, HistoryCommentActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_BACKGROUND_TASK_RESULT)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.launcher);
        notificationManager.notify(id, builder.build());
    }

    public void postStateNotification(int id, String message, int max, int progress, boolean indeterminate) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_BACKGROUND_TASK)
                .setContentTitle("评论状态监控")
                .setContentText(message)
                .setSmallIcon(R.drawable.launcher)
                .setProgress(max, progress, indeterminate)  // 设置通知进度条
                .setOngoing(true);  // 使通知不可移除
        notificationManager.notify(id, builder.build());
    }

    //根据任务列表余量决定是POST一个前台任务的通知还是移除前台任务通知
    public void updateForeground() {
        if (tasks.isEmpty()) {
            stopForeground(true);
            fsNotification = null;
        } else {
            if (fsNotification == null) {
                fsNotification = new NotificationCompat.Builder(this, NotificationService.CHANNEL_BACKGROUND_TASK)
                        .setContentTitle("评论状态监控服务正在运行……")
                        .setPriority(Notification.PRIORITY_LOW)
                        .setSmallIcon(R.drawable.launcher)
                        .setOngoing(true)
                        .build();
                startForeground(foregroundServiceId, fsNotification);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
