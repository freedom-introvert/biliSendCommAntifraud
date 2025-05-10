package icu.freedomIntrovert.biliSendCommAntifraud.xposed.hooks;

import android.app.Activity;
import android.widget.Toast;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.ReviewCommentStatusTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.BaseHook;
import icu.freedomIntrovert.biliSendCommAntifraud.xposed.XB;


public class ShelfHook extends BaseHook {

    @Override
    public void startHook(int appVersionCode, ClassLoader classLoader) throws ClassNotFoundException {
        XB.log("已启动");
        XposedHelpers.findAndHookMethod(CommentCheckTask.class, "check", Account.class, CommentArea.class, CommentCheckTask.EventHandler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                System.out.println("我调用了");
                CommentCheckTask task = (CommentCheckTask) param.thisObject;
                Field fContext = CommentCheckTask.class.getField("context");
                Field fComment = CommentCheckTask.class.getField("comment");
                fContext.setAccessible(true);
                fComment.setAccessible(true);
                Activity activity = (Activity) fContext.get(task);
                Comment comment = (Comment) fComment.get(task);
                Account account = (Account) param.args[0];
                CommentManipulator commentManipulator = CommentManipulator.getInstance();
                assert comment != null;
                assert activity != null;
                Toast.makeText(activity, "我调用了", Toast.LENGTH_SHORT).show();
                XB.log("我调用了");

                BiliComment biliComment = commentManipulator.findCommentUsingSeekRpid(comment, account, true);
                activity.runOnUiThread(() -> {
                    if (biliComment == null) {
                        Toast.makeText(activity, "评论state: NOT FOUND", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "评论state: "+biliComment.state, Toast.LENGTH_SHORT).show();
                        XB.log("评论state: ");
                    }
                });
            }
        });



    }
}
