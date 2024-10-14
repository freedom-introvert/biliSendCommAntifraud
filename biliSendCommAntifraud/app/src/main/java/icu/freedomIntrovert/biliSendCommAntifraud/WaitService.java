package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

import icu.freedomIntrovert.biliSendCommAntifraud.async.CountdownTask;

public class WaitService extends Service {

    private NotificationService notificationService;
    private NotificationManager notificationManager;
    private int foregroundServiceId;

    public WaitService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationService = NotificationService.getInstance(this);
        notificationManager = notificationService.notificationManager;
        foregroundServiceId = notificationService.createNewId();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int nId = notificationService.createNewId();
        int waitSeconds = intent.getIntExtra("wait_seconds", 5);
        System.out.println("等待时间："+waitSeconds);
        long rpid = intent.getLongExtra("rpid", -1);
        String comment = intent.getStringExtra("comment");
        ArrayList<String> cookies = intent.getStringArrayListExtra("cookies");
        new CountdownTask(waitSeconds, new CountdownTask.EventHandler() {
            @Override
            public void onProgress(int max, int progress) {
                postProgress(nId,comment,max,progress);
            }

            @Override
            public void onComplete(CountdownTask task) {
                postOver(nId,rpid,cookies,comment);
            }
        }).execute();
        return super.onStartCommand(intent, flags, startId);
    }

    private void postProgress(int id, String comment, int max, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_BACKGROUND_TASK)
                .setContentTitle("等待剩余" + (max - progress) + "秒")
                .setContentText(comment)
                .setSmallIcon(R.drawable.launcher)
                .setProgress(max, progress, false)  // 设置通知进度条
                .setOngoing(true);  // 使通知不可移除
        notificationManager.notify(id, builder.build());
    }

    private void postOver(int id, long rpid,ArrayList<String> cookies,String comment) {
        notificationManager.cancel(id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_BACKGROUND_TASK_RESULT);
        Intent intent = new Intent(this, ByXposedLaunchedActivity.class);
        intent.putExtra("action", ByXposedLaunchedActivity.ACTION_RESUME_CHECK_COMMENT);
        intent.putExtra("rpid", rpid);
        intent.putExtra("cookies", cookies);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_MUTABLE);
        builder.setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle("已完成等待，点击此通知继续检查")
                .setContentText(comment != null ? comment : "null")
                .setAutoCancel(true);
        notificationManager.notify(id, builder.build());
    }

}