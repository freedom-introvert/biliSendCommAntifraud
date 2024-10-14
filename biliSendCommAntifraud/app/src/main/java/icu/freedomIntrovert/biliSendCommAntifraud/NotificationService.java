package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class NotificationService {

    //后台任务（比如等待检查、评论监控）
    public static final String CHANNEL_BACKGROUND_TASK = "BACKGROUND_TASK";
    //后台任务结果（等待完毕、评论监控到状态改变）
    public static final String CHANNEL_BACKGROUND_TASK_RESULT = "BACKGROUND_TASK_RESULT";
    private static NotificationService instance;
    public final NotificationManager notificationManager;
    private int idIndex = 1;//自增ID
    private NotificationService(Context context){
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    public synchronized static NotificationService getInstance(Context context) {
        if (instance == null){
            instance = new NotificationService(context);
        }
        return instance;
    }

    private void createNotificationChannels(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_BACKGROUND_TASK,
                    "后台任务",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("检查等待、评论监控的进度等");
            notificationManager.createNotificationChannel(channel);
            channel = new NotificationChannel(
                    CHANNEL_BACKGROUND_TASK_RESULT,
                    "后台任务结果",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("等待完毕、评论监控到状态改变等");
            channel.enableVibration(true);

            notificationManager.createNotificationChannel(channel);
        }
    }

    public int createNewId(){
        return idIndex++;
    }

    public static boolean checkOrRequestNotificationPermission(Context context){
        if (checkNotificationPermission(context)){
            return true;
        } else {
            requestNotificationPermission(context);
            Toast.makeText(context, "请授予通知权限。为避免不显示，建议设置弹出权限和一些重要性", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                return notificationManager.areNotificationsEnabled();
            }
        } else {
            // 对于Android 6.0及以下版本，无法直接检查通知权限，需要用户手动设置
            return true;
        }
        return false;
    }

    public static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "无法自动请求通知权限，请手动设置", Toast.LENGTH_SHORT).show();
        }
    }


}
