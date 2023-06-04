package icu.freedomIntrovert.biliSendCommAntifraud;

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
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import okhttp3.OkHttpClient;

public class ByXposedLaunchedActivity extends AppCompatActivity {
    Context context;
    Handler handler;
    SharedPreferences sp_config;
    CommentManipulator commentManipulator;
    CommentUtil commentUtil;
    StatisticsDBOpenHelper statisticsDBOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_by_xposed_launched);
        this.context = this;
        handler = new Handler();
        sp_config = getSharedPreferences("config", Context.MODE_PRIVATE);
        commentManipulator = new CommentManipulator(new OkHttpClient(), sp_config.getString("cookie", ""));
        commentUtil = new CommentUtil(sp_config);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        Intent intent = getIntent();
        String message = intent.getStringExtra("message");
        long oid = Long.parseLong(intent.getStringExtra("oid"));
        String type = intent.getStringExtra("type");
        String resultRpid = intent.getStringExtra("rpid");
        String root = intent.getStringExtra("root");
        String parent = intent.getStringExtra("parent");
        String comment = intent.getStringExtra("comment");
        String id = intent.getStringExtra("id");

        CommentPresenter commentPresenter = new CommentPresenter(handler, commentManipulator, statisticsDBOpenHelper, sp_config.getLong("wait_time", 5000), sp_config.getBoolean("autoRecorde", true));
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
                    worker.checkComment(new CommentArea(oid, response.data.bvid, Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, progressDialog);
                }
            });
        } else {
            if (Integer.parseInt(type) == CommentArea.AREA_TYPE_DYNAMIC17) {//动态17的动态ID就是评论区oid
                worker.checkComment(new CommentArea(oid,String.valueOf(oid), Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, progressDialog);
            } else {//动态11的动态ID在ComposeActivity的Extras里获取
                worker.checkComment(new CommentArea(oid, id != null ? id : "null", Integer.parseInt(type)), Long.parseLong(resultRpid), Long.parseLong(parent), Long.parseLong(root), comment, progressDialog);
            }
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