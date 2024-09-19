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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;
import icu.freedomIntrovert.biliSendCommAntifraud.async.SendCommentTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.account.CookieGetAccountTask;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.BiliComment;
import icu.freedomIntrovert.biliSendCommAntifraud.biliApis.CommentAddResult;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.view.ProgressBarDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.AccountSelectionDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.AppealCommentDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.SetForwardDynamicDialog;
import icu.freedomIntrovert.biliSendCommAntifraud.workerdialog.SetRandomCommentsDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_SAVE_LOG_ZIP = 1;
    private static final int REQUEST_CODE_GET_COOKIE = 2;
    EditText edt_bvid, edt_comment;
    Button btn_test;
    CommentManipulator commentManipulator;
    DrawerLayout drawerLayout;
    SwitchCompat sw_recorde_history;
    SwitchCompat sw_use_client_cookie;
    SwitchCompat sw_hook_picture_select;
    Toolbar toolbar;
    private Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    boolean enableRecordeHistoryComment;
    Handler handler;
    DialogCommCheckWorker dialogCommCheckWorker;
    Config config;
    EditText currentEditText; // 当前正在编辑的Cookie输入框

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        config = Config.getInstance(context);

        commentManipulator = CommentManipulator.getInstance();
        handler = new Handler();
        statisticsDBOpenHelper = StatisticsDBOpenHelper.getInstance(context);
        dialogCommCheckWorker = new DialogCommCheckWorker(context);

        initView();
        initRecordeHistoryCommentSW();
        initExportLogs();




       /* Intent serviceIntent = new Intent(this, CommentMonitoringService.class);
        ContextCompat.startForegroundService(this, serviceIntent);*/


        /*findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case REQUEST_CODE_SAVE_LOG_ZIP:
                    if (data.getData() == null) {
                        return;
                    }
                    ProgressBarDialog dialog = new ProgressBarDialog.Builder(context)
                            .setMessage("导出日志中……")
                            .setIndeterminate(true)
                            .show();
                    new Thread(() -> {
                        try {
                            File sourceFolder = new File(getFilesDir(), "logs");
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
                                Toast.makeText(context, "导出失败！\n原因:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                    break;
                case REQUEST_CODE_GET_COOKIE:
                    if (currentEditText != null) {
                        currentEditText.setText(data.getStringExtra("cookie"));
                    }
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
        View ll_export_logs = findViewById(R.id.ll_export_logs);
        ll_export_logs.setOnClickListener(v -> {
            File sourceFolder = new File(getFilesDir(), "logs");
            if (!sourceFolder.exists()) {
                Toast.makeText(context, "无日志可导出！", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINA);
            intent.putExtra(Intent.EXTRA_TITLE, "logs_" + sdf.format(new Date()) + ".zip");
            startActivityForResult(intent, REQUEST_CODE_SAVE_LOG_ZIP);
        });
        ll_export_logs.setOnLongClickListener(v -> {
            File logsFolder = new File(getFilesDir(), "logs");
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setMessage(String.format(Locale.getDefault(), "确认删除全部日志吗？(占用空间%.2fMB)\n日志保留100条，100条之前的会自动删除，您可以不用手动清理",
                            (double) calculateFolderSize(logsFolder) / (1024 * 1024)))
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .setPositiveButton("删除", (dialog1, which) -> {
                        deleteFolder(logsFolder);
                        Toast.makeText(MainActivity.this, "删除完成！", Toast.LENGTH_SHORT).show();
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

        setThisOnclickListener(R.id.ll_pending_check_comment_list,
                R.id.ll_martial_law_comment_area_list,
                R.id.ll_history_comment,
                R.id.ll_random_test_comment_pool,
                R.id.ll_wait_time,
                R.id.ll_forward_dynamic,
                R.id.cl_recorde_history_comment_sw,
                R.id.cl_use_client_cookie,
                R.id.ll_targeting,
                R.id.ll_github_project,
                R.id.btn_send,
                R.id.btn_send_and_appeal,
                R.id.btn_clean);

        sw_recorde_history = findViewById(R.id.sw_recorde_history);
        sw_use_client_cookie = findViewById(R.id.sw_use_client_cookie);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initRecordeHistoryCommentSW() {
        enableRecordeHistoryComment = config.getRecordeHistory();
        sw_recorde_history.setChecked(enableRecordeHistoryComment);

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

        sw_use_client_cookie.setChecked(config.getUseClientCookie());
        sw_use_client_cookie.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                config.setUseClientCookie(true);
                Toast.makeText(context, "使用B站客户端Cookie", Toast.LENGTH_SHORT).show();
            }else{
                config.setUseClientCookie(false);
                Toast.makeText(context, "不使用B站客户端Cookie", Toast.LENGTH_SHORT).show();
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
        if (item.getItemId() == R.id.cookie) {
            accountList();
        }
        return true;
    }

    private void accountList() {
        View dialogView = View.inflate(MainActivity.this, R.layout.dialog_accounts, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_accounts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        AccountAdapter accountAdapter = new AccountAdapter(context);
        recyclerView.setAdapter(accountAdapter);

        accountAdapter.setItemClickListener(account -> {
            View editAccountView = View.inflate(context, R.layout.dialog_edit_account, null);
            EditText editCookie = editAccountView.findViewById(R.id.edit_cookie);
            editCookie.setText(account.cookie);
            currentEditText = editCookie;
            EditText editCommentAreaLocation = editAccountView.findViewById(R.id.edit_comment_area_location);
            editCommentAreaLocation.setText(account.accountCommentArea == null ? "" : account.accountCommentArea.commentAreaLocation);
            AlertDialog editDialog = new AlertDialog.Builder(context)
                    .setTitle("编辑账号▪" + account.uname)
                    .setView(editAccountView)
                    .setPositiveButton("更新", null)
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .setNeutralButton("浏览器获取cookie", null).show();
            editDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (TextUtils.isEmpty(editCookie.getText().toString())) {
                    editCookie.setError("请输入cookie");
                    return;
                }
                ProgressBarDialog pDialog = new ProgressBarDialog.Builder(context)
                        .setTitle("正在获取账号信息……")
                        .setIndeterminate(true)
                        .show();
                CookieGetAccountTask task = new CookieGetAccountTask(new CookieGetAccountTask.EventHandler() {

                    @Override
                    public void onGettingCommentArea() {
                        pDialog.setMessage("正在获取评论区信息……");
                    }

                    @Override
                    public void onCommentAreaNull() {
                        pDialog.dismiss();
                        editCommentAreaLocation.setError("输入的评论区地址未解析到评论区");
                    }

                    @Override
                    public void onOnSuccess(Account newAccount) {
                        if (newAccount == null) {
                            Toast.makeText(context, "你所输入的cookie未解析到用户", Toast.LENGTH_SHORT).show();
                        } else if (newAccount.uid == account.uid) {
                            accountAdapter.updateAccount(account, newAccount);
                            editDialog.dismiss();
                            Toast.makeText(context, "账号已更新", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "你所输入的cookie解析到的用户与原来的不一致！", Toast.LENGTH_SHORT).show();
                        }
                        pDialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable th) {
                        toastLong(th.getMessage());
                    }
                }, editCookie.getText().toString(),editCommentAreaLocation.getText().toString());
                task.execute();
            });
            editDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                startWebLoginActivity();
            });

        });

        accountAdapter.setItemLongClickListener(account -> {
            new AlertDialog.Builder(context)
                    .setTitle("确认删除")
                    .setMessage(String.format("确认删除用户：%s(%s) 吗？", account.uname, account.uid))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        accountAdapter.removeAccount(account);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        });

        AlertDialog listDialog = new AlertDialog.Builder(context)
                .setTitle("账号列表")
                .setView(dialogView)
                .setNegativeButton("添加", null)
                .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener())
                .show();

        listDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            View inputView = View.inflate(context, R.layout.edit_text, null);
            EditText editText = inputView.findViewById(R.id.edit_text);
            editText.setHint("输入cookie");
            currentEditText = editText;
            AlertDialog addDialog = new AlertDialog.Builder(context)
                    .setTitle("添加账号")
                    .setView(inputView)
                    .setOnDismissListener(dialog -> currentEditText = null)
                    .setPositiveButton("确定", null)
                    .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                    .setNeutralButton("网页登录获取", null)
                    .show();
            addDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v12 -> {
                startWebLoginActivity();
            });
            addDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                if (TextUtils.isEmpty(editText.getText().toString())) {
                    editText.setError("请输入cookie");
                    return;
                }
                ProgressBarDialog pDialog = new ProgressBarDialog.Builder(context)
                        .setTitle("正在获取账号信息……")
                        .setIndeterminate(true)
                        .show();
                CookieGetAccountTask task = new CookieGetAccountTask(new CookieGetAccountTask.EventHandler() {

                    @Override
                    public void onGettingCommentArea() {

                    }

                    @Override
                    public void onCommentAreaNull() {

                    }

                    @Override
                    public void onOnSuccess(Account account) {
                        if (account == null) {
                            Toast.makeText(context, "你所输入的cookie未解析到用户", Toast.LENGTH_SHORT).show();
                        } else {
                            if (accountAdapter.addAccount(account)) {
                                Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "账号已存在", Toast.LENGTH_SHORT).show();
                            }
                            addDialog.dismiss();
                        }
                        pDialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable th) {
                        toastLong(th.getMessage());
                    }
                }, editText.getText().toString(),null);
                task.execute();
            });
        });
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

    private void setThisOnclickListener(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            findViewById(viewId).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ll_history_comment) {
            startActivity(new Intent(context, HistoryCommentActivity.class));
        } else if (id == R.id.ll_pending_check_comment_list) {
            startActivity(new Intent(MainActivity.this, PendingCheckCommentsActivity.class));
        } else if (id == R.id.cl_recorde_history_comment_sw) {
            sw_recorde_history.setChecked(!enableRecordeHistoryComment);
        } else if (id == R.id.cl_use_client_cookie){
            sw_use_client_cookie.setChecked(!config.getUseClientCookie());
        } else if (id == R.id.ll_martial_law_comment_area_list) {
            startActivity(new Intent(context, MartialLawCommentAreaListActivity.class));
        } else if (id == R.id.ll_random_test_comment_pool) {
            SetRandomCommentsDialog.show(context);
        } else if (id == R.id.ll_forward_dynamic) {
            SetForwardDynamicDialog.show(context);
        } else if (id == R.id.ll_wait_time) {
            openSetWaitTime();
        } else if (id == R.id.ll_targeting) {
            openTargeting();
        } else if (id == R.id.ll_github_project) {
            Uri uri = Uri.parse("https://github.com/freedom-introvert/biliSendCommAntifraud");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } else if (id == R.id.btn_send) {
            sendAndCheckOrAppealComment(false);
        } else if (id == R.id.btn_send_and_appeal) {
            sendAndCheckOrAppealComment(true);
        } else if (id == R.id.btn_clean) {
            cleanInput();
        }
    }

    private void sendAndCheckOrAppealComment(boolean isAppeal){
        String commentText = edt_comment.getText().toString();
        String commentAreaText = edt_bvid.getText().toString();
        if (TextUtils.isEmpty(commentText)){
            Toast.makeText(context, "请输入评论内容！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(commentAreaText)){
            Toast.makeText(context, "请输入评论区地址！", Toast.LENGTH_SHORT).show();
            return;
        }
        AccountSelectionDialog.show(context, "请选择发送评论的账号", null, account -> {
            if (isAppeal){
                sendAndAppealComment(commentText,commentAreaText,account);
            } else {
                sendAndCheckComment(commentText,commentAreaText,account);
            }
        });
    }

    private void sendAndCheckComment(String commentText,String commentAreaText,Account account){
        sendComment(commentText, commentAreaText, account, new SendCommentHandler(context) {
            @Override
            public void onSent(Comment comment) {
                new DialogCommCheckWorker(context)
                        .checkComment(comment, true, null, dialog -> {});
            }
        });
    }

    private void sendAndAppealComment(String commentText,String commentAreaText,Account account){
        sendComment(commentText, commentAreaText, account, new SendCommentHandler(context) {
            @Override
            public void onSent(Comment comment) {
                AppealCommentDialog.toAppeal(context, comment, new AppealCommentDialog.ResultCallback(context) {
                    @Override
                    public void onSuccess(String successToast) {
                        HistoryComment historyComment = new HistoryComment(comment);
                        insert(HistoryComment.STATE_UNKNOWN);
                        DialogUtil.dialogMessage(context, successToast+"，证明评论被ban，但具体情况未知", successToast);
                        if (config.getRecordeHistoryIsEnable()) {
                            statisticsDBOpenHelper.insertHistoryComment(historyComment);
                        }
                    }

                    @Override
                    public void onNoCommentToAppeal(String successToast) {
                        insert(HistoryComment.STATE_NORMAL);
                        DialogUtil.dialogMessage(context, "评论正常显示！", successToast);
                    }

                    private void insert(String state){
                        HistoryComment historyComment = new HistoryComment(comment);
                        historyComment.setFirstStateAndCurrentState(state);
                        historyComment.lastCheckDate = new Date();
                        if (config.getRecordeHistoryIsEnable()) {
                            statisticsDBOpenHelper.insertHistoryComment(historyComment);
                        }
                    }
                });
            }
        });
    }

    private void sendComment(String commentText,String commentAreaText,Account account,SendCommentHandler handler){
        new SendCommentTask(context, commentText, commentAreaText, account, 0, 0,handler).execute();
    }

    private static abstract class SendCommentHandler implements SendCommentTask.EventHandler{
        ProgressDialog progressDialog;
        StatisticsDBOpenHelper statisticsDBOpenHelper;
        Context context;
        public SendCommentHandler(Context context){
            this.context = context;
            this.statisticsDBOpenHelper = StatisticsDBOpenHelper.getInstance(context);
            progressDialog = DialogUtil.newProgressDialog(context, null, "发送评论中……");
            progressDialog.show();
        }
        @Override
        public void onCommentAreaMoMatch() {
            progressDialog.dismiss();
            Toast.makeText(context, "你输入的评论区地址未解析到评论区！", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSent(CommentArea commentArea,CommentAddResult commentAddResult) {
            progressDialog.dismiss();
            BiliComment reply = commentAddResult.reply;
            Comment comment = new Comment(commentArea, reply.rpid, reply.parent, reply.root,
                    reply.content.message, null, new Date(reply.ctime * 1000), reply.mid);
            statisticsDBOpenHelper.insertPendingCheckComment(comment);
            onSent(comment);
        }

        public abstract void onSent(Comment comment);

        @Override
        public void onCommentContainSensitive(CommentArea commentArea, String commentText,long uid,BiliBiliApiException e) {
            progressDialog.dismiss();
            HistoryComment historyComment = new HistoryComment(new Comment(commentArea,
                    -System.currentTimeMillis(), 0, 0, commentText, null, new Date(),uid));
            historyComment.setFirstStateAndCurrentState(HistoryComment.STATE_SENSITIVE);
            statisticsDBOpenHelper.insertHistoryComment(historyComment);
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(Throwable th) {
            progressDialog.dismiss();
            DialogUtil.dialogError(context,th);
        }
    }

    private void cleanInput() {
        new AlertDialog.Builder(context)
                .setTitle("确认清空输入内容？")
                .setPositiveButton("确定", (dialog, which) -> {
                    edt_bvid.setText("");
                    edt_comment.setText("");
                }).setNegativeButton("手滑了", new VoidDialogInterfaceOnClickListener())
                .show();
    }

    private void openSetWaitTime() {
        View dialogView = View.inflate(context, R.layout.dialog_set_wait_time, null);
        EditText editTextWTByCommentSent = dialogView.findViewById(R.id.edit_text_wt_by_after_comment_sent);
        EditText editTextWTByHasPictures = dialogView.findViewById(R.id.edit_text_wt_by_has_pictures);
        EditText editTextWTByDanmakuSent = dialogView.findViewById(R.id.edit_text_wt_danmaku_sent);
        editTextWTByCommentSent.setText(String.valueOf(config.getWaitTime()));
        editTextWTByHasPictures.setText(String.valueOf(config.getWaitTimeByHasPictures()));
        editTextWTByDanmakuSent.setText(String.valueOf(config.getWaitTimeByDanmakuSend()));
        new AlertDialog.Builder(context).setTitle("设置发评后等待时间（毫秒/ms）")
                .setView(dialogView)
                .setPositiveButton("设置", (dialog, which) -> {
                    long waitTime = Long.parseLong(editTextWTByCommentSent.getText().toString());
                    long waitTimeByHasPictures = Long.parseLong(editTextWTByHasPictures.getText().toString());
                    long waitTimeByDanmakuSent = Long.parseLong(editTextWTByDanmakuSent.getText().toString());
                    config.setWaitTime(waitTime);
                    config.setWaitTimeByHasPictures(waitTimeByHasPictures);
                    config.setWaitTimeByDanmakuSend(waitTimeByDanmakuSent);
                    toastLong("设置成功！");
                })
                .setNegativeButton("取消", new VoidDialogInterfaceOnClickListener())
                .show();
    }

    private void openTargeting() {
        View dialogView = View.inflate(context, R.layout.dialog_targeting_comment, null);
        Spinner spinner = dialogView.findViewById(R.id.spinner_area_type);
        EditText oid = dialogView.findViewById(R.id.edit_oid);
        EditText rpid = dialogView.findViewById(R.id.edit_rpid);
        EditText root = dialogView.findViewById(R.id.edit_root);
        EditText sourceId = dialogView.findViewById(R.id.edit_source_id);
        new AlertDialog.Builder(context)
                .setTitle("定位评论（XPosed）")
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    int type = CommentArea.AREA_TYPE_VIDEO;
                    switch (spinner.getSelectedItemPosition()) {
                        case 1:
                            type = CommentArea.AREA_TYPE_ARTICLE;
                            break;
                        case 2:
                            type = CommentArea.AREA_TYPE_DYNAMIC11;
                            break;
                        case 3:
                            type = CommentArea.AREA_TYPE_DYNAMIC17;
                            break;
                        default:
                            break;
                    }
                    CommentLocator.lunch(context, type,
                            Long.parseLong(oid.getText().toString()),
                            Long.parseLong(rpid.getText().toString()),
                            TextUtils.isEmpty(root.getText().toString()) ?
                                    0 : Long.parseLong(root.getText().toString()),
                            TextUtils.isEmpty(sourceId.getText().toString()) ?
                                    oid.getText().toString() : sourceId.getText().toString());
                })
                .setNegativeButton(R.string.cancel, new VoidDialogInterfaceOnClickListener())
                .show();
    }

    private void startWebLoginActivity() {
        startActivityForResult(new Intent(context, WebViewLoginActivity.class), REQUEST_CODE_GET_COOKIE);
    }

}
