package icu.freedomIntrovert.biliSendCommAntifraud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentMonitoringTask;

public class CancelMonitorReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long rpid = intent.getLongExtra("rpid", 0);
        for (CommentMonitoringTask task : CommentMonitoringService.tasks) {
            if (task.comment.rpid == rpid){
                task.cancel();
            }
        }
    }
}
