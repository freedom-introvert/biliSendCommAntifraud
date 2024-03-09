package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.async.TaskManger;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressTimer;

public class ByXposedLaunchedActivity extends AppCompatActivity {
    public static final int TODO_CHECK_COMMENT = 0;
    public static final int TODO_CHECK_DANMAKU = 1;
    public static final int TODO_CONTINUE_CHECK_COMMENT = 2;
    public static final int TODO_SAVE_CONTAIN_SENSITIVE_CONTENT = 3;
    public static Activity lastActivity;
    Context context;
    Handler handler;
    CommentManipulator commentManipulator;
    CommentUtil commentUtil;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    DanmakuManipulator danmakuManipulator;
    boolean toContinueTo = true;
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (lastActivity != null) {
            System.out.println("已finish"+lastActivity.getClass().getCanonicalName()+"，避免离开哔哩哔哩");
            lastActivity.finish();
        }
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_by_xposed_launched);
        this.context = this;
        config = new Config(context);
        handler = new Handler();
        commentManipulator = new CommentManipulator(config.getCookie(), config.getDeputyCookie());
        commentUtil = new CommentUtil(context);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        danmakuManipulator = new DanmakuManipulator();
        Intent intent = getIntent();
        int todo = intent.getIntExtra("todo", -1);

        long waitTime = config.getWaitTime();
        long waitTimeByHasPictures = config.getWaitTimeByHasPictures();

        Bundle extras = intent.getExtras();
        if (extras == null) {
            showExtrasError(null);
        } else if (todo == TODO_CHECK_COMMENT) {
            String message = extras.getString("message");
            String s_oid = extras.getString("oid");
            String s_type = extras.getString("type");
            String s_resultRpid = extras.getString("rpid");
            String s_root = extras.getString("root");
            String s_parent = extras.getString("parent");
            String commentText = extras.getString("comment");
            String dynamicId = extras.getString("dynamic_id");
            String bvid = extras.getString("bvid");
            String pictures = extras.getString("pictures");
            long ctime = extras.getLong("ctime", 0) * 1000;
            if (message == null || s_oid == null || s_type == null || s_resultRpid == null || s_root == null || s_parent == null || commentText == null) {
                showExtrasError(extras);
                return;
            }
            long oid = Long.parseLong(s_oid);
            int type = Integer.parseInt(s_type);
            long resultRpid = Long.parseLong(s_resultRpid);
            long root = Long.parseLong(s_root);
            long parent = Long.parseLong(s_parent);
            CommentArea commentArea = null;
            if (type == CommentArea.AREA_TYPE_VIDEO) {
                commentArea = new CommentArea(oid,bvid,type);
            } else if (type == CommentArea.AREA_TYPE_DYNAMIC17) {
                //动态17的动态ID就是评论区oid
                commentArea = new CommentArea(oid, s_oid, type);
            } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
                commentArea = new CommentArea(oid, "cv" + oid, type);
            } else {
                //动态11的动态ID在ComposeActivity的Extras里获取
                commentArea = new CommentArea(oid, dynamicId != null ? dynamicId : "null", type);
            }
            Comment comment = new Comment(commentArea, resultRpid, parent, root, commentText, pictures, new Date(ctime));
            statisticsDBOpenHelper.insertPendingCheckComment(comment);

            boolean hasPictures = comment.hasPictures();
            long totalWaitTime;
            String proMsg;
            if (hasPictures) {
                totalWaitTime = waitTime + waitTimeByHasPictures;
                proMsg = "评论包含图片，等待" + waitTime + "+" + waitTimeByHasPictures + "=(%d/" + totalWaitTime + ")ms后检查评论……";
            } else {
                totalWaitTime = waitTime;
                proMsg = "等待(%d/" + waitTime + ")ms后检评论……";
            }

            ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                    .setTitle("等待中")
                    .setMessage(String.format(proMsg, 0))
                    .setPositiveButton("后台等待", null)
                    .setCancelable(false)
                    .show();

            ProgressTimer progressTimer = new ProgressTimer(totalWaitTime, ProgressBarDialog.DEFAULT_MAX_PROGRESS, new ProgressTimer.ProgressLister() {
                @Override
                public void onNewProgress(int progress, long sleepSeg) {
                    runOnUiThread(() -> {
                        progressBarDialog.setProgress(progress);
                        progressBarDialog.setMessage(String.format(proMsg, progress * sleepSeg));
                    });
                }
            });
            long lastTime = System.currentTimeMillis();
            progressBarDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (checkNotificationPermission(context)) {
                    Intent intent1 = new Intent(context, WaitService.class);
                    if (hasPictures) {
                        intent1.putExtra("wait_time", totalWaitTime - (System.currentTimeMillis() - lastTime));
                    } else {
                        intent1.putExtra("wait_time", waitTime - (System.currentTimeMillis() - lastTime));
                    }
                    intent1.putExtra("rpid", resultRpid);
                    intent1.putExtra("comment",commentText);
                    startService(intent1);
                    toContinueTo = false;
                    finish();
                } else {
                    Toast.makeText(context, "请授予通知权限！", Toast.LENGTH_LONG).show();
                    requestNotificationPermission(context);
                }
            });

            new Thread(() -> {
                progressTimer.start();
                if (toContinueTo) {
                    runOnUiThread(() -> {
                        progressBarDialog.setIndeterminate(true);
                        progressBarDialog.setTitle("检查中");
                        progressBarDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                        toCheckComment(comment,progressBarDialog);
                    });
                }
            }).start();
        } else if (todo == TODO_CONTINUE_CHECK_COMMENT) {
            ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                    .setTitle("检查中")
                    .setMessage("恢复检查进度……")
                    .setIndeterminate(true)
                    .setCancelable(false)
                    .show();
            long rpid = intent.getLongExtra("rpid",-1);
            Comment comment = statisticsDBOpenHelper.getPendingCheckCommentByRpid(rpid);
            System.out.println(comment);
            if (comment == null){
                dialogMessageAndExit("错误","未找到rpid="+rpid+"的待检查评论，可能你删除了该记录");
            } else {
                toCheckComment(comment,progressBarDialog);
            }


        } else if (todo == TODO_SAVE_CONTAIN_SENSITIVE_CONTENT) {
            ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                    .setTitle("统计敏感内容警告")
                    .setMessage("获取评论区bvid……")
                    .setIndeterminate(true)
                    .setCancelable(false)
                    .show();

            String comment = intent.getStringExtra("comment");
            String message = intent.getStringExtra("message");
            String s_oid = intent.getStringExtra("oid");
            String s_type = intent.getStringExtra("type");
            String dynamicId = intent.getStringExtra("dynamic_id");
            DialogInterface.OnClickListener onClose = (dialog, which) -> finish();
            if (comment != null && s_oid != null && s_type != null) {
                long oid = Long.parseLong(s_oid);
                int type = Integer.parseInt(s_type);
                if (type == CommentArea.AREA_TYPE_VIDEO) {
                    commentManipulator.getVideoInfoByAid(oid).enqueue(new BiliApiCallback<GeneralResponse<VideoInfo>>() {
                        @Override
                        public void onError(Throwable th) {
                            progressBarDialog.dismiss();
                            DialogUtil.dialogMessage(context, "网络错误", th.getMessage(), onClose);
                        }

                        @Override
                        public void onSuccess(GeneralResponse<VideoInfo> videoInfoGeneralResponse) {
                            progressBarDialog.dismiss();
                            if (videoInfoGeneralResponse.isSuccess()) {
                                addSensitiveComment(new CommentArea(oid, videoInfoGeneralResponse.data.bvid, type), comment, message, onClose);
                            } else {
                                DialogUtil.dialogMessage(context, "错误", videoInfoGeneralResponse.message, onClose);
                            }
                        }
                    });
                } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
                    addSensitiveComment(new CommentArea(oid, "cv" + oid, type), comment, message, onClose);
                } else if (type == CommentArea.AREA_TYPE_DYNAMIC17) {
                    addSensitiveComment(new CommentArea(oid, String.valueOf(oid), type), comment, message, onClose);
                } else {
                    addSensitiveComment(new CommentArea(oid, dynamicId != null ? dynamicId : "null", type), comment, message, onClose);
                }
            } else {
                DialogUtil.dialogMessage(context, "错误", "无效的extras！\nextras:" + intent.getExtras(), onClose);
            }
        }
    }

    private void addSensitiveComment(CommentArea commentArea, String commentText, String message, DialogInterface.OnClickListener onClose) {
        HistoryComment historyComment = new HistoryComment(new Comment(commentArea, -System.currentTimeMillis(), 0, 0, commentText, null, new Date()));
        historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SENSITIVE);
        if (statisticsDBOpenHelper.insertHistoryComment(historyComment) > 0) {
            DialogUtil.dialogMessage(context, "包含敏感内容统计", "你的评论发送时提示：“" + message + "”，被ban统计数据库已添加包含敏感词的评论：" + commentText, onClose);
        } else {
            DialogUtil.dialogMessage(context, "包敏感内容统计", "添加统计失败，可能条目有重复", onClose);
        }
    }
    private void showExtrasError(@Nullable Bundle extras){
        dialogMessageAndExit("发生错误","intent参数错误，intentExtras:" + extras);
    }

    private void dialogMessageAndExit(String title,String message){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("关闭", (dialog, which) -> finish())
                .show();
    }
    private void toCheckComment(Comment comment,ProgressBarDialog progressDialog){
        DialogCommCheckWorker worker = new DialogCommCheckWorker(context, config, statisticsDBOpenHelper, commentManipulator, commentUtil);
        worker.setExitListener(new OnExitListener() {
            @Override
            public void exit() {
                finish();
            }
        });
        List<Comment.PictureInfo> pictureInfoList = comment.getPictureInfoList();
        if (pictureInfoList != null){
            TaskManger.start(() -> {
                try {
                    for (int i = 0; i < pictureInfoList.size(); i++) {
                        int finalI = i;
                        runOnUiThread(() -> {
                            progressDialog.setMessage("正在存档图片["+ (finalI+1) +"/"+pictureInfoList.size()+"]");
                        });
                        PictureStorage.save(context, pictureInfoList.get(i).img_src);
                    }
                    runOnUiThread(() -> {
                        progressDialog.setMessage("检查中……");
                        worker.checkComment(comment,progressDialog);
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        DialogUtil.dialogMessage(context,"错误","保存图片失败！因为"+e.getMessage());
                    });
                    e.printStackTrace();
                }
            });
        } else {
            worker.checkComment(comment, progressDialog);
        }
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(getConfigurationContext(newBase));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("onNewIntent");
        System.out.println(this);
    }

    private static Context getConfigurationContext(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration.fontScale > 0.86f) {
            configuration.fontScale = 0.86f;
        }
        return context.createConfigurationContext(configuration);
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