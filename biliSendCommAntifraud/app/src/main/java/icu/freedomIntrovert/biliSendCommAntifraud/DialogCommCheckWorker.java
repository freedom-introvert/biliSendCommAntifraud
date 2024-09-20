package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.DeleteCommentTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.CommentCheckTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.AppealCommentDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.CheckCommentAreaDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.ScanSensitiveWordDialog;

public class DialogCommCheckWorker {
    public static final int BANNED_TYPE_SHADOW_BAN = 1;
    public static final int BANNED_TYPE_QUICK_DELETE = 2;
    public static final int BANNED_TYPE_UNDER_REVIEW = 3;
    public static final int BANNED_TYPE_INVISIBLE = 4;

    private final Context context;
    private final StatisticsDBOpenHelper statDB;

    public DialogCommCheckWorker(Context context) {
        this.context = context;
        this.statDB = StatisticsDBOpenHelper.getInstance(context);
    }

    public void checkComment(Comment comment, boolean needWait, ArrayList<String> clientCookies, CheckCommentCallBack callback) {

        ProgressBarDialog dialog = new ProgressBarDialog.Builder(context)
                .setIndeterminate(true)
                .setTitle("检查评论")
                .setMessage("等待检查评论……")
                .setCancelable(false)
                .setPositiveButton("后台等待", null)
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

        CommentCheckTask task = new CommentCheckTask(context, comment, needWait, clientCookies, new CommentCheckTask.EventHandler() {
            String waitMessage = "等待一段时间后检查评论，剩余 %s ms";

            @Override
            public void onGettingClientAccount() {
                dialog.setMessage("正在获取客户端cookie对应的账号……");
            }

            @Override
            public void onClientCookieInvalid(String cookie) {
                dialog.dismiss();
                dialogMessage("客户端cookie无效", "若客户端cookie始终不能用，请自行设置账号与cookie。设置完可回到待检查评论列表来检查", callback);
            }

            @Override
            public void onLocalAccountCookieFailed(Account account) {
                dialog.dismiss();
                dialogMessage("本地账号cookie失效", String.format("账号：%s(%s) 的cookie已失效，请从新获取并刷新", account.uname, account.uid), callback);
            }

            @Override
            public void onClientCookieUidNoMatch(long commentUid, long cookieUid) {
                dialog.dismiss();
                dialogMessage("错误", String.format("客户端cookie对应的账号与评论发布者账号不一致！\n客户端cookie UID：%s\n评论发布者UID：%s", cookieUid, commentUid), callback);
            }

            @Override
            public void onNoAccountAndClientCookie(long uid) {
                dialog.dismiss();
                dialogMessage("未找到账号", String.format("评论发布者UID：%s 没有对应的账号，请添加与b站客户端一致的账号或者启用『自动获取b站客户端cookie』功能", uid), callback);
            }

            @Override
            public void onStartWait(long totalMs) {
                waitMessage = "等待" + totalMs + "ms后检查评论\n当前剩余：%s ms";
                dialog.setIndeterminate(false);
                dialog.setMax((int) totalMs);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void onStartWaitHasPicture(long totalCommWaitMs, long totalPicWaitMs, long totalMs) {
                waitMessage = "评论包含图片，等待" + totalCommWaitMs + "+" + totalPicWaitMs + "=" + totalMs + "ms后检查评论\n当前剩余：%s ms";
                dialog.setIndeterminate(false);
                dialog.setMax((int) totalMs);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void onWaitProgress(long remainingMs, long currentMs) {
                dialog.setMessage(String.format(waitMessage, remainingMs));
                dialog.setProgress((int) currentMs);
            }

            @Override
            public void onSavingPictures(int total, int index, Comment.PictureInfo pictureInfo) {
                dialog.setMessage("正在保存图片，进度：" + index + "/" + total);
                dialog.setMax(total);
                dialog.setProgress(index);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }

            @Override
            public void onStartCheck() {
                //禁用后台等待按钮
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                dialog.setIndeterminate(true);
                dialog.setMessage("正在检查评论...");
            }

            @Override
            public void onCommentNotFound() {
                dialog.setMessage("无账号下未找到该评论，判断状态中...");
            }

            @Override
            public void onResult(HistoryComment historyComment) {
                dialog.dismiss();
                callback.onResult(historyComment);
                switch (historyComment.lastState) {
                    case HistoryComment.STATE_NORMAL:
                        showCommentIsOkResult(comment.comment, callback);
                        break;
                    case HistoryComment.STATE_INVISIBLE:
                        showCommentBannedResult(BANNED_TYPE_INVISIBLE, historyComment, callback);
                        break;
                    case HistoryComment.STATE_UNDER_REVIEW:
                        showCommentBannedResult(BANNED_TYPE_UNDER_REVIEW, historyComment, callback);
                        break;
                    case HistoryComment.STATE_SHADOW_BAN:
                        showCommentBannedResult(BANNED_TYPE_SHADOW_BAN, historyComment, callback);
                        break;
                    case HistoryComment.STATE_DELETED:
                        showCommentBannedResult(BANNED_TYPE_QUICK_DELETE, historyComment, callback);
                        break;
                    default:
                        throw new RuntimeException("意外的检查结果：" + historyComment.lastState);
                }
            }

            @Override
            public void onError(Throwable th) {
                dialog.dismiss();
                DialogUtil.dialogError(context, th, callback);
            }
        });

        task.execute();

        //后台等待
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (NotificationService.checkOrRequestNotificationPermission(context)) {
                Intent intent = new Intent(context, WaitService.class);
                System.out.println("剩余时间"+task.getRemainingWaitTime());
                intent.putExtra("wait_seconds", (int) (task.getRemainingWaitTime() / 1000));
                intent.putExtra("rpid", comment.rpid);
                intent.putExtra("comment", comment.comment);
                intent.putExtra("cookies", clientCookies);
                context.startService(intent);
                task.cancelCheck();
                dialog.dismiss();
                callback.onDismiss(dialog);
            }
        });
    }

