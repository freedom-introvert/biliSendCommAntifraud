package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.VideoInfo;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.danmaku.DanmakuPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import okhttp3.OkHttpClient;

public class ByXposedLaunchedActivity extends AppCompatActivity {
    public static final int TODO_CHECK_COMMENT = 0;
    public static final int TODO_CHECK_DANMAKU = 1;
    public static final int TODO_CONTINUE_CHECK_COMMENT = 2;

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

            String proMsg;
            if (hasPictures) {
                proMsg = "评论包含图片，等待" + waitTime + "+" + waitTimeByHasPictures + "=" + (waitTime + waitTimeByHasPictures) + "ms后检查评论……";
            } else {
                proMsg = "等待" + waitTime + "ms后检评论……";
            }

            ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "等待中", proMsg);
            long lastTime = System.currentTimeMillis();
            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "后台等待", (dialog, which) -> {
                toContinueTo = false;
                Intent intent1 = new Intent(context, WaitService.class);
                if (hasPictures) {
                    intent1.putExtra("wait_time", (waitTime + waitTimeByHasPictures) - (System.currentTimeMillis() - lastTime));
                } else {
                    intent1.putExtra("wait_time", waitTime - (System.currentTimeMillis() - lastTime));
                }
                intent1.putExtra("check_extras", intent.getExtras());
                startService(intent1);
                finish();
            });
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                if (!hasPictures) {
                    try {
                        Thread.sleep(waitTime);
                        if (toContinueTo) {
                            runOnUiThread(() -> {
                                progressDialog.setTitle("检查中");
                                progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                toCheck(intent.getExtras(), TODO_CHECK_COMMENT, progressDialog);
                            });
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        Thread.sleep(waitTime + waitTimeByHasPictures);
                        if (toContinueTo) {
                            runOnUiThread(() -> {
                                progressDialog.setTitle("检查中");
                                progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                toCheck(intent.getExtras(), TODO_CHECK_COMMENT, progressDialog);
                            });
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        } else if (todo == TODO_CONTINUE_CHECK_COMMENT){

            ProgressDialog progressDialog = DialogUtil.newProgressDialog(context,"检查中","恢复检查进度……");
            progressDialog.setCancelable(false);
            progressDialog.show();
            Bundle extras = intent.getBundleExtra("check_extras");
            if (extras != null){
                Set<String> keySet = extras.keySet();
                for(String key : keySet) {
                    Object value = extras.get(key);
                    System.out.println("name:"+key+" value:"+value);
                }
                System.out.println(extras.getBundle("check_extras"));
            }
            toCheck(extras,TODO_CHECK_COMMENT,progressDialog);
        }
    }

    private void toCheck(@Nullable Bundle extras, int todo, @NonNull ProgressDialog progressDialog) {
        if (extras == null){
            progressDialog.dismiss();
            DialogUtil.dialogMessage(context,"未传入参数异常","参数："+extras);
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
                DialogDanmakuCheckWorker worker = new DialogDanmakuCheckWorker(context, handler, new DanmakuPresenter(handler, danmakuManipulator, statisticsDBOpenHelper, sp_config.getLong("wait_time_by_danmaku_sent", 20000), sp_config.getBoolean("autoRecorde", true)), new OnExitListener() {
                    @Override
                    public void exit() {
                        finish();
                        Process.killProcess(Process.myPid());
                    }
                });
                long dmid = extras.getLong("dmid", 0);
                String content = extras.getString("content");
                String accessKey = extras.getString("accessKey");
                long avid = extras.getLong("avid", 0);
                worker.startCheckDanmaku(oid, dmid, content, accessKey, avid);
            }
            //DialogUtil.dialogMessage(context, null, "oid=" + oid + "\ntype=" + type + "\nmessage=" + message + "\nrpid=" + resultRpid + "\nroot=" + root + "\nparent=" + parent + "\ncomment=" + comment);
        } else {
            DialogUtil.dialogMessage(context,"缺少参数异常","参数："+extras);
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
}