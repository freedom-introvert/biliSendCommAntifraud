package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.GeneralResponse;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentUtil;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.presenters.AppealDialogPresenter;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.okretro.BiliApiCallback;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressTimer;

public class MainActivity extends AppCompatActivity {
    private static final int RESULT_CODE_SAVE_LOG_ZIP = 1;
    EditText edt_bvid, edt_comment;
    Button btn_send, btn_clean, btn_send_and_appeal, btn_test;
    CommentManipulator commentManipulator;
    DrawerLayout drawerLayout;
    SwitchCompat sw_recorde_history;
    SwitchCompat sw_hook_picture_select;
    ConstraintLayout cl_recorde_history_comment_sw;
    LinearLayout ll_pending_check_comments, ll_martial_law_comment_area_list, ll_history_comment, ll_wait_time, ll_github_project;
    //NavigationView navigation_view;
    Toolbar toolbar;
    private Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    boolean enableRecordeHistoryComment;
    LinearLayout ll_test_comment_pool;
    LinearLayout ll_you_comment_area;
    LinearLayout ll_export_logs;
    CommentUtil commentUtil;
    Handler handler;
    DialogCommCheckWorker dialogCommSendWorker;
    Config config;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ByXposedLaunchedActivity.lastActivity = this;
        context = this;
        config = new Config(context);
        commentUtil = new CommentUtil(context);
        commentManipulator = new CommentManipulator(config.getCookie(),config.getDeputyCookie());
        handler = new Handler();
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        dialogCommSendWorker = new DialogCommCheckWorker(context,config,statisticsDBOpenHelper, commentManipulator, commentUtil);

