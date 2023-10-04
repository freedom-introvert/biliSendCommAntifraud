package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.Set;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressTimer;
import okhttp3.OkHttpClient;

public class ByXposedLaunchedActivity extends AppCompatActivity {
    public static final int TODO_CHECK_COMMENT = 0;
    public static final int TODO_CHECK_DANMAKU = 1;
    public static final int TODO_CONTINUE_CHECK_COMMENT = 2;
    public static final int TODO_SAVE_CONTAIN_SENSITIVE_CONTENT = 3;

    Context context;
    Handler handler;
    SharedPreferences sp_config;
    CommentManipulator commentManipulator;
    CommentUtil commentUtil;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    DanmakuManipulator danmakuManipulator;
    boolean toContinueTo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MainActivity.activity != null) {
            System.out.println("已finish MainActivity，避免离开哔哩哔哩");
            MainActivity.activity.finish();
        }
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_by_xposed_launched);
        this.context = this;
        handler = new Handler();
        sp_config = getSharedPreferences("config", Context.MODE_PRIVATE);
        OkHttpClient httpClient = new OkHttpClient();
        commentManipulator = new CommentManipulator(httpClient, sp_config.getString("cookie", ""));
        commentUtil = new CommentUtil(sp_config);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        danmakuManipulator = new DanmakuManipulator(httpClient);
        Intent intent = getIntent();
        int todo = intent.getIntExtra("todo", -1);

        long waitTime = sp_config.getLong("wait_time", 5000);
        long waitTimeByHasPictures = sp_config.getLong("wait_time_by_has_pictures", 10000);

        if (todo == -1) {
            new AlertDialog.Builder(context)
                    .setTitle("发生错误")
                    .setMessage("intent参数错误，intentExtras:" + intent.getExtras())
                    .setNegativeButton("关闭", (dialog, which) -> finish())
                    .show();
        } else if (todo == TODO_CHECK_COMMENT || todo == TODO_CHECK_DANMAKU) {

            boolean hasPictures = intent.getBooleanExtra("hasPictures", false);

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
                    intent1.putExtra("check_extras", intent.getExtras());
                    startService(intent1);
                    toContinueTo = false;
                    finish();
                } else {
                    Toast.makeText(context, "请授予通知权限！", Toast.LENGTH_LONG).show();
                    requestNotificationPermission(context);
                }
            });

            new Thread(() -> {
                if (!hasPictures) {
                    progressTimer.start();
                    if (toContinueTo) {
                        runOnUiThread(() -> {
                            progressBarDialog.setIndeterminate(true);
                            progressBarDialog.setTitle("检查中");
                            progressBarDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                            toCheck(intent.getExtras(), TODO_CHECK_COMMENT, progressBarDialog);
                        });
                    }
                } else {
                    progressTimer.start();
                    if (toContinueTo) {
                        runOnUiThread(() -> {
                            progressBarDialog.setIndeterminate(true);
                            progressBarDialog.setTitle("检查中");
                            progressBarDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                            toCheck(intent.getExtras(), TODO_CHECK_COMMENT, progressBarDialog);
                        });
                    }
                }
            }).start();
        } else if (todo == TODO_CONTINUE_CHECK_COMMENT) {
            ProgressBarDialog progressBarDialog = new ProgressBarDialog.Builder(context)
                    .setTitle("检查中")
                    .setMessage("恢复检查进度……")
                    .setIndeterminate(true)
                    .setCancelable(false)
                    .show();
            Bundle extras = intent.getBundleExtra("check_extras");
            if (extras != null) {
                Set<String> keySet = extras.keySet();
                for (String key : keySet) {
                    Object value = extras.get(key);
                    System.out.println("name:" + key + " value:" + value);
                }
                System.out.println(extras.getBundle("check_extras"));
            }
            toCheck(extras, TODO_CHECK_COMMENT, progressBarDialog);
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
            String id = intent.getStringExtra("id");
            DialogInterface.OnClickListener onClose = (dialog, which) -> finish();
            if (comment != null && s_oid != null && s_type != null) {
                long oid = Long.parseLong(s_oid);
                int type = Integer.parseInt(s_type);
                if (type == CommentArea.AREA_TYPE_VIDEO) {
                    commentManipulator.getVideoInfoByAid(oid).enqueue(new BiliApiCallback<GeneralResponse<VideoInfo>>() {
                        @Override
                        public void onError(Throwable th) {
                            progressBarDialog.dismiss();
                            DialogUtil.dialogMessage(context, "网络错误", th.getMessage(),onClose);
                        }

                        @Override
                        public void onSuccess(GeneralResponse<VideoInfo> videoInfoGeneralResponse) {
                            progressBarDialog.dismiss();
                            if (videoInfoGeneralResponse.isSuccess()) {
                                addSensitiveComment(new BannedCommentBean(new CommentArea(oid, videoInfoGeneralResponse.data.bvid, type), -System.currentTimeMillis(), comment, BannedCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BannedCommentBean.CHECKED_NO_CHECK),message,onClose);
                            } else {
                                DialogUtil.dialogMessage(context, "错误", videoInfoGeneralResponse.message,onClose);
                            }
                        }
                    });
                } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
                    addSensitiveComment(new BannedCommentBean(new CommentArea(oid, "cv" + oid, type), -System.currentTimeMillis(), comment, BannedCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BannedCommentBean.CHECKED_NO_CHECK),message,onClose);
                } else if (type == CommentArea.AREA_TYPE_DYNAMIC17) {
                    addSensitiveComment(new BannedCommentBean(new CommentArea(oid, String.valueOf(oid), type), -System.currentTimeMillis(), comment, BannedCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BannedCommentBean.CHECKED_NO_CHECK),message,onClose);
                } else {
                    addSensitiveComment(new BannedCommentBean(new CommentArea(oid,  id != null ? id : "null", type), -System.currentTimeMillis(), comment, BannedCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BannedCommentBean.CHECKED_NO_CHECK),message,onClose);
                }
            } else {
                DialogUtil.dialogMessage(context,"错误","无效的extras！\nextras:"+intent.getExtras(),onClose);
            }
        }
    }

    private void addSensitiveComment(BannedCommentBean bannedCommentBean,String message, DialogInterface.OnClickListener onClose) {
        if (statisticsDBOpenHelper.insertBannedComment(bannedCommentBean) > 0) {
            DialogUtil.dialogMessage(context, "包含敏感内容统计", "你的评论发送时提示：“"+message+"”，被ban统计数据库已添加包含敏感词的评论：" + bannedCommentBean.comment,onClose);
        } else {
            DialogUtil.dialogMessage(context, "包含敏感内容统计", "添加统计失败，可能条目有重复",onClose);
        }
    }

    private void toCheck(@Nullable Bundle extras, int todo, @NonNull ProgressBarDialog progressDialog) {
        if (extras == null) {
            progressDialog.dismiss();
            DialogUtil.dialogMessage(context, "未传入参数异常", "参数：" + extras);
            return;
        }
        String message = extras.getString("message");
        String s_oid = extras.getString("oid");
        String s_type = extras.getString("type");
        String s_resultRpid = extras.getString("rpid");
        String s_root = extras.getString("root");
        String s_parent = extras.getString("parent");
        String comment = extras.getString("comment");
        String id = extras.getString("id");
        boolean hasPictures = extras.getBoolean("hasPictures", false);
        if (message != null && s_oid != null && s_type != null && s_resultRpid != null && s_root != null && s_parent != null && comment != null) {
            long oid = Long.parseLong(s_oid);
            int type = Integer.parseInt(s_type);
            long resultRpid = Long.parseLong(s_resultRpid);
            long root = Long.parseLong(s_root);
            long parent = Long.parseLong(s_parent);

            if (todo == TODO_CHECK_COMMENT) {
                CommentPresenter commentPresenter = new CommentPresenter(handler, commentManipulator, statisticsDBOpenHelper,
                        sp_config.getLong("wait_time", 5000),
                        sp_config.getLong("wait_time_by_has_pictures", 10000),
                        sp_config.getBoolean("autoRecorde", true),
                        sp_config.getBoolean("recordeHistory", true));
                DialogCommCheckWorker worker = new DialogCommCheckWorker(context, handler, commentManipulator, commentPresenter, commentUtil, this::finish);

                if (type == CommentArea.AREA_TYPE_VIDEO) {
                    commentManipulator.getVideoInfoByAid(oid).enqueue(new BiliApiCallback<GeneralResponse<VideoInfo>>() {
                        @Override
                        public void onError(Throwable th) {
                            progressDialog.dismiss();
                            Toast.makeText(context, "网络错误" + th.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onSuccess(GeneralResponse<VideoInfo> response) {
                            worker.checkComment(new CommentArea(oid, response.data.bvid, type), resultRpid, parent, root, comment, hasPictures, progressDialog);
                        }
                    });
                } else {
                    if (type == CommentArea.AREA_TYPE_DYNAMIC17) {//动态17的动态ID就是评论区oid
                        worker.checkComment(new CommentArea(oid, String.valueOf(oid), type), resultRpid, parent, root, comment, hasPictures, progressDialog);
                    } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
                        worker.checkComment(new CommentArea(oid, "cv" + oid, type), resultRpid, parent, root, comment, hasPictures, progressDialog);
                    } else {//动态11的动态ID在ComposeActivity的Extras里获取
                        worker.checkComment(new CommentArea(oid, id != null ? id : "null", type), resultRpid, parent, root, comment, hasPictures, progressDialog);
                    }
                }
            } else if (todo == TODO_CHECK_DANMAKU) {
                DialogDanmakuCheckWorker worker = new DialogDanmakuCheckWorker(context, handler, new DanmakuPresenter(handler, danmakuManipulator, statisticsDBOpenHelper, sp_config.getLong("wait_time_by_danmaku_sent", 20000), sp_config.getBoolean("autoRecorde", true)), () -> finish());
                long dmid = extras.getLong("dmid", 0);
                String content = extras.getString("content");
                String accessKey = extras.getString("accessKey");
                long avid = extras.getLong("avid", 0);
                worker.startCheckDanmaku(oid, dmid, content, accessKey, avid);
            }
            //DialogUtil.dialogMessage(context, null, "oid=" + oid + "\ntype=" + type + "\nmessage=" + message + "\nrpid=" + resultRpid + "\nroot=" + root + "\nparent=" + parent + "\ncomment=" + comment);
        } else {
            DialogUtil.dialogMessage(context, "缺少参数异常", "参数：" + extras);
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