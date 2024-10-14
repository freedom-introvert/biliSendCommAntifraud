package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Date;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks.Utils;

public class ByXposedLaunchedActivity extends AppCompatActivity {
    public static final int ACTION_CHECK_COMMENT = 0;
    public static final int ACTION_CHECK_DANMAKU = 1;
    public static final int ACTION_RESUME_CHECK_COMMENT = 2;
    public static final int ACTION_SAVE_CONTAIN_SENSITIVE_CONTENT = 3;

    private Context context;
    private StatisticsDBOpenHelper statisticsDBOpenHelper;
    private Config config;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_by_xposed_launched);
        context = this;
        config = Config.getInstance(context);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (!checkIntentExtras(extras)) {
            showExtrasErrorDialog(extras);
            return;
        }

        statisticsDBOpenHelper = StatisticsDBOpenHelper.getInstance(context);

        int action = extras.getInt("action");
        switch (action) {
            case ACTION_CHECK_COMMENT:
                checkComment(extras.getLong("oid"), extras.getInt("type"),
                        extras.getLong("rpid"), extras.getLong("root"),
                        extras.getLong("parent"), extras.getString("source_id"),
                        extras.getString("comment_text"), extras.getString("pictures"),
                        extras.getLong("ctime"), extras.getLong("uid"),
                        extras.getStringArrayList("cookies"));
                break;
            case ACTION_RESUME_CHECK_COMMENT:
                resumeCheckComment(extras.getLong("rpid"), extras.getStringArrayList("cookies"));
        }
    }

    private void checkComment(long oid, int type, long rpid, long root, long parent, String sourceId,
                              String commentText, String pictures, long ctime, long uid, ArrayList<String> clientCookies) {
        if (!config.getUseClientCookie()){
            clientCookies = null;
        }
        CommentArea commentArea = new CommentArea(oid, sourceId != null ? sourceId : "null", type);
        Comment comment = new Comment(commentArea, rpid, parent, root, commentText, pictures, new Date(ctime * 1000), uid);
        //插入待检查评论，要是检查出问题可以回来检查
        statisticsDBOpenHelper.insertPendingCheckComment(comment);
        new DialogCommCheckWorker(context).checkComment(comment, true, clientCookies, dialog -> finish());
    }

    private void resumeCheckComment(long rpid, ArrayList<String> clientCookies) {
        Comment comment = statisticsDBOpenHelper.getPendingCheckCommentByRpid(rpid);
        if (comment == null) {
            dialogMessageAndExit("待检查评论不存在", String.format("rpid为 %s 的待检查评论不存在，" +
                    "你应该删除了该待检查评论或已在待检查评论列表检查过了", rpid));
            return;
        }
        new DialogCommCheckWorker(context).checkComment(comment, false, clientCookies, dialog -> finish());
    }

    private boolean checkIntentExtras(Bundle extras) {
        if (extras == null) {
            return false;
        }
        int action = extras.getInt("action", -1);
        switch (action) {
            case ACTION_CHECK_COMMENT:
                return Utils.checkExtras(extras, "oid", "type", "rpid", "root",
                        "parent", "source_id", "comment_text", "ctime", "uid");
            case ACTION_RESUME_CHECK_COMMENT:
                return Utils.checkExtras(extras, "rpid");
            default:
                return false;
        }
    }

    private void showExtrasErrorDialog(Bundle extras) {
        dialogMessageAndExit("启动参数错误，请参阅文档", "你传入的Intent Extras：" + extras);
    }

    private void dialogMessageAndExit(String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("关闭", (dialog, which) -> finish())
                .show();
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
