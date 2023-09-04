package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.AppealDialogPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;

public class DialogCommCheckWorker {
    private Context context;
    private Handler handler;
    private CommentManipulator commentManipulator;
    private CommentPresenter commentPresenter;
    private CommentUtil commentUtil;
    private OnExitListener exitListener;

    public DialogCommCheckWorker(Context context, Handler handler, CommentManipulator commentManipulator, CommentPresenter commentPresenter, CommentUtil commentUtil, OnExitListener exitListener) {
        this.context = context;
        this.handler = handler;
        this.commentManipulator = commentManipulator;
        this.commentPresenter = commentPresenter;
        this.commentUtil = commentUtil;
        this.exitListener = exitListener;
    }

    public void checkComment(CommentArea commentArea, long rpid, long parent, long root, String comment,boolean hasPictures, ProgressDialog dialog) {
        if (commentManipulator.cookieAreSet()) {
            /*
            commentPresenter.checkCommentStatusByNewMethod(commentArea, comment, rpid, new CommentPresenter.CheckCommentStatusByNewMethodCallBack() {
                @Override
                public void onSleeping(long waitTime) {
                    dialog.setMessage("等待" + waitTime + "ms后检评论……");
                }

                @Override
                public void onStartCheckComment() {
                    dialog.setMessage("检查评论中……");
                }

                @Override
                public void thenOk() {
                    dialog.dismiss();
                    DialogUtil.dialogMessage(context,"检查结果","评论正常显示！");
                }

                @Override
                public void thenShadowBan() {
                    dialog.dismiss();
                    DialogUtil.dialogMessage(context,"检查结果","评论被ShadowBan！");
                }

                @Override
                public void thenQuickDelete() {
                    dialog.dismiss();
                    DialogUtil.dialogMessage(context,"检查结果","评论被系统秒删！");
                }

                @Override
                public void thenError() {
                    dialog.dismiss();
                    DialogUtil.dialogMessage(context,":(","啥情况！观众能看到评论而发评者却不能\n!!!∑(ﾟДﾟノ)ノ");
                }

                @Override
                public void onNetworkError(Throwable th) {
                    dialog.dismiss();
                    toastNetError(th.getMessage());
                }
            });
             */

            commentPresenter.checkCommentStatus(commentArea, comment, commentUtil.getRandomComment(commentArea), rpid, parent, root,hasPictures, new CommentPresenter.CheckCommentStatusCallBack() {
                @Override
                public void onSleeping(long waitTime,long waitTimeByPictures) {
                    if (waitTimeByPictures == -1){
                        dialog.setMessage("等待" + waitTime + "ms后检评论……");
                    } else {
                        dialog.setMessage("评论包含图片，等待"+waitTime+"+"+waitTimeByPictures+"="+(waitTime+waitTimeByPictures)+"ms后检查评论……");
                    }

                }

                @Override
                public void onStartCheckComment() {
                    dialog.setMessage("检查评论中……");
                }

                @Override
                public void thenOk() {
                    dialog.dismiss();
                    showCommentIsOkResult(comment);
                }

                @Override
                public void onCommentNotFound(String sentTestComment) {
                    dialog.setMessage("评论列表未找到该评论，判断状态中……");
                    //dialog.setMessage("评论列表未找到该评论，正在使用测试评论：“" + sentTestComment + "”对其回复判断状态……");
                }

                @Override
                public void onPageTurnForHasAccReply(int pn) {
                    dialog.setMessage("正在有账号条件下查找评论回复列表，第"+pn+"页");
                }

                @Override
                public void onOtherError(int code, String message) {
                    dialog.dismiss();
                    DialogUtil.dialogMessage(context,"获取评论回复时发生错误！","code:"+code+"\nmessage:"+message);
                }

                @Override
                public void onAccountFailure(int code, String message) {
                    dialog.dismiss();
                    if (code == -101) {
                        showTokenExpires("登录信息已过期，请重新登录（获取cookie）！");
                    } else {
                        showTokenExpires("code:" + code + "\nmessage:" + message);
                    }
                }

                @Override
                public void thenInvisible() {
                    dialog.dismiss();
                    showCommentBannedResult(BannedCommentBean.BANNED_TYPE_INVISIBLE,commentArea,rpid,parent,root,comment);
                }

                @Override
                public void thenShadowBan() {
                    dialog.dismiss();
                    showCommentBannedResult(BannedCommentBean.BANNED_TYPE_SHADOW_BAN, commentArea, rpid, parent, root, comment);
                }

                @Override
                public void thenUnderReview() {
                    dialog.dismiss();
                    showCommentBannedResult(BannedCommentBean.BANNED_TYPE_UNDER_REVIEW, commentArea, rpid, parent, root, comment);
                }

                @Override
                public void thenQuickDelete() {
                    dialog.dismiss();
                    showCommentBannedResult(BannedCommentBean.BANNED_TYPE_QUICK_DELETE, commentArea, rpid, parent, root, comment);
                }

                @Override
                public void onNetworkError(Throwable th) {
                    dialog.dismiss();
                    exitListener.exit();
                    toastNetError(th.getMessage());
                }
            });


        } else {
            dialog.dismiss();
            DialogUtil.dialogMessage(context, "未登录", "请先设置cookie！");
        }
    }

    private void showCommentIsOkResult(String comment) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("检查结果")
                .setMessage("你的评论：“" + comment + "”正常显示！")
                .setCancelable(false)
                .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitListener.exit();
                    }
                })
                .setOnKeyListener((dialog1, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK){
                        exitListener.exit();
                        return true;
                    }
                    return false;
                })
                .show();
    }


    private void showTokenExpires(String message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("账号错误")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitListener.exit();
                    }
                })
                .show();
    }

    private void showCommentBannedResult(String bannedType, CommentArea commentArea, long rpid, long parent, long root, String comment) {
        AlertDialog.Builder resultDialogBuilder = new AlertDialog.Builder(context).setTitle("检查结果");
        if (bannedType.equals(BannedCommentBean.BANNED_TYPE_SHADOW_BAN)) {
            resultDialogBuilder.setIcon(R.drawable.hide_black);
            resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(comment, 100) + "”在无账号环境下无法找到，自己账号下获取该评论的回复列表成功，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严");
        }else if (bannedType.equals(BannedCommentBean.BANNED_TYPE_UNDER_REVIEW)){
            resultDialogBuilder.setIcon(R.drawable.hide_black);
            resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(comment, 100) + "”在无账号环境下无法找到，自己账号下获取该评论的回复列表成功，接着又能在无账号下获取回复，疑似审核中，此时你可能无法申诉（回复无可申诉评论），请后续来统计中复查（记得搜遍评论区）！");
        } else if (bannedType.equals(BannedCommentBean.BANNED_TYPE_QUICK_DELETE)) {
            resultDialogBuilder.setIcon(R.drawable.deleted_black);
            resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(comment, 100) + "”在自己账号下获取该评论的回复列表和对该评论发送回复时均收到提示：“" + "已经被删除了" + "”，判定改评论被系统速删，请检查评论内容或者检查评论区是否被戒严");
        } else if (bannedType.equals(BannedCommentBean.BANNED_TYPE_INVISIBLE)){
            resultDialogBuilder.setIcon(R.drawable.ghost_black);
            resultDialogBuilder.setMessage("您的评论“" + CommentUtil.subComment(comment, 100) + "”在无账号环境下成功找到，但是被标记invisible，也就是隐身（在前端被隐藏）！这是非常罕见的情况……通常在评论发送很久时间后才会出现。可以的话把评论信息发给开发者，以分析触发条件");
        }
        resultDialogBuilder.setPositiveButton("关闭", (dialog, which) -> exitListener.exit());
        resultDialogBuilder.setNeutralButton("检查评论区", null);
        resultDialogBuilder.setNegativeButton("更多评论选项", null);
        AlertDialog resultDialog = resultDialogBuilder.show();
        //检查评论区
        resultDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "检测评论区是否被戒严", "发布测试评论中……");
            progressDialog.setCancelable(false);
            progressDialog.show();
            checkAreaMartialLaw(commentArea, comment, rpid, progressDialog);
        });
        //更多评论选项
        resultDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("更多选项").setItems(new String[]{"扫描敏感词", "申诉", "删除发布的评论","复制rpid、oid、type"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: //扫描敏感词
                            CommentArea yourCommentArea = commentUtil.getYourCommentArea();
                            scanSensitiveWorld(commentArea, yourCommentArea, rpid, comment, commentUtil.getRandomComment(yourCommentArea), commentUtil.getRandomComment(yourCommentArea));
                            break;
                        case 1: //申诉
                            AlertDialog dialog1 = new AlertDialog.Builder(context)
                                    .setTitle("警告")
                                    .setMessage("评论不能正常显示时判断评论状态会发送测试回复评论、测试戒严的评论区会发送测试评论，请注意：申诉后，这些自己删掉的测试评论可能会被恢复，如果通知是“无法恢复”，那么不用管他，如果是“无违规”，请注意去删除测试评论！")
                                    .setNegativeButton("还是算了", new VoidDialogInterfaceOnClickListener())
                                    .setNeutralButton("官方申诉网址", (dialog23, which1) -> {
                                        Uri uri = Uri.parse("https://www.bilibili.com/h5/comment/appeal");
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        context.startActivity(intent);
                                    })
                                    .setPositiveButton("去申诉", (dialog24, which1) -> {
                                        AppealDialogPresenter appealDialogPresenter = new AppealDialogPresenter(context, handler, commentManipulator);
                                        appealDialogPresenter.appeal(CommentUtil.sourceIdToUrl(commentArea), comment, new AppealDialogPresenter.CallBack() {

                                            @Override
                                            public void onRespInUI(int code, String toastText) {
                                                //如果这个时候还出现“无可申述评论”那么可能把评论状态误判了或者在某种审核中
                                                if (code == 12082) {
                                                    commentPresenter.statisticsDBOpenHelper.updateBannedCommentBannedType(String.valueOf(rpid), BannedCommentBean.BANNED_TYPE_SUSPECTED_NO_PROBLEM);
                                                    toastLong(toastText + "\n可能因为检查评论时误判了或评论在某种处理或审核状态，等待一段时间后应该可以显示");
                                                } else {
                                                    toastLong(toastText);
                                                }
                                            }

                                            @Override
                                            public void onNetErrInUI(String msg) {
                                                toastNetError(msg);
                                            }
                                        });
                                    })
                                    .show();
                            break;
                        case 2: //删除发布的评论
                            commentPresenter.deleteComment(commentArea, rpid).enqueue(new BiliApiCallback<Void>() {
                                @Override
                                public void onError(Throwable th) {
                                    toastNetError(th.getMessage());
                                }

                                @Override
                                public void onSuccess(Void unused) {
                                    resultDialog.dismiss();
                                    toastLong("删除成功！");
                                    exitListener.exit();
                                }
                            });
                            break;
                        case 3://复制rpid等评论信息
                            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData mClipData = ClipData.newPlainText("Label", "rpid:"+rpid+"\noid:"+commentArea.oid+"\ntype:"+commentArea.areaType);
                            cm.setPrimaryClip(mClipData);
                            toastShort("已复制");
                    }
                }
            }).show();
        });
    }

    private void checkAreaMartialLaw(CommentArea commentArea, String mainComment, long mainCommRpid, ProgressDialog progressDialog) {
        commentPresenter.checkCommentAreaMartialLaw(commentArea, mainCommRpid, commentUtil.getRandomComment(commentArea), commentUtil.getRandomComment(commentArea), new CommentPresenter.CheckCommentAreaMartialLawCalBack() {
            @Override
            public void onTestCommentSent(String testComment) {
                progressDialog.setMessage("已发送测评论：“" + testComment + "”，等待设置好的时间后检查评论……");
            }

            @Override
            public void onStartCheck() {
                progressDialog.setMessage("检查中……");
            }

            @Override
            public void onCommentSendFail(int code, String message) {
                progressDialog.dismiss();
                if (code == -101) {
                    showTokenExpires("收到错误信息：“" + message + "”，测试评论发送失败，可能Token已过期，请重新登录获取Cookie！");
                } else {
                    toastLong(message);
                }
            }

            @Override
            public void thenAreaOk() {
                progressDialog.dismiss();
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle("评论区检查结果")
                        .setMessage("评论区没有戒严，是否继续检查该评论是否仅在此评论区被ban？")
                        .setPositiveButton("检查", (dialog1, which) -> {
                            checkIfBannedOnlyInThisArea(mainCommRpid, mainComment);
                        })
                        .setNegativeButton("不了", new VoidDialogInterfaceOnClickListener())
                        .show();
            }

            @Override
            public void thenMartialLaw() {
                progressDialog.dismiss();
                DialogUtil.dialogMessage(context, "检查结果", "评论区被戒严！");
            }

            @Override
            public void onNetworkError(Throwable th) {
                progressDialog.dismiss();
                toastNetError(th.getMessage());
            }
        });
    }

    private void checkIfBannedOnlyInThisArea(long mainCommentRpid, String mainComment) {
        ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, "检测评论是否仅在该评论区被ban", "等待设置好的时间后发送评论到你的评论区进行测试……");
        progressDialog.setCancelable(false);
        progressDialog.show();
        CommentArea yourCommentArea = commentUtil.getYourCommentArea();
        if (yourCommentArea != null) {
            commentPresenter.checkIfOnlyBannedInThisArea(yourCommentArea, mainCommentRpid, mainComment, new CommentPresenter.CheckIfOnlyBannedInThisAreaCallBack() {
                @Override
                public void onCommentSent(String yourCommentArea) {
                    progressDialog.setMessage("已将评论发送至你的评论区：" + yourCommentArea);
                }

                @Override
                public void onStartCheck() {
                    progressDialog.setMessage("检查中……");
                }

                @Override
                public void thenOnlyBannedInThisArea() {
                    progressDialog.dismiss();
                    showResult("该评论仅在此评论区被ban，因为发送在你的评论区能正常显示");
                }

                @Override
                public void thenBannedInYourArea() {
                    progressDialog.dismiss();
                    showResult("该评论不仅在此评论区被ban，因为发送在你的评论区也不能正常显示");
                }

                @Override
                public void onNetworkError(Throwable th) {
                    progressDialog.dismiss();
                    toastNetError(th.getMessage());
                }

                private void showResult(String message) {
                    DialogUtil.dialogMessage(context, "检查结果", message);
                }
            });
        } else {
            commentUtil.setYourCommentArea(context, commentPresenter);
        }
    }

    private void scanSensitiveWorld(CommentArea mainCommentArea, CommentArea yourCommentArea, long mainCommRpid, String comment, String testComment1, String testComment2) {
        if (yourCommentArea == null) {
            commentUtil.setYourCommentArea(context, commentPresenter);
        } else if (comment.length() < 8) {
            toastShort("您要扫描的评论太短！至少8个字符");
        } else {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("准备扫描敏感词");
            progressDialog.show();
            commentPresenter.readyToScanSensitiveWorld(mainCommentArea, yourCommentArea, mainCommRpid, comment, testComment1, testComment2, new CommentPresenter.ReadyToScanSensitiveWorldCallBack() {
                @Override
                public void onCommentIsOnlyBannedInThisArea() {
                    progressDialog.dismiss();
                    showNoNeedToScan(context.getString(R.string.no_need_to_scan_only_ban));
                }

                @Override
                public void onCommentAreaIsMartialLaw() {
                    progressDialog.dismiss();
                    showNoNeedToScan(context.getString(R.string.no_need_to_scan_martial_law));
                }

                @Override
                public void onStartCheckIsOnlyBannedInThisArea() {
                    progressDialog.setMessage("正在检查该评论是否仅在此评论区被ban");
                }

                @Override
                public void onStartCheckAreaMartialLaw() {
                    progressDialog.setMessage("正在检查评论区是否被戒严……");
                }

                @Override
                public void startScan() {
                    progressDialog.dismiss();
                    scanningSensitiveWord_UI(yourCommentArea, comment);
                }

                @Override
                public void onNetworkError(Throwable th) {
                    progressDialog.dismiss();
                    toastNetError(th.getMessage());
                }
            });
        }
    }


    private void showNoNeedToScan(String message) {
        new AlertDialog.Builder(context)
                .setTitle("没必要扫描了:(")
                .setMessage(message)
                .setPositiveButton("知道了", new VoidDialogInterfaceOnClickListener()).show();
    }

    //偷个懒，copy旧代码还能跑就懒得动了，嵌套不严重😂
    private void scanningSensitiveWord_UI(CommentArea yourCommentArea, String comment) {
        View dialogView = View.inflate(context, R.layout.dialog_scanning_sensitive_word, null);
        TextView tvx_result = dialogView.findViewById(R.id.txv_scanning_result_of_sensitive_world);
        ProgressBar prog_scanning_ssw = dialogView.findViewById(R.id.prog_scanning_ssw);
        TextView txv_scanning_status = dialogView.findViewById(R.id.txv_scanning_status);
        TextView txv_scanning_progress = dialogView.findViewById(R.id.txv_scanning_progress);
        AlertDialog scanningDialog = new AlertDialog.Builder(context).setTitle("正在扫描敏感词……").setView(dialogView).setCancelable(false).setPositiveButton("关闭", null).show();
        Button buttonClose = scanningDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonClose.setEnabled(false);
        new Thread(() -> {
            /*
            计算扫描要多少次？你只需要高中知识
            例如最小块为4，评论长度为256，要经过这扫描过程：256/2/2/2/2/2/2,直到  4<结果大小<8  ,扫描次数为6
            分析可得公式 设扫描次数为x
            最小块大小*2^x=256
            最小块大小=4
            4*2^x=256
            等式两边同时*4
            2^x=64
            x=log2(64)
            x=6
             */
            try {
                int max = (int) (Math.log((comment.length() + 1) / 4) / Math.log(2));//根据换底公式，logx(y)=lgy/lgx-
                int currProg = 1;
                Log.i("comment.length", String.valueOf(comment.length() + 1));
                Log.i("max:", String.valueOf(max));
                prog_scanning_ssw.setMax(max);
                handler.post(() -> {
                    tvx_result.setText(comment);
                    txv_scanning_progress.setText("0/" + max);
                });
                String passText = "";
                String[] split = splitFromTheMiddle(comment);

                ForegroundColorSpan greenSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.green));
                ForegroundColorSpan redSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.red));
                ForegroundColorSpan yellowSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.yellow));
                ForegroundColorSpan blueSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.blue));

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (split != null) {
                    System.out.println(Arrays.toString(split));
                    String finalPassText = passText;
                    String[] finalSplit = split;
                    handler.post(() -> {
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                        spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        spannableStringBuilder.setSpan(yellowSpan, finalPassText.length(), finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        spannableStringBuilder.setSpan(blueSpan, finalPassText.length() + finalSplit[0].length(), finalPassText.length() + finalSplit[0].length() + finalSplit[1].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        tvx_result.setText(spannableStringBuilder);
                        txv_scanning_status.setText("发送评论&等待……");
                    });
                    GeneralResponse<CommentAddResult> resp = commentManipulator.sendComment(passText + split[0], 0, 0, yourCommentArea).execute().body();
                    long rpid1 = resp.data.rpid;
                    try {
                        Thread.sleep(commentPresenter.waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int finalCurrProg = currProg;
                    handler.post(() -> {
                        prog_scanning_ssw.setProgress(finalCurrProg);
                        txv_scanning_progress.setText(finalCurrProg + "/" + max);
                        if (finalCurrProg != max) {
                            txv_scanning_status.setText("检查评论……");
                        } else {
                            scanningDialog.setTitle("扫描已完成");
                            txv_scanning_status.setText("检查完毕！");
                            buttonClose.setEnabled(true);
                            buttonClose.setOnClickListener(v -> {
                                scanningDialog.dismiss();
                            });
                        }
                    });
                    if (commentManipulator.checkComment(yourCommentArea, rpid1)) {
                        passText += split[0];
                        handler.post(() -> {
                            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                            spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            spannableStringBuilder.setSpan(redSpan, finalPassText.length() + finalSplit[0].length(), finalPassText.length() + finalSplit[0].length() + finalSplit[1].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            tvx_result.setText(spannableStringBuilder);
                        });
                        split = splitFromTheMiddle(split[1]);
                    } else {
                        handler.post(() -> {
                            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                            spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            spannableStringBuilder.setSpan(redSpan, finalPassText.length(), finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            tvx_result.setText(spannableStringBuilder);
                        });
                        split = splitFromTheMiddle(split[0]);
                    }
                    commentManipulator.deleteComment(yourCommentArea, rpid1).execute();
                    System.out.println(passText);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currProg++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String[] splitFromTheMiddle(String input) {
        if (input.length() >= 8) {
            return new String[]{input.substring(0, input.length() / 2), input.substring(input.length() / 2)};
        } else {
            return null;
        }
    }

    public void toAppeal(String comment, CommentArea commentArea, String parent, String root, String areaIdentifier) {

    }

    private void toastShort(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void toastLong(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    private void toastNetError(String msg) {
        toastShort("网络错误：" + msg);
    }

}