    private void showCommentIsOkResult(String comment, DialogInterface.OnDismissListener listener) {
        new AlertDialog.Builder(context)
                .setTitle("检查结果")
                .setMessage("你的评论：“" + comment + "”正常显示！")
                .setCancelable(false)
                .setOnDismissListener(listener)
                .setPositiveButton("关闭", null)
                .show();
    }


    private void showCommentBannedResult(int bannedType, HistoryComment comment, DialogInterface.OnDismissListener listener) {
        CommentArea commentArea = comment.commentArea;
        long rpid = comment.rpid;
        String commentText = comment.comment;
        AlertDialog.Builder resultDialogBuilder = new AlertDialog.Builder(context).setTitle("检查结果");
        switch (bannedType) {
            case BANNED_TYPE_SHADOW_BAN:
                resultDialogBuilder.setIcon(R.drawable.hide_black);
                if (comment.root == 0) {
                    resultDialogBuilder.setMessage("您的评论“" + CommentUtil.omitComment(commentText, 100) +
                            "”在无账号环境下未找到，自己账号下获取该评论的回复列表成功，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严");
                } else {
                    resultDialogBuilder.setMessage("您的楼中楼评论“" + CommentUtil.omitComment(commentText, 100) +
                            "”在无账号环境下未找到，自己账号下成功找到，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严");
                }
                break;
            case BANNED_TYPE_UNDER_REVIEW:
                resultDialogBuilder.setIcon(R.drawable.i_black);
                resultDialogBuilder.setMessage("您的评论“" + CommentUtil.omitComment(commentText, 100) +
                        "”在无账号环境下无法找到，自己账号下获取该评论的回复列表成功，接着又能在无账号下获取回复，疑似审核中。建议你直接去申诉，因为它很可能回复“无可申诉评论”，请后续约十分钟来历史记录复查！\n" +
                        "您也可以在更多评论选项中选择『监控评论』对此评论进行监控，若评论状态变化将通知您");
                break;
            case BANNED_TYPE_QUICK_DELETE:
                resultDialogBuilder.setIcon(R.drawable.deleted_black);
                if (comment.root == 0) {
                    resultDialogBuilder.setMessage("您的评论“" + CommentUtil.omitComment(commentText, 100) +
                            "”在自己账号下获取该评论的回复列表和对该评论发送回复时均收到提示：“已经被删除了”，判定改评论被系统速删，请检查评论内容或者检查评论区是否被戒严");
                } else {
                    resultDialogBuilder.setMessage("您的楼中楼评论“" + CommentUtil.omitComment(commentText, 100) +
                            "”在无账号和自己账号下都没找到，判定该评论被系统速删，请检查评论内容或者检查评论区是否被戒严");
                }
                break;
            case BANNED_TYPE_INVISIBLE:
                resultDialogBuilder.setIcon(R.drawable.ghost_black);
                resultDialogBuilder.setMessage("您的评论“" + CommentUtil.omitComment(commentText, 100) +
                        "”在无账号环境下成功找到，但是被标记invisible，也就是隐身（在前端被隐藏）！这是非常罕见的情况……通常在评论发送很久时间后才会出现。" +
                        "可以的话把评论信息发给开发者，以分析触发条件（也许你被UP主拉黑了）");
                break;
        }
        resultDialogBuilder.setOnDismissListener(listener);
        resultDialogBuilder.setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener());
        resultDialogBuilder.setNeutralButton("检查评论区", null);
        resultDialogBuilder.setNegativeButton("更多评论选项", null);
        AlertDialog resultDialog = resultDialogBuilder.show();
        //检查评论区
        resultDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new CheckCommentAreaDialog(context).show(comment));

        //更多评论选项
        resultDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            String[] strings = {"扫描敏感词", "申诉", "删除发布的评论", "复制rpid、oid、type"};
            if (bannedType == BANNED_TYPE_UNDER_REVIEW) {
                strings = new String[]{"扫描敏感词", "申诉", "删除发布的评论", "复制rpid、oid、type", "监控评论"};
            }
            new AlertDialog.Builder(context).setTitle("更多选项").setItems(strings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: //扫描敏感词
                            ScanSensitiveWordDialog.show(context, comment);
                            break;
                        case 1: //申诉
                            AppealCommentDialog.show(context, comment, new AppealCommentDialog.ResultCallback(context) {
                                @Override
                                public void onNoCommentToAppeal(String successToast) {
                                    super.onNoCommentToAppeal(successToast);
                                    statDB.updateHistoryCommentLastState(rpid, HistoryComment.STATE_SUSPECTED_NO_PROBLEM);
                                }
                            });
                            break;
                        case 2:
                            new DeleteCommentTask(context, comment, new DeleteCommentTask.EventHandler() {
                                @Override
                                public void onAccountNotFound(long uid) {
                                    Toast.makeText(context, "删除失败，未找到用户UID：" + uid, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onSuccess() {
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onComplete() {
                                    dialog.dismiss();
                                }

                                @Override
                                public void onError(Throwable th) {
                                    DialogUtil.dialogError(context, th);
                                }
                            });
                            break;
                        case 3://复制rpid等评论信息
                            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData mClipData = ClipData.newPlainText("Label", "rpid:" + rpid + "\noid:" + commentArea.oid + "\ntype:" + commentArea.type);
                            cm.setPrimaryClip(mClipData);
                            toastShort("已复制");
                            break;
                        case 4:
                            CommentUtil.toMonitoringURComment(context, comment);
                    }
                }
            }).show();
        });
    }

    private void toastShort(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void toastLong(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public void dialogMessage(String title, String message, DialogInterface.OnDismissListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(listener)
                .show();
    }

    public interface CheckCommentCallBack extends DialogInterface.OnDismissListener {
        default void onResult(HistoryComment historyComment) {

        }
    }


}