        initView();
        initRecordeHistoryCommentSW();
        initTestCommentPoolItem();
        initWaitTimeItem();
        initHomePageCommentCheck();
        initToNewActivityItem();
        initExportLogs();
        ll_you_comment_area.setOnClickListener(v -> {
            commentUtil.setYourCommentArea(context, commentManipulator);
        });
        findViewById(R.id.ll_forward_dynamic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentUtil.setDynamicIdToBeForward(context, commentManipulator);
            }
        });


        /*findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getData() != null){
            switch (requestCode) {
                case RESULT_CODE_SAVE_LOG_ZIP:
                    ProgressBarDialog dialog = new ProgressBarDialog.Builder(context)
                            .setMessage("导出日志中……")
                            .setIndeterminate(true)
                            .show();
                    new Thread(() -> {
                        try {
                            File sourceFolder = new File(getFilesDir(),"logs");
                            OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                            ZipOutputStream zos = new ZipOutputStream(outputStream);
                            zipDirectory(sourceFolder, sourceFolder, zos);
                            zos.close();
                            outputStream.close();
                            runOnUiThread(() -> {
                                dialog.dismiss();
                                Toast.makeText(context, "导出完成！", Toast.LENGTH_SHORT).show();
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                dialog.dismiss();
                                Toast.makeText(context, "导出失败！\n原因:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                    break;
                default:
                    throw new RuntimeException();
            }
        }
    }

    private static void zipDirectory(File rootPath, File sourceFolder, ZipOutputStream zos) throws IOException {
        for (File file : sourceFolder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(rootPath, file, zos);
            } else {
                addToZip(rootPath, file, zos);
            }
        }
    }

    private static void addToZip(File rootPath, File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        String zipFilePath = file.getAbsolutePath().substring(rootPath.getAbsolutePath().length() + 1);
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    private static long calculateFolderSize(File folder) {
        long size = 0;

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += calculateFolderSize(file);
                    }
                }
            }
        } else if (folder.isFile()) {
            size += folder.length();
        }

        return size;
    }

    private static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        return folder.delete();
    }



    private void initExportLogs() {
        ll_export_logs.setOnClickListener(v -> {
            File sourceFolder = new File(getFilesDir(),"logs");
            if (!sourceFolder.exists()){
                Toast.makeText(context, "无日志可导出！", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINA);
            intent.putExtra(Intent.EXTRA_TITLE,"logs_"+sdf.format(new Date())+".zip");
            startActivityForResult(intent,RESULT_CODE_SAVE_LOG_ZIP);
        });
        ll_export_logs.setOnLongClickListener(v -> {
            File logsFolder = new File(getFilesDir(),"logs");
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setMessage(String.format("确认删除全部日志吗？(占用空间%.2fMB)\n日志保留100条，100条之前的会自动删除，您可以不用手动清理",(double) calculateFolderSize(logsFolder) / (1024 * 1024),Locale.getDefault()))
                    .setNegativeButton("取消",new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFolder(logsFolder);
                            Toast.makeText(MainActivity.this, "删除完成！", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return false;
        });
    }

    private void initView() {
        drawerLayout = findViewById(R.id.drawerLayout);
        //navigation_view = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
        edt_bvid = findViewById(R.id.edt_bvid);
        edt_comment = findViewById(R.id.edt_comment);
        btn_send = findViewById(R.id.btn_send);
        btn_clean = findViewById(R.id.btn_clean);

        ll_pending_check_comments = findViewById(R.id.ll_pending_check_comment_list);
        ll_martial_law_comment_area_list = findViewById(R.id.ll_martial_law_comment_area_list);
        ll_history_comment = findViewById(R.id.ll_history_comment);
        ll_test_comment_pool = findViewById(R.id.ll_test_comment_pool);
        ll_you_comment_area = findViewById(R.id.ll_your_comment_area);
        ll_wait_time = findViewById(R.id.ll_wait_time);
        ll_export_logs = findViewById(R.id.ll_export_logs);
        cl_recorde_history_comment_sw = findViewById(R.id.cl_recorde_history_comment_sw);
        ll_github_project = findViewById(R.id.ll_github_project);

        sw_recorde_history = findViewById(R.id.sw_recorde_history);


        btn_send_and_appeal = findViewById(R.id.btn_send_and_appeal);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initRecordeHistoryCommentSW() {
        enableRecordeHistoryComment = config.getRecordeHistory();
        sw_recorde_history.setChecked(enableRecordeHistoryComment);
        cl_recorde_history_comment_sw.setOnClickListener(v -> {
            sw_recorde_history.setChecked(!enableRecordeHistoryComment);
        });

        sw_recorde_history.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableRecordeHistoryComment = true;
                config.setRecordeHistory(true);
                Toast.makeText(context, "开启历史评论记录", Toast.LENGTH_SHORT).show();
            } else {
                enableRecordeHistoryComment = false;
                config.setRecordeHistory(false);
                Toast.makeText(context, "关闭历史评论记录", Toast.LENGTH_SHORT).show();
            }
        });

       /* sw_hook_picture_select.setChecked(xConfig.getHookPictureSelectIsEnable());
        findViewById(R.id.cl_hook_picture_select).setOnClickListener(v -> {
            sw_hook_picture_select.setChecked(!xConfig.getHookPictureSelectIsEnable());
        });
        sw_hook_picture_select.setOnCheckedChangeListener((buttonView, isChecked) -> {
            xConfig.setHookPictureSelectEnable(isChecked);
        });*/


    }


    private void initToNewActivityItem() {
        ll_pending_check_comments.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PendingCheckCommentsActivity.class));
        });

        ll_martial_law_comment_area_list.setOnClickListener(v -> {
            startActivity(new Intent(context, MartialLawCommentAreaListActivity.class));
        });

        ll_history_comment.setOnClickListener(v -> {
            startActivity(new Intent(context, HistoryCommentActivity.class));
        });

        ll_github_project.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://github.com/freedom-introvert/biliSendCommAntifraud");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        });
    }

    private void initTestCommentPoolItem() {
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
    }

    private void initWaitTimeItem() {
        ll_wait_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View dialogView = View.inflate(context, R.layout.dialog_set_wait_time, null);
                EditText editTextWTByCommentSent = dialogView.findViewById(R.id.edit_text_wt_by_after_comment_sent);
                EditText editTextWTByHasPictures = dialogView.findViewById(R.id.edit_text_wt_by_has_pictures);
                EditText editTextWTByDanmakuSent = dialogView.findViewById(R.id.edit_text_wt_danmaku_sent);
                editTextWTByCommentSent.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                editTextWTByCommentSent.setText(String.valueOf(config.getWaitTime()));
                editTextWTByHasPictures.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                editTextWTByHasPictures.setText(String.valueOf(config.getWaitTimeByHasPictures()));
                editTextWTByDanmakuSent.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                editTextWTByDanmakuSent.setText(String.valueOf(config.getWaitTimeByDanmakuSend()));
                new AlertDialog.Builder(context).setTitle("设置发评后等待时间（毫秒/ms）")
                        .setView(dialogView)
                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long waitTime = Long.parseLong(editTextWTByCommentSent.getText().toString());
                                long waitTimeByHasPictures = Long.parseLong(editTextWTByHasPictures.getText().toString());
                                long waitTimeByDanmakuSent = Long.parseLong(editTextWTByDanmakuSent.getText().toString());
                                config.setWaitTime(waitTime);
                                config.setWaitTimeByHasPictures(waitTimeByHasPictures);
                                config.setWaitTimeByDanmakuSend(waitTimeByDanmakuSent);
                                toastLong("设置成功！");
                            }
                        })
                        .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                        .show();
            }
        });
    }

    private void initHomePageCommentCheck() {
        btn_send.setOnClickListener(v -> {
            //Toast.makeText(MainActivity.this,edt_comment.getText().toString(),Toast.LENGTH_LONG).show();
            ProgressBarDialog dialog = new ProgressBarDialog.Builder(context)
                    .setTitle("发布并检测评论")
                    .setMessage("正在获取评论区信息……")
                    .setIndeterminate(true)
                    .setCancelable(false)
                    .show();
            commentManipulator.matchCommentAreaInUi(edt_bvid.getText().toString(), new CommentManipulator.MatchCommentAreaCallBack() {
                @Override
                public void onNetworkError(IOException e) {
                    dialog.dismiss();
                    toastNetErr(e.getMessage());
                }

                @Override
                public void onMatchedArea(CommentArea commentArea) {
                    if (commentArea != null) {
                        dialog.setMessage("发送评论中……");
                        String comment = edt_comment.getText().toString();
                        commentManipulator.sendComment(comment, 0, 0, commentArea,false).enqueue(new BiliApiCallback<GeneralResponse<CommentAddResult>>() {
                            @Override
                            public void onError(Throwable th) {
                                dialog.dismiss();
                                toastNetErr(th.getMessage());
                            }

                            @Override
                            public void onSuccess(GeneralResponse<CommentAddResult> response) {
                                if (commentSendSuccess(response, commentArea, comment, dialog)) {
                                    dialog.setIndeterminate(false);
                                    new Thread(() -> {
                                        long waitTime = config.getWaitTime();
                                        new ProgressTimer(waitTime, ProgressBarDialog.DEFAULT_MAX_PROGRESS, new ProgressTimer.ProgressLister() {
                                            @Override
                                            public void onNewProgress(int progress, long sleepSeg) {
                                                runOnUiThread(() -> {
                                                    dialog.setMessage("等待("+progress*sleepSeg+"/"+waitTime+")ms后检查评论");
                                                    dialog.setProgress(progress);
                                                });
                                            }
                                        }).start();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialogCommSendWorker.checkComment(new Comment(commentArea, response.data.rpid, 0, 0, comment, null,new Date(response.data.reply.ctime*1000)), dialog);
                                            }
                                        });
                                    }).start();

                                }
                            }
                        });
                    } else {
                        Toast.makeText(context, R.string.bv_cv_url_typo, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        });

        //发送后直接申诉
        btn_send_and_appeal.setOnClickListener(v -> {
            if (commentManipulator.cookieAreSet()) {
                ProgressDialog progressDialog = DialogUtil.newProgressDialog(context, null, "正在获取评论区信息……");
                progressDialog.show();
                String comment = edt_comment.getText().toString();
                commentManipulator.matchCommentAreaInUi(edt_bvid.getText().toString(), new CommentManipulator.MatchCommentAreaCallBack() {
                    @Override
                    public void onNetworkError(IOException e) {
                        progressDialog.dismiss();
                        toastShort(e.getMessage());
                    }

                    @Override
                    public void onMatchedArea(CommentArea commentArea) {
                        if (commentArea != null) {
                            progressDialog.setMessage("发送评论中……");
                            AppealDialogPresenter appealDialogPresenter = new AppealDialogPresenter(context, handler, commentManipulator);
                            commentManipulator.sendComment(comment, 0, 0, commentArea,false).enqueue(new BiliApiCallback<GeneralResponse<CommentAddResult>>() {
                                @Override
                                public void onError(Throwable th) {
                                    progressDialog.dismiss();
                                    toastLong(th.getMessage());
                                }

                                @Override
                                public void onSuccess(GeneralResponse<CommentAddResult> response) {
                                    progressDialog.dismiss();
                                    toastLong("评论发送成功，请填写申诉信息");
                                    if (commentSendSuccess(response, commentArea, comment, progressDialog)) {
                                        appealDialogPresenter.appeal(edt_bvid.getText().toString(), comment, new AppealDialogPresenter.CallBack() {
                                            @Override
                                            public void onRespInUI(int code, String toastText) {
                                                HistoryComment historyComment = new HistoryComment(new Comment(commentArea, response.data.rpid, 0, 0, comment, HistoryComment.STATE_UNKNOWN, new Date(response.data.reply.ctime)));
                                                if (code == 0) {
                                                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_UNKNOWN);
                                                    DialogUtil.dialogMessage(context, "评论被ban", toastText);
                                                } else if (code == 12082) {
                                                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_NORMAL);
                                                    DialogUtil.dialogMessage(context, "评论正常显示！", toastText);
                                                } else {
                                                    historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_UNKNOWN);
                                                    DialogUtil.dialogMessage(context, "其他情况", toastText);
                                                    return;
                                                }
                                                if (config.getRecordeHistoryIsEnable()){
                                                    statisticsDBOpenHelper.insertHistoryComment(historyComment);
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
                            commentManipulator.createDeleteCommentCall(commentArea, response.data.rpid).enqueue(new BiliApiCallback<GeneralResponse<Object>>() {
                                @Override
                                public void onError(Throwable th) {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onSuccess(GeneralResponse<Object> unused) {
                                    if (unused.isSuccess()) {
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }).show();
            }
        } else if (response.code == CommentAddResult.CODE_CONTAIN_SENSITIVE) {//包含敏感词时
            if (config.getRecordeHistoryIsEnable()){
                HistoryComment historyComment = new HistoryComment(new Comment(commentArea, -System.currentTimeMillis(),0,0, comment, null, new Date()));
                historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SENSITIVE);
                statisticsDBOpenHelper.insertHistoryComment(historyComment);
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
        if (item.getItemId() == R.id.main_account_cookie){
            View edtView = View.inflate(MainActivity.this, R.layout.edit_text, null);
            EditText editText = edtView.findViewById(R.id.edit_text);
            editText.setText(config.getCookie());
            new AlertDialog.Builder(MainActivity.this).setTitle("设置cookie（主号）").setView(edtView).setPositiveButton("设置", (dialog, which) -> {
                String cookie = editText.getText().toString();
                config.setCookie(cookie);
                commentManipulator.setCookie(cookie);
            }).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setNeutralButton("网页登录获取", (dialog, which) -> {
                startActivity(new Intent(context, WebViewLoginActivity.class));
            }).show();
        } else if (item.getItemId() == R.id.deputy_account_cookie){
            View edtView = View.inflate(MainActivity.this, R.layout.edit_text, null);
            EditText editText = edtView.findViewById(R.id.edit_text);
            editText.setText(config.getDeputyCookie());
            new AlertDialog.Builder(MainActivity.this).setTitle("设置cookie（小号）").setView(edtView).setPositiveButton("设置", (dialog, which) -> {
                String cookie = editText.getText().toString();
                config.setDeputyCookie(cookie);
            }).setNegativeButton("取消", new VoidDialogInterfaceOnClickListener()).setNeutralButton("网页登录获取", (dialog, which) -> {
                startActivity(new Intent(context, WebViewLoginByDeputyActivity.class));
            }).show();
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
