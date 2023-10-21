package icu.freedomIntrovert.biliSendCommAntifraud;

import static android.app.Notification.VISIBILITY_SECRET;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.Set;

public class WaitService extends Service {
    public static final int ID_WAIT_PROGRESS = 1;
    public static final int ID_WAIT_OVER = 2;
    public static final String CHANNEL_ID_TIMER = "timer";
    public static final String CHANNEL_ID_OVER = "over";
    long waitTime;
    Handler handler;
    NotificationCompat.Builder builder;
    NotificationManager manager;
    Bundle checkExtra;

    public WaitService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_TIMER)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle("等待中")
                .setContentText("")
                .setOngoing(true)
                .setProgress(100, 0, false);
        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {//不保证兼容老版本安卓，若不兼容请发issues
            notificationChannel = new NotificationChannel(CHANNEL_ID_TIMER, "定时器", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setVibrationPattern(new long[]{100, 100, 200});//设置震动模式
            notificationChannel.setLockscreenVisibility(VISIBILITY_SECRET);//锁屏显示通知
            notificationChannel.enableLights(true);//闪光灯
            notificationChannel.setShowBadge(true);
            notificationChannel.enableVibration(true);//是否允许震动
            manager.createNotificationChannel(notificationChannel);
            notificationChannel = new NotificationChannel(CHANNEL_ID_OVER, "等待结束", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setVibrationPattern(new long[]{100, 100, 200});//设置震动模式
            notificationChannel.setLockscreenVisibility(VISIBILITY_SECRET);//锁屏显示通知
            notificationChannel.enableLights(true);//闪光灯
            notificationChannel.setShowBadge(true);
            notificationChannel.enableVibration(true);//是否允许震动
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        waitTime = intent.getLongExtra("wait_time", 5000);
        if (waitTime < 100) {
            waitTime = 100;
        }
        checkExtra = intent.getBundleExtra("check_extras");
        System.out.println("检查Extras：" + checkExtra);
        manager.notify(ID_WAIT_PROGRESS, builder.build());
        startTimer();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startTimer() {
        new Thread(() -> {
            int graduation = 100;
            long sleepSeg = waitTime / graduation;
            for (int i = 0; i <= graduation; i++) {
                try {
                    Thread.sleep(sleepSeg);
                    int finalI = i;
                    handler.post(() -> {
                        builder.setProgress(graduation, finalI, false);
                        builder.setContentText(finalI * sleepSeg + "/" + graduation * sleepSeg + "ms");
                        manager.notify(ID_WAIT_PROGRESS, builder.build());
                        if (finalI == graduation) {
                            manager.cancel(ID_WAIT_PROGRESS);
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            waitOver();
        }).start();
    }

    @SuppressLint("MissingPermission")
    private void waitOver() {
        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(this, CHANNEL_ID_OVER);
        Intent intent = new Intent(this, ByXposedLaunchedActivity.class);
        intent.putExtra("todo", ByXposedLaunchedActivity.TODO_CONTINUE_CHECK_COMMENT);
        intent.putExtra("check_extras", checkExtra);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (checkExtra != null) {
            Set<String> keySet = checkExtra.keySet();
            for (String key : keySet) {
                Object value = checkExtra.get(key);
                System.out.println("name:" + key + " value:" + value);
            }
            System.out.println(checkExtra.getBundle("check_extras"));
        }
        SharedPreferences sp_counter = getSharedPreferences("counter",MODE_PRIVATE);
        int id = sp_counter.getInt("notification_id", 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_MUTABLE);
        System.out.println("pendingIntent:" + pendingIntent);
        builder1.setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle("已完成等待，点击此通知继续检查！")
                .setContentText(checkExtra.getString("commentText","null"))
                .setAutoCancel(true);
        String comment = checkExtra.getString("comment");
        if (comment != null) {
            builder1.setContentText(comment);
        }
        //NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        //manager.notify(ID_WAIT_OVER,builder1.build());
        manager.notify(id, builder1.build());
        id++;
        sp_counter.edit().putInt("notification_id",id).apply();
        //manager.notify(ID_WAIT_OVER,builder1.build());
        stopSelf();
    }
}