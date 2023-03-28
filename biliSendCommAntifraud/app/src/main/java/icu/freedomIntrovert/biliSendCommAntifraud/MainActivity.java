package icu.freedomIntrovert.biliSendCommAntifraud;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import okhttp3.OkHttpClient;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.DialogUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BandCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class MainActivity extends AppCompatActivity {
    EditText edt_bvid, edt_comment;
    Button btn_send, btn_clean, btn_send_and_appeal;
    SharedPreferences sp_config;
    CommentManipulator commentManipulator;
    DrawerLayout drawerLayout;
    SwitchCompat sw_auto_recorde;
    ConstraintLayout cl_band_comment_sw;
    LinearLayout ll_band_comment, ll_martial_law_comment_area_list;
    //NavigationView navigation_view;
    Toolbar toolbar;
    private Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    boolean enableRecorde;
    LinearLayout ll_test_comment_pool;
    LinearLayout ll_you_comment_area;
    CommentUtil commentUtil;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        drawerLayout = findViewById(R.id.drawerLayout);
        //navigation_view = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
        edt_bvid = findViewById(R.id.edt_bvid);
        edt_comment = findViewById(R.id.edt_comment);
        btn_send = findViewById(R.id.btn_send);
        btn_clean = findViewById(R.id.btn_clean);
        sp_config = getSharedPreferences("config", Context.MODE_PRIVATE);
        ll_band_comment = findViewById(R.id.ll_band_comment_list);
        ll_martial_law_comment_area_list = findViewById(R.id.ll_martial_law_comment_area_list);
        ll_test_comment_pool = findViewById(R.id.ll_test_comment_pool);
        ll_you_comment_area = findViewById(R.id.ll_your_comment_area);
        cl_band_comment_sw = findViewById(R.id.cl_band_comment_sw);
        sw_auto_recorde = findViewById(R.id.sw_auto_recorde);
        btn_send_and_appeal = findViewById(R.id.btn_send_and_appeal);
        commentUtil = new CommentUtil(sp_config);
        commentManipulator = new CommentManipulator(new OkHttpClient(), sp_config.getString("cookie", ""));
        setSupportActionBar(toolbar);
        // 將drawerLayout和toolbar整合，會出現「三」按鈕
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        enableRecorde = sp_config.getBoolean("autoRecorde", true);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        sw_auto_recorde.setChecked(enableRecorde);
        cl_band_comment_sw.setOnClickListener(v -> {
            sw_auto_recorde.setChecked(!enableRecorde);
        });

        sw_auto_recorde.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableRecorde = true;
                sp_config.edit().putBoolean("autoRecorde", true).commit();
                Toast.makeText(context, "开启自动记录", Toast.LENGTH_SHORT).show();
            } else {
                enableRecorde = false;
                sp_config.edit().putBoolean("autoRecorde", false).commit();
                Toast.makeText(context, "关闭自动记录", Toast.LENGTH_SHORT).show();
            }
        });

        ll_band_comment.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BandCommentListActivity.class));
        });

        ll_martial_law_comment_area_list.setOnClickListener(v -> {
            startActivity(new Intent(context, MartialLawCommentAreaListActivity.class));
        });

        ll_test_comment_pool.setOnClickListener(v -> {
            View edtView = View.inflate(context, R.layout.edit_text, null);
            EditText editText = edtView.findViewById(R.id.edit_text);
            editText.setText(commentUtil.getSourceRandomComments());
            AlertDialog setRandomDialog = new AlertDialog.Builder(context).setTitle("随机测试评论池，用换行符分割，一行一个，越多越好").setView(edtView).setPositiveButton("设置", null).setNeutralButton("说明", null).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).show();
            setRandomDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                if (editText.getText().toString().split("\n").length > 3) {
                    commentUtil.updateRandomComments(editText.getText().toString());
                    setRandomDialog.dismiss();
                } else {
                    editText.setError("最少输入4条测试评论！");
                }
            });
            setRandomDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v12 -> {
                new AlertDialog.Builder(context).setMessage("随机抽取一个用于回复自己的评论以及测试戒严评论区。因为重复（甚至相似）评论发布或回复的时候，有一定可能会被删掉或仅自己可见，造成检测戒严评论区或戒严评论区默认处理评论方式误判").setPositiveButton("知道了", new VoidDialogInterfaceOnClickListener()).show();
            });
        });

        ll_you_comment_area.setOnClickListener(v -> {
            setYourCommentArea();
        });

        btn_send.setOnClickListener(v -> {
            //Toast.makeText(MainActivity.this,edt_comment.getText().toString(),Toast.LENGTH_LONG).show();
            ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle("发布并检测评论中……");
            dialog.setMessage("发布评论中……");
            dialog.show();
            new Thread(() -> {
                try {
                    CommentArea commentArea = commentManipulator.matchCommentArea(edt_bvid.getText().toString());
                    if (sp_config.getString("cookie", "").contains("bili_jct=")) {
                        if (commentArea != null) {
                            String oid = commentArea.oid;
                            String comment = edt_comment.getText().toString();
                            JSONObject callBack = commentManipulator.sendComment(comment, commentArea, null, null);
                            if (callBack.getInteger("code") == 0) {
                                if (!callBack.getJSONObject("data").getString("success_toast").contains("精选")) {
                                    long rpid = callBack.getJSONObject("data").getLong("rpid");
                                    runOnUiThread(() -> {
                                        dialog.setMessage("收到系统回复：“" + callBack.getJSONObject("data").getString("success_toast") + "”，等待2秒后检查评论是否正常……");
                                        //dialog.dismiss();
                                    });
                                    try {
                                        //等待b站处理评论，不然太快没处理好会导致正常的评论没有出现在列表里
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    boolean found = commentManipulator.checkComment(commentArea, rpid);
                                    if (found) {
                                        dialogCheckResultOnUI("你的评论：“" + comment + "”正常显示！！", comment, null, dialog, commentArea, rpid);
                                    } else {
                                        runOnUiThread(() -> {
                                            dialog.setMessage("对评论：“" + comment + "”，rpid：“" + rpid + "”，回复测试评论以检查该评论状态……");
                                        });
                                        JSONObject callBack1 = commentManipulator.sendComment(commentUtil.getRandomComment(commentArea), commentArea, String.valueOf(rpid), String.valueOf(rpid));
                                        if (callBack1.getInteger("code") == 12022) {
                                            if (enableRecorde) {
                                                statisticsDBOpenHelper.insertBannedComment(new BandCommentBean(commentArea, String.valueOf(rpid), comment, BandCommentBean.BANNED_TYPE_QUICK_DELETE, new Date(), BandCommentBean.CHECKED_NO_CHECK));
                                            }
                                            dialogCheckResultOnUI("尝试回复评论“" + comment + "”时收到系统回复：“" + callBack1.getString("message") + "”，判定改评论被系统速删，请检查评论内容或者检查评论区是否被戒严（评论区被戒严可能性更大）", comment, BandCommentBean.BANNED_TYPE_QUICK_DELETE, dialog, commentArea, rpid);
                                        } else if (callBack1.getInteger("code") == 0) {
                                            runOnUiThread(() -> {
                                                dialog.setMessage("删除用于测试的回复评论……");
                                            });
                                            try {
                                                //等一会，避免评论在服务器没有准备好就进行删除，导致评论没有删除掉
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            commentManipulator.deleteComment(commentArea, callBack1.getJSONObject("data").getLong("rpid"));
                                            if (enableRecorde) {
                                                statisticsDBOpenHelper.insertBannedComment(new BandCommentBean(commentArea, String.valueOf(rpid), comment, BandCommentBean.BANNED_TYPE_SHADOW_BAN, new Date(), BandCommentBean.CHECKED_NO_CHECK));
                                            }
                                            dialogCheckResultOnUI("您的评论“" + comment + "”在无账号环境下无法找到，但自己可以成功对其进行回复，判定为被ShadowBan（仅自己可见），请检查评论内容或者检查评论区是否被戒严", comment, BandCommentBean.BANNED_TYPE_SHADOW_BAN, dialog, commentArea, rpid);
                                        }
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        dialog.dismiss();
                                        new AlertDialog.Builder(context).setMessage(callBack.getJSONObject("data").getString("success_toast")).setPositiveButton("留着", new VoidDialogInterfaceOnClickListener()).setNegativeButton("删除", (dialog1, which) -> {
                                            new Thread(() -> {
                                                try {
                                                    commentManipulator.deleteComment(commentArea, callBack.getJSONObject("data").getLong("rpid"));
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                                    });
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                                    });
                                                }
                                            }).start();
                                        }).show();
                                    });
                                }
                            } else if (callBack.getInteger("code") == 12016) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "code:" + callBack.getString("code") + "  " + callBack.getString("message"), Toast.LENGTH_SHORT).show();
                                    if (enableRecorde) {
                                        statisticsDBOpenHelper.insertBannedComment(new BandCommentBean(commentArea, "st" + System.currentTimeMillis(), comment, BandCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BandCommentBean.CHECKED_NO_CHECK));
                                    }
                                    dialog.dismiss();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, callBack.toJSONString(), Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "你输入的BV号不正确！", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "您未设置cookie或设置了错误的cookie", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    });
                }
            }).start();
        });

        btn_send_and_appeal.setOnClickListener(v -> {
            View dialogView = View.inflate(context, R.layout.dialog_appeal_comment, null);
            EditText edt_appeal_area_location = dialogView.findViewById(R.id.edt_appeal_area_location);
            EditText edt_reason = dialogView.findViewById(R.id.edt_reason);
            edt_appeal_area_location.setText(edt_bvid.getText().toString());
            edt_reason.setText("评论内容:" + edt_comment.getText().toString());
            AlertDialog editAppealInfoDialog = new AlertDialog.Builder(context)
                    .setTitle("填写申诉信息")
                    .setView(dialogView)
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("确定", null)
                    .show();
            editAppealInfoDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edt_appeal_area_location.getText().toString().equals("")) {
                        edt_appeal_area_location.setError("请输入所在稿件BV号或位置链接");
                    } else if (edt_reason.getText().toString().length() < 10){
                        edt_reason.setError("申诉理由要大于10个字");
                    } else if (edt_reason.getText().toString().length() > 99){
                        edt_reason.setError("申诉理由不能超过99个字");
                    } else {
                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setMessage("发送评论并等待2秒……");
                        progressDialog.show();
                        new Thread(() -> {
                            try {
                                CommentArea commentArea = commentManipulator.matchCommentArea(edt_bvid.getText().toString());
                                String comment = edt_comment.getText().toString();
                                if (commentArea != null){
                                    JSONObject sendRespJson = commentManipulator.sendComment(comment, commentArea, null, null);
                                    Thread.sleep(2000);
                                    if (sendRespJson.getInteger("code") == 0) {
                                        long rpid = CommentManipulator.getRpidInSendRespJSON(sendRespJson);
                                        runOnUiThread(() -> {
                                            progressDialog.setMessage("收到回复“发送成功”，提交申诉中……");
                                        });
                                        JSONObject appealRespJson = commentManipulator.appealComment(edt_appeal_area_location.getText().toString(), edt_reason.getText().toString());
                                        runOnUiThread(() -> {
                                            editAppealInfoDialog.dismiss();
                                            progressDialog.dismiss();
                                            if (appealRespJson.getInteger("code") == 0) {
                                                if(enableRecorde){
                                                    statisticsDBOpenHelper.insertBannedComment(new BandCommentBean(commentArea,rpid,comment,BandCommentBean.BANNED_TYPE_UNKNOWN,new Date(),BandCommentBean.CHECKED_NO_CHECK));
                                                }
                                                DialogUtil.dialogMessage(context,"评论被ban！",appealRespJson.getJSONObject("data").getString("success_toast"));
                                            } else if (appealRespJson.getInteger("code") == 12082){
                                                DialogUtil.dialogMessage(context,"评论正常显示！",appealRespJson.getString("message"));
                                            }else {
                                                Toast.makeText(context, appealRespJson.toJSONString(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(context,sendRespJson.toJSONString(),Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(context,"输入的评论区地址错误",Toast.LENGTH_LONG).show();
                                }
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            });
        });



        btn_clean.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("确认清空输入内容？").setPositiveButton("确定", (dialog, which) -> {
                edt_bvid.setText("");
                edt_comment.setText("");
            }).setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener()).show();

        });
    }


    public static String[] splitFromTheMiddle(String input) {
        if (input.length() >= 8) {
            return new String[]{input.substring(0, input.length() / 2), input.substring(input.length() / 2)};
        } else {
            return null;
        }
    }

    private void dialogCheckResultOnUI(String message, String comment, String bannedType, Dialog dialog, CommentArea commentArea, long rpid) {
        runOnUiThread(() -> {
            if (dialog != null) {
                dialog.dismiss();
            }
            AlertDialog.Builder resultDialogBuilder = new AlertDialog.Builder(MainActivity.this).setTitle("检查结果").setMessage(message).setPositiveButton("关闭", (dialog1, which) -> {
            });
            if (bannedType != null) {
                if (bannedType.equals(BandCommentBean.BANNED_TYPE_SHADOW_BAN)) {
                    resultDialogBuilder.setIcon(R.drawable.hide_black);
                } else if (bannedType.equals(BandCommentBean.BANNED_TYPE_QUICK_DELETE)) {
                    resultDialogBuilder.setIcon(R.drawable.deleted_black);
                }
                resultDialogBuilder.setNeutralButton("检查评论区", null);
                resultDialogBuilder.setNegativeButton("更多评论选项", null);
                AlertDialog resultDialog = resultDialogBuilder.show();
                resultDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    ProgressDialog dialog2 = new ProgressDialog(MainActivity.this);
                    dialog2.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog2.setTitle("检测评论区是否被戒严……");
                    dialog2.setMessage("发布测试评论中……");
                    dialog2.show();
                    new Thread(() -> {
                        try {
                            JSONObject callBack = commentManipulator.sendComment(commentUtil.getRandomComment(commentArea), commentArea, null, null);
                            if (callBack.getInteger("code") == 0) {
                                long testCommentRpid = callBack.getJSONObject("data").getLong("rpid");
                                runOnUiThread(() -> {
                                    dialog2.setMessage("收到系统回复：“" + callBack.getJSONObject("data").getString("success_toast") + "”，等待2秒后检查评论是否正常……");
                                });
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                runOnUiThread(() -> {
                                    dialog2.setMessage("检查中……");
                                });
                                boolean noMartialLaw = commentManipulator.checkComment(commentArea, testCommentRpid);
                                if (noMartialLaw) {
                                    commentManipulator.deleteComment(commentArea, testCommentRpid);
                                    if (enableRecorde) {
                                        statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_NOT_MARTIAL_LAW);
                                    }
                                    runOnUiThread(() -> {
                                        dialog2.dismiss();
                                        AlertDialog dialogCkMLResult = new AlertDialog.Builder(context)
                                                .setTitle("检查结果").setMessage("评论区没有戒严，是否继续检查该评论是否仅在此评论区被ban？")
                                                .setPositiveButton("检查", null)
                                                .setNegativeButton("不了", new VoidDialogInterfaceOnClickListener())
                                                .show();
                                        dialogCkMLResult.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                                            CommentArea yourCommentArea = commentUtil.getYourCommentArea();
                                            if (yourCommentArea != null) {
                                                dialogCkMLResult.dismiss();
                                                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                                progressDialog.setTitle("检查中……");
                                                progressDialog.setMessage("发布评论在你的评论区然后并检测……");
                                                progressDialog.show();
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            JSONObject testCallBack = commentManipulator.sendComment(comment, yourCommentArea, null, null);
                                                            Thread.sleep(2000);
                                                            long testRpid = CommentManipulator.getRpidInSendRespJSON(testCallBack);
                                                            if (commentManipulator.checkComment(yourCommentArea, testRpid)) {
                                                                if (enableRecorde) {
                                                                    statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA);
                                                                }
                                                                showCheckResult("该评论仅在此评论区被ban，因为发到你的评论区就能正常显示。这有可能因为「查重黑名单」或者其他原因所致。");
                                                            } else {
                                                                if (enableRecorde) {
                                                                    statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
                                                                }
                                                                showCheckResult("该评论就连在你的评论区发也被ban，这应该全站也不行吧……若你想找出其中的敏感词，可以使用[更多评论选项]中的「扫描敏感词」功能！");
                                                            }
                                                            commentManipulator.deleteComment(yourCommentArea, testRpid);
                                                        } catch (IOException | InterruptedException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    private void showCheckResult(String message) {
                                                        runOnUiThread(() -> {
                                                            progressDialog.dismiss();
                                                            new AlertDialog.Builder(context).setTitle("检查结果").setMessage(message).setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener()).show();
                                                        });
                                                    }
                                                }).start();
                                            } else {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(context, "请先设置你的评论区！", Toast.LENGTH_LONG).show();
                                                    setYourCommentArea();
                                                });
                                            }
                                        });
                                    });
                                    //dialogCheckResultOnUI("评论区正常", comment, null, dialog2, commentArea, testCommentRpid);
                                } else {
                                    if (enableRecorde) {
                                        statisticsDBOpenHelper.deleteBandComment(String.valueOf(rpid));
                                        statisticsDBOpenHelper.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(commentArea, testCommentRpid, commentUtil.getRandomComment(commentArea)));
                                    }
                                    commentManipulator.deleteComment(commentArea, testCommentRpid);
                                    dialogCheckResultOnUI("评论区被戒严", comment, null, dialog2, commentArea, testCommentRpid);
                                }
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, callBack.toJSONString(), Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            dialog2.dismiss();
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }).start();
                });
                resultDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                    AlertDialog dialogMoreOpt = new AlertDialog.Builder(context).setTitle("更多选项").setItems(new String[]{"扫描敏感词", "申诉", "删除发布的评论"}, (dialog22, which1) -> {
                        switch (which1) {
                            case 0:
                                //TODO:扫描敏感词
                                CommentArea yourCommentArea = commentUtil.getYourCommentArea();
                                if (yourCommentArea == null) {
                                    Toast.makeText(context, "请先设置你的评论区！", Toast.LENGTH_LONG).show();
                                    setYourCommentArea();
                                } else if (comment.length() > 8) {
                                    //TODO 是否检查过评论仅在该评论区被ban
                                    Boolean checkedIsOnlyBan = statisticsDBOpenHelper.getCommentIsOnlyBannedInThisArea(String.valueOf(rpid));
                                    if (checkedIsOnlyBan == null) {
                                        //TODO 是否检查过戒严
                                        Boolean areaIsMartialLaw = statisticsDBOpenHelper.getCommentAreaIsMartialLaw(commentArea.oid, String.valueOf(rpid));
                                        if (areaIsMartialLaw == null) {
                                            ProgressDialog dialog2 = new ProgressDialog(MainActivity.this);
                                            dialog2.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                            dialog2.setMessage("检测该评论区是否为戒严评论区……");
                                            dialog2.show();
                                            new Thread(() -> {
                                                try {
                                                    JSONObject checkMLCallBack = commentManipulator.sendComment(commentUtil.getRandomComment(commentArea), commentArea, null, null);
                                                    if (checkMLCallBack.getInteger("code") == 0) {
                                                        long testCommentRpid = CommentManipulator.getRpidInSendRespJSON(checkMLCallBack);
                                                        try {
                                                            Thread.sleep(2000);
                                                        } catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                        }
                                                        boolean noMartialLaw = commentManipulator.checkComment(commentArea, testCommentRpid);
                                                        if (noMartialLaw) {
                                                            if (enableRecorde) {
                                                                statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_NOT_MARTIAL_LAW);
                                                            }
                                                            commentManipulator.deleteComment(commentArea, testCommentRpid);
                                                            runOnUiThread(() -> {
                                                                dialog2.dismiss();
                                                                checkOnlyBanAndScanningSsWd_UI(yourCommentArea, comment, rpid);
                                                            });
                                                        } else {
                                                            if (enableRecorde) {
                                                                statisticsDBOpenHelper.deleteBandComment(String.valueOf(rpid));
                                                                statisticsDBOpenHelper.insertMartialLawCommentArea(commentManipulator.getMartialLawCommentArea(commentArea, testCommentRpid, commentUtil.getRandomComment(commentArea)));
                                                            }
                                                            commentManipulator.deleteComment(commentArea, testCommentRpid);
                                                            runOnUiThread(() -> {
                                                                dialog2.dismiss();
                                                                showNoNeedToScan_UI(getString(R.string.no_need_to_scan_martial_law));
                                                            });
                                                        }
                                                    } else {
                                                        runOnUiThread(() -> {
                                                            Toast.makeText(MainActivity.this, checkMLCallBack.toJSONString(), Toast.LENGTH_LONG).show();
                                                        });
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }).start();
                                        } else if (areaIsMartialLaw) {
                                            if (enableRecorde) {
                                                statisticsDBOpenHelper.deleteBandComment(String.valueOf(rpid));
                                            }
                                            showNoNeedToScan_UI(getString(R.string.no_need_to_scan_martial_law));
                                        } else {
                                            checkOnlyBanAndScanningSsWd_UI(yourCommentArea, comment, rpid);
                                        }
                                    } else if (checkedIsOnlyBan) {
                                        showNoNeedToScan_UI(getString(R.string.no_need_to_scan_only_ban));
                                    } else {
                                        scanningSensitiveWord_UI(yourCommentArea, comment);
                                    }
                                } else {
                                    Toast.makeText(context, "您要扫描的评论太短！至少8个字符", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 1:
                                //TODO:申诉
                                AlertDialog dialog1 = new AlertDialog.Builder(context)
                                        .setTitle("警告")
                                        .setMessage("评论不能正常显示时判断评论状态会发送测试回复评论、测试戒严的评论区会发送测试评论，请注意：申诉后，这些自己删掉的测试评论可能会被恢复，如果通知是“无法恢复”，那么不用管他，如果是“无违规”，请注意去删除测试评论！")
                                        .setNegativeButton("还是算了", new VoidDialogInterfaceOnClickListener())
                                        .setNeutralButton("官方申诉网址", (dialog23, which) -> {
                                            Uri uri = Uri.parse("https://www.bilibili.com/h5/comment/appeal");
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            context.startActivity(intent);
                                        })
                                        .setPositiveButton("去申诉", (dialog24, which) -> {
                                            View dialogView = View.inflate(context, R.layout.dialog_appeal_comment, null);
                                            EditText edt_appeal_area_location = dialogView.findViewById(R.id.edt_appeal_area_location);
                                            EditText edt_reason = dialogView.findViewById(R.id.edt_reason);
                                            edt_appeal_area_location.setText(edt_bvid.getText().toString());
                                            edt_reason.setText("评论内容:" + edt_comment.getText().toString());
                                            AlertDialog editAppealInfoDialog = new AlertDialog.Builder(context)
                                                    .setTitle("填写申诉信息")
                                                    .setView(dialogView)
                                                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                                                    .setPositiveButton("提交申诉", null)
                                                    .show();
                                            editAppealInfoDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    if (edt_appeal_area_location.getText().toString().equals("")) {
                                                        edt_appeal_area_location.setError("请输入所在稿件BV号或位置链接");
                                                    } else if (edt_reason.getText().toString().length() < 10){
                                                        edt_reason.setError("申诉理由要大于10个字");
                                                    } else if (edt_reason.getText().toString().length() > 99){
                                                        edt_reason.setError("申诉理由不能超过99个字");
                                                    } else {
                                                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                                        progressDialog.setMessage("提交申诉中……");
                                                        progressDialog.show();
                                                        new Thread(() -> {
                                                            try {
                                                                JSONObject appealRespJson = commentManipulator.appealComment(edt_appeal_area_location.getText().toString(), edt_reason.getText().toString());
                                                                runOnUiThread(() -> {
                                                                    if (appealRespJson.getInteger("code") == 0){
                                                                        Toast.makeText(context,appealRespJson.getJSONObject("data").getString("success_toast"),Toast.LENGTH_LONG).show();
                                                                    } else {
                                                                        Toast.makeText(context,appealRespJson.toJSONString(),Toast.LENGTH_LONG).show();
                                                                    }
                                                                    progressDialog.dismiss();
                                                                    editAppealInfoDialog.dismiss();
                                                                });
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }).start();
                                                    }
                                                }
                                            });
                                        })
                                        .show();
                                break;
                            case 2:
                                new Thread(() -> {
                                    try {
                                        JSONObject callBack = commentManipulator.deleteComment(commentArea, rpid);
                                        boolean success = callBack.getInteger("code") == 0;
                                        runOnUiThread(() -> {
                                            if (success) {
                                                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(MainActivity.this, "删除失败", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                    runOnUiThread(resultDialog::dismiss);
                                }).start();
                                break;
                        }
                    }).show();
                });
            } else {
                resultDialogBuilder.show();
            }
        });

    }


    private void showNoNeedToScan_UI(String message) {
        new AlertDialog.Builder(context).setTitle("没必要扫描了:(").setMessage(message)
                //.setMessage("你的评论只是在当前评论区无法显示，但发送在你的评论区可以正常显示。没有扫描的必要！详情请了解「查重黑名单」机制")
                .setPositiveButton("知道了", new VoidDialogInterfaceOnClickListener()).show();
    }

    private void checkOnlyBanAndScanningSsWd_UI(CommentArea yourCommentArea, String comment, long rpid) {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("检测该内容评论是否仅在该评论区不能正常显示……");
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject sendYourAreaCallBack = commentManipulator.sendComment(comment, yourCommentArea, null, null);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long testInYourAreaRpid = CommentManipulator.getRpidInSendRespJSON(sendYourAreaCallBack);
                    if (commentManipulator.checkComment(yourCommentArea, testInYourAreaRpid)) {
                        if (enableRecorde) {
                            statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA);
                        }
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            showNoNeedToScan_UI(getString(R.string.no_need_to_scan_only_ban));
                        });
                    } else {
                        if (enableRecorde) {
                            statisticsDBOpenHelper.updateCheckedArea(String.valueOf(rpid), BandCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA);
                        }
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            scanningSensitiveWord_UI(yourCommentArea, comment);
                        });
                    }
                    commentManipulator.deleteComment(yourCommentArea, testInYourAreaRpid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


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
                runOnUiThread(() -> {
                    tvx_result.setText(comment);
                    txv_scanning_progress.setText("0/" + max);
                });
                String passText = "";
                String[] split = splitFromTheMiddle(comment);

                ForegroundColorSpan greenSpan = new ForegroundColorSpan(getResources().getColor(R.color.green));
                ForegroundColorSpan redSpan = new ForegroundColorSpan(getResources().getColor(R.color.red));
                ForegroundColorSpan yellowSpan = new ForegroundColorSpan(getResources().getColor(R.color.yellow));
                ForegroundColorSpan blueSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue));

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (split != null) {
                    System.out.println(Arrays.toString(split));
                    String finalPassText = passText;
                    String[] finalSplit = split;
                    runOnUiThread(() -> {
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                        spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        spannableStringBuilder.setSpan(yellowSpan, finalPassText.length(), finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        spannableStringBuilder.setSpan(blueSpan, finalPassText.length() + finalSplit[0].length(), finalPassText.length() + finalSplit[0].length() + finalSplit[1].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        tvx_result.setText(spannableStringBuilder);
                        txv_scanning_status.setText("发送评论……");
                    });
                    JSONObject callBack = commentManipulator.sendComment(passText + split[0], yourCommentArea, null, null);
                    long rpid1 = callBack.getJSONObject("data").getLong("rpid");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int finalCurrProg = currProg;
                    runOnUiThread(() -> {
                        prog_scanning_ssw.setProgress(finalCurrProg);
                        txv_scanning_progress.setText(finalCurrProg + "/" + max);
                        if (finalCurrProg != max) {
                            txv_scanning_status.setText("检查评论……");
                        } else {
                            txv_scanning_status.setText("检查完毕！");
                            buttonClose.setEnabled(true);
                            buttonClose.setOnClickListener(v -> {
                                scanningDialog.dismiss();
                            });
                        }
                    });
                    if (commentManipulator.checkComment(yourCommentArea, rpid1)) {
                        passText += split[0];
                        runOnUiThread(() -> {
                            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                            spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            spannableStringBuilder.setSpan(redSpan, finalPassText.length() + finalSplit[0].length(), finalPassText.length() + finalSplit[0].length() + finalSplit[1].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            tvx_result.setText(spannableStringBuilder);
                        });
                        split = splitFromTheMiddle(split[1]);
                    } else {
                        runOnUiThread(() -> {
                            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(comment);
                            spannableStringBuilder.setSpan(greenSpan, 0, finalPassText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            spannableStringBuilder.setSpan(redSpan, finalPassText.length(), finalPassText.length() + finalSplit[0].length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                            tvx_result.setText(spannableStringBuilder);
                        });
                        split = splitFromTheMiddle(split[0]);
                    }
                    commentManipulator.deleteComment(yourCommentArea, rpid1);
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

    private void setYourCommentArea() {
        View dialogView = View.inflate(context, R.layout.edit_text, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(commentUtil.getAreaSourceText());
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("你的评论区（你是up主），用于检测查重黑名单与扫描敏感词").setView(dialogView).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setPositiveButton("设置", null).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v13 -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (commentUtil.setYourCommentArea(editText.getText().toString(), commentManipulator)) {
                            runOnUiThread(() -> {
                                Toast.makeText(context, "设置成功！", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        } else {
                            error("输入的内容未解析到评论区！");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        error("网络错误：" + e.getMessage());
                    }
                }

                private void error(String msg) {
                    runOnUiThread(() -> {
                        editText.setError(msg);
                    });
                }
            }).start();
        });
    }

    private void showNetworkError(Exception e) {

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cookie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cookie:
                View edtView = View.inflate(MainActivity.this, R.layout.edit_text, null);
                EditText editText = edtView.findViewById(R.id.edit_text);
                editText.setText(sp_config.getString("cookie", ""));
                new AlertDialog.Builder(MainActivity.this).setTitle("设置cookie").setView(edtView).setPositiveButton("设置", (dialog, which) -> {
                    sp_config.edit().putString("cookie", editText.getText().toString()).commit();
                    commentManipulator = new CommentManipulator(new OkHttpClient(), sp_config.getString("cookie", ""));
                }).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setNeutralButton("网页登录获取", (dialog, which) -> {
                    startActivity(new Intent(context, WebViewLoginActivity.class));
                }).show();
                break;
            default:
                break;
        }
        return true;
    }
}