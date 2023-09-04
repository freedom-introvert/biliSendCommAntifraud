package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    Context context;
    Handler handler;
    SharedPreferences sp_config;
    CommentManipulator commentManipulator;
    CommentUtil commentUtil;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    DanmakuManipulator danmakuManipulator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        if (todo == -1) {
            new AlertDialog.Builder(context)
                    .setTitle("发生错误")
                    .setMessage("intent参数错误，intentExtras:" + intent.getExtras())
                    .setNegativeButton("关闭", (dialog, which) -> finish())
                    .show();
        } else if (todo == TODO_CHECK_COMMENT){
            String message = intent.getStringExtra("message");
            long oid = Long.parseLong(intent.getStringExtra("oid"));
            String type = intent.getStringExtra("type");
            String resultRpid = intent.getStringExtra("rpid");
            String root = intent.getStringExtra("root");
            String parent = intent.getStringExtra("parent");
            String comment = intent.getStringExtra("comment");
            String id = intent.getStringExtra("id");
            boolean hasPictures = intent.getBooleanExtra("hasPictures", false);

            CommentPresenter commentPresenter = new CommentPresenter(handler, commentManipulator, statisticsDBOpenHelper,
                    sp_config.getLong("wait_time", 5000),
                    sp_config.getLong("wait_time_by_has_pictures", 10000),
                    sp_config.getBoolean("autoRecorde", true),
                    sp_config.getBoolean("recordeHistory",true));
            DialogCommCheckWorker worker = new DialogCommCheckWorker(context, handler, commentManipulator, commentPresenter, commentUtil, this::finish);
            ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "检查中", "从哔哩哔哩APP来，正在获取评论区信息……");
            progressDialog.setCancelable(false);
            progressDialog.show();
            if (Integer.parseInt(type) == CommentArea.AREA_TYPE_VIDEO) {
                commentManipulator.getVideoInfoByAid(oid).enqueue(new BiliApiCallback<GeneralResponse<VideoInfo>>() {
                    @Override
                    public void onError(Throwable th) {
                        progressDialog.dismiss();
                        Toast.makeText(context, "网络错误" + th.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(GeneralResponse<VideoInfo> response) {
                        worker.checkComment(new CommentArea(oid, response.data.bvid, Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, hasPictures, progressDialog);
                    }
                });
            } else {
                if (Integer.parseInt(type) == CommentArea.AREA_TYPE_DYNAMIC17) {//动态17的动态ID就是评论区oid
                    worker.checkComment(new CommentArea(oid, String.valueOf(oid), Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, hasPictures, progressDialog);
                } else {//动态11的动态ID在ComposeActivity的Extras里获取
                    worker.checkComment(new CommentArea(oid, id != null ? id : "null", Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, hasPictures, progressDialog);
                }
            }
        } else if (todo == TODO_CHECK_DANMAKU){
            DialogDanmakuCheckWorker worker = new DialogDanmakuCheckWorker(context, handler, new DanmakuPresenter(handler, danmakuManipulator, statisticsDBOpenHelper, sp_config.getLong("wait_time_by_danmaku_sent", 20000), sp_config.getBoolean("autoRecorde", true)), new OnExitListener() {
                @Override
                public void exit() {
                    finish();
                }
            });
            long oid = intent.getLongExtra("oid",0);
            long dmid = intent.getLongExtra("dmid",0);
            String content = intent.getStringExtra("content");
            String accessKey = intent.getStringExtra("accessKey");
            long avid = intent.getLongExtra("avid",0);
            worker.startCheckDanmaku(oid,dmid,content,accessKey,avid);
            //DialogUtil.dialogMessage(context,"extras",intent.getExtras().toString());
            //worker.startCheckDanmaku(oid,dmid,content,accessKey,avid);
            //String accessKey =
            //worker.startCheckDanmaku();
        }
        //DialogUtil.dialogMessage(context, null, "oid=" + oid + "\ntype=" + type + "\nmessage=" + message + "\nrpid=" + resultRpid + "\nroot=" + root + "\nparent=" + parent + "\ncomment=" + comment);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(getConfigurationContext(newBase));
    }

    private static Context getConfigurationContext(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration.fontScale > 0.86f) {
            configuration.fontScale = 0.86f;
        }
        return context.createConfigurationContext(configuration);
    }
}