package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Date;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.AppealDialogPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.CommentPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {
    EditText edt_bvid, edt_comment;
    Button btn_send, btn_clean, btn_send_and_appeal;
    SharedPreferences sp_config;
    CommentManipulator commentManipulator;
    CommentPresenter commentPresenter;
    DrawerLayout drawerLayout;
    SwitchCompat sw_auto_recorde;
    ConstraintLayout cl_banned_comment_sw;
    LinearLayout ll_banned_comments, ll_martial_law_comment_area_list, ll_wait_time,ll_github_project;
    //NavigationView navigation_view;
    Toolbar toolbar;
    private Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    boolean enableRecorde;
    LinearLayout ll_test_comment_pool;
    LinearLayout ll_you_comment_area;
    CommentUtil commentUtil;
    Handler handler;
    DialogCommCheckWorker dialogCommSendWorker;

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
        ll_banned_comments = findViewById(R.id.ll_banned_comment_list);
        ll_martial_law_comment_area_list = findViewById(R.id.ll_martial_law_comment_area_list);
        ll_test_comment_pool = findViewById(R.id.ll_test_comment_pool);
        ll_you_comment_area = findViewById(R.id.ll_your_comment_area);
        ll_wait_time = findViewById(R.id.ll_wait_time);
        cl_banned_comment_sw = findViewById(R.id.cl_banned_comment_sw);
        ll_github_project = findViewById(R.id.ll_github_project);

        sw_auto_recorde = findViewById(R.id.sw_auto_recorde);
        btn_send_and_appeal = findViewById(R.id.btn_send_and_appeal);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        commentUtil = new CommentUtil(sp_config);
        commentManipulator = new CommentManipulator(new OkHttpClient(), sp_config.getString("cookie", ""));
        handler = new Handler();
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        commentPresenter = new CommentPresenter(handler, commentManipulator, statisticsDBOpenHelper, sp_config.getLong("wait_time", 5000), sp_config.getBoolean("autoRecorde", true));
        dialogCommSendWorker = new DialogCommCheckWorker(context, handler, commentManipulator, commentPresenter, commentUtil, () -> {
        });

        enableRecorde = sp_config.getBoolean("autoRecorde", true);
        sw_auto_recorde.setChecked(enableRecorde);
        cl_banned_comment_sw.setOnClickListener(v -> {
            sw_auto_recorde.setChecked(!enableRecorde);
        });

        sw_auto_recorde.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableRecorde = true;
                commentPresenter.setEnableStatistics(true);
                sp_config.edit().putBoolean("autoRecorde", true).apply();
                Toast.makeText(context, "开启自动记录", Toast.LENGTH_SHORT).show();
            } else {
                enableRecorde = false;
                commentPresenter.setEnableStatistics(false);
                sp_config.edit().putBoolean("autoRecorde", false).apply();
                Toast.makeText(context, "关闭自动记录", Toast.LENGTH_SHORT).show();
            }
        });

        ll_banned_comments.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BannedCommentListActivity.class));
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
            commentUtil.setYourCommentArea(context, commentPresenter);
        });

        ll_wait_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogView = View.inflate(context, R.layout.edit_text, null);
                EditText editText = dialogView.findViewById(R.id.edit_text);
                editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                editText.setText(String.valueOf(sp_config.getLong("wait_time", 5000)));
                new AlertDialog.Builder(context).setTitle("设置发评后等待时间（毫秒/ms）")
                        .setView(dialogView)
                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long waitTime = Long.parseLong(editText.getText().toString());
                                sp_config.edit().putLong("wait_time", waitTime).apply();
                                commentPresenter.setWaitTime(waitTime);
                                toastLong("设置成功！");
                            }
                        })
                        .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                        .show();
            }
        });

        ll_github_project.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://github.com/freedom-introvert/biliSendCommAntifraud");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        });

        btn_send.setOnClickListener(v -> {
            //Toast.makeText(MainActivity.this,edt_comment.getText().toString(),Toast.LENGTH_LONG).show();
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle("发布并检测评论");
            dialog.setMessage("正在获取评论区信息……");
            dialog.setCancelable(false);
            dialog.show();

            commentPresenter.matchToArea(edt_bvid.getText().toString(), new CommentPresenter.MatchToAreaCallBack() {
                @Override
                public void onMatchedArea(CommentArea commentArea) {
                    if (commentArea != null) {
                        dialog.setMessage("发送评论中……");
                        String comment = edt_comment.getText().toString();
                        commentManipulator.sendComment(comment, 0, 0, commentArea).enqueue(new BiliApiCallback<GeneralResponse<CommentAddResult>>() {
                            @Override
                            public void onError(Throwable th) {
                                dialog.dismiss();
                                toastNetErr(th.getMessage());
                            }

                            @Override
                            public void onSuccess(GeneralResponse<CommentAddResult> response) {
                                if (commentSendSuccess(response, commentArea, comment, dialog)) {
                                    dialogCommSendWorker.checkComment(commentArea, response.data.rpid, 0, 0, comment, dialog);
                                }
                            }
                        });
                    } else {
                        Toast.makeText(context, R.string.bv_cv_url_typo, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onNetworkError(Throwable th) {
                    dialog.dismiss();
                    toastNetErr(th.getMessage());
                }
            });
        });

        //发送后直接申诉
        btn_send_and_appeal.setOnClickListener(v -> {
            if (commentManipulator.cookieAreSet()) {
                ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "正在获取评论区信息……");
                progressDialog.show();
                String comment = edt_comment.getText().toString();

                commentPresenter.matchToArea(edt_bvid.getText().toString(), new CommentPresenter.MatchToAreaCallBack() {
                    @Override
                    public void onNetworkError(Throwable th) {
                        progressDialog.dismiss();
                        toastShort(th.getMessage());
                    }

                    @Override
                    public void onMatchedArea(CommentArea commentArea) {
                        if (commentArea != null) {
                           progressDialog.setMessage("发送评论中……");
                            AppealDialogPresenter appealDialogPresenter = new AppealDialogPresenter(context, handler, commentManipulator);
                            commentManipulator.sendComment(comment,0,0,commentArea).enqueue(new BiliApiCallback<GeneralResponse<CommentAddResult>>() {
                                @Override
                                public void onError(Throwable th) {
                                    progressDialog.dismiss();
                                    toastLong(th.getMessage());
                                }

                                @Override
                                public void onSuccess(GeneralResponse<CommentAddResult> response) {
                                    progressDialog.dismiss();
                                    toastLong("评论发送成功，请填写申诉信息");
                                    if (commentSendSuccess(response,commentArea,comment,progressDialog)) {
                                        appealDialogPresenter.appeal(edt_bvid.getText().toString(), comment, new AppealDialogPresenter.CallBack() {
                                            @Override
                                            public void onRespInUI(int code, String toastText) {
                                                if (code == 0) {
                                                    if (enableRecorde) {
                                                        statisticsDBOpenHelper.insertBannedComment(new BannedCommentBean(commentArea, response.data.rpid, comment, BannedCommentBean.BANNED_TYPE_UNKNOWN, new Date(), BannedCommentBean.CHECKED_NO_CHECK));
                                                    }
                                                    DialogUtil.dialogMessage(context, "评论被ban", toastText);
                                                } else if (code == 12082) {
                                                    DialogUtil.dialogMessage(context, "评论正常显示！", toastText);
                                                } else {
                                                    DialogUtil.dialogMessage(context, "其他情况", toastText);
                                                }
                                            }

                                            @Override
                                            public void onNetErrInUI(String msg) {
                                                toastNetErr(msg);
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            toastShort(R.string.bv_cv_url_typo);
                        }
                    }
                });
            } else {
                toastShort("请先设置cookie");
            }
        });


        btn_clean.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setTitle("确认清空输入内容？").setPositiveButton("确定", (dialog, which) -> {
                edt_bvid.setText("");
                edt_comment.setText("");
            }).setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener()).show();
        });

    }




    private boolean commentSendSuccess(GeneralResponse<CommentAddResult> response, CommentArea commentArea, String comment, DialogInterface dialog) {
        if (response.isSuccess()) {
            if (response.data.success_action == 0) {
                return true;
            } else {
                dialog.dismiss();
                new AlertDialog.Builder(context).setMessage(response.data.success_toast).setPositiveButton("留着", new VoidDialogInterfaceOnClickListener())
                        .setNegativeButton("删除", (dialog1, which) -> {
                            commentManipulator.deleteComment(commentArea, response.data.rpid).enqueue(new BiliApiCallback<Void>() {
                                @Override
                                public void onError(Throwable th) {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).show();
            }
        } else if (response.code == CommentAddResult.CODE_CONTAIN_SENSITIVE) {//包含敏感词时
            if (enableRecorde) {
                statisticsDBOpenHelper.insertBannedComment(new BannedCommentBean(commentArea, "st" + System.currentTimeMillis(), comment, BannedCommentBean.BANNED_TYPE_SENSITIVE, new Date(), BannedCommentBean.CHECKED_NO_CHECK));
            }
            dialog.dismiss();
            toastLong(response.message);
        } else {
            dialog.dismiss();
            toastLong(response.message);
        }
        return false;
    }

    private void toastShort(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void toastShort(int text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void toastLong(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private void toastNetErr(String e) {
        toastShort("网络错误：" + e);
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
                    String cookie = editText.getText().toString();
                    sp_config.edit().putString("cookie", cookie).commit();
                    commentManipulator.setCookie(cookie);
                }).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setNeutralButton("网页登录获取", (dialog, which) -> {
                    startActivity(new Intent(context, WebViewLoginActivity.class));
                }).show();
                break;
            default:
                break;
        }
        return true;
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
