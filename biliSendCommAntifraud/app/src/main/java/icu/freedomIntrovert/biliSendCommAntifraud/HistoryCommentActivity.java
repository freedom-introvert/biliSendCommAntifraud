package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.async.HistoryCommentSearchTask;
import icu.freedomIntrovert.biliSendCommAntifraud.async.commentcheck.ReviewCommentStatusTask;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.HistoryCommentCsvSerializer;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResult;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResultCallbackForSaveDoc;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResultForFile;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;

public class HistoryCommentActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    HistoryCommentAdapter adapter;
    RecyclerView recyclerView;
    Context context;
    Config config;
    private LoadingHistoryCommentFragment loadingHistoryCommentFragment;
    private HistoryCommentFragment historyCommentFragment;
    public ActivityResultLauncher<File> savePicFileLauncher;
    public ActivityResultLauncher<Intent> exportLauncher;
    public CommentManipulator commentManipulator;
    public String search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_comment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        search = getIntent().getStringExtra("search");
        context = this;
        config = Config.getInstance(context);
        commentManipulator = CommentManipulator.getInstance();
        statisticsDBOpenHelper = StatisticsDBOpenHelper.getInstance(context);
        adapter = new HistoryCommentAdapter(this, commentManipulator, statisticsDBOpenHelper);
        loadingHistoryCommentFragment = new LoadingHistoryCommentFragment();
        historyCommentFragment = new HistoryCommentFragment(adapter);
        reloadData(search);
        savePicFileLauncher = registerForActivityResult(new ActivityResultContract<File, ActivityResultForFile>() {
            File inputFile;

            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, File file) {
                inputFile = file;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_TITLE, file.getName());
                return intent;
            }

            @Override
            public ActivityResultForFile parseResult(int i, @Nullable Intent intent) {
                return new ActivityResultForFile(intent, inputFile);
            }
        }, new ActivityResultCallbackForSaveDoc<ActivityResultForFile>(context) {
            @Override
            protected void onOpenOutputStream(OutputStream outputStream, ActivityResultForFile result) throws IOException {
                FileInputStream inputStream = new FileInputStream(result.file);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) > -1) {
                    outputStream.write(buffer, 0, read);
                }
                HistoryCommentActivity.this.toastInUi("保存成功");
                inputStream.close();
                outputStream.close();
            }

            @Override
            protected void onNullOutputStream() {
                toastInUi("保存失败，无法打开输出流");
            }

            @Override
            protected void onIOException(Exception e) {
                toastInUi("保存失败，因为：" + e.getMessage());
            }
        });
        exportLauncher = registerForActivityResult(new ActivityResultContract<Intent, ActivityResult>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Intent intent) {
                return intent;
            }

            @Override
            public ActivityResult parseResult(int i, @Nullable Intent intent) {
                return new ActivityResult(intent);
            }
        }, new ActivityResultCallbackForSaveDoc<ActivityResult>(context) {
            ProgressDialog progressDialog;

            @Override
            protected void onHasResult() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("保存文件中");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected void onOpenOutputStream(OutputStream outputStream, ActivityResult result) throws IOException {
                ZipOutputStream zos = new ZipOutputStream(outputStream);
                byte[] buffer = new byte[1024];

                List<HistoryComment> historyCommentList = statisticsDBOpenHelper.exportAllHistoryComment();
                ZipEntry csvEntry = new ZipEntry("comments.csv");
                zos.putNextEntry(csvEntry);
                CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zos));
                //写入csv头
                csvWriter.writeNext(HistoryCommentCsvSerializer.getLatestHeader());
                for (HistoryComment comment : historyCommentList) {
                    csvWriter.writeNext(HistoryCommentCsvSerializer.toCsvData(comment));
                }

                csvWriter.flush();
                zos.closeEntry();
                File file = PictureStorage.getPicturesDir(context);

                File[] fileList = file.listFiles();
                if (fileList != null) {
                    for (File fileToZip : fileList) {
                        try (FileInputStream fis = new FileInputStream(fileToZip)) {
                            ZipEntry zipEntry = new ZipEntry("pictures/" + fileToZip.getName());
                            zos.putNextEntry(zipEntry);
                            int length;
                            while ((length = fis.read(buffer)) >= 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        }
                    }
                }
                zos.close();
                outputStream.close();
                toastInUi("保存成功！");
                HistoryCommentActivity.this.runOnUiThread((progressDialog::dismiss));
            }

            @Override
            protected void onNullOutputStream() {
                progressDialog.dismiss();
                toastInUi("保存失败，无法打开输出流");
            }

            @Override
            protected void onIOException(Exception e) {
                progressDialog.dismiss();
                toastInUi("保存失败，因为：" + e.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_more_options, menu);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        assert searchView != null;
        if (search != null) {
            searchView.setQuery(search, false);
        }
        searchView.setSubmitButtonEnabled(true);
        menu.findItem(R.id.花里胡哨).setChecked(config.get花里胡哨Enable());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                reloadData(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    reloadData(newText);
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //工具栏返回上一级按钮
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.item_sort) {
            //这要求sortRuler数字与选项位置一致
            AtomicInteger sortRuler = new AtomicInteger(config.getSortRuler());
            new AlertDialog.Builder(context)
                    .setTitle("排序")
                    .setIcon(R.drawable.baseline_sort_24)
                    .setSingleChoiceItems(new String[]{"发送日期(新-旧)", "发送日期(旧-新)", "点赞数降序", "评论数降序"}, sortRuler.get(), (dialog, which) -> {
                        sortRuler.set(which);
                    }).setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (sortRuler.get() == 0) {
                            onSortTypeSet(Config.SORT_RULER_DATE_DESC);
                        } else if (sortRuler.get() == 1) {
                            onSortTypeSet(Config.SORT_RULER_DATE_ASC);
                        } else if (sortRuler.get() == 2) {
                            onSortTypeSet(Config.SORT_RULER_LIKE_DESC);
                        } else if (sortRuler.get() == 3) {
                            onSortTypeSet(Config.SORT_RULER_REPLY_COUNT_DESC);
                        }
                    }).setNegativeButton(android.R.string.cancel, new VoidDialogInterfaceOnClickListener()).show();
        } else if (itemId == R.id.item_filter) {
            AtomicBoolean enableNormal = new AtomicBoolean(config.getFilterRulerEnableNormal());
            AtomicBoolean enableShadowBan = new AtomicBoolean(config.getFilterRulerEnableShadowBan());
            AtomicBoolean enableDeleted = new AtomicBoolean(config.getFilterRulerEnableDelete());
            AtomicBoolean enableOther = new AtomicBoolean(config.getFilterRulerEnableOther());
            AtomicBoolean enableType1 = new AtomicBoolean(config.getFilterRulerEnableType1());
            AtomicBoolean enableType12 = new AtomicBoolean(config.getFilterRulerEnableType12());
            AtomicBoolean enableType11 = new AtomicBoolean(config.getFilterRulerEnableType11());
            AtomicBoolean enableType17 = new AtomicBoolean(config.getFilterRulerEnableType17());
            new AlertDialog.Builder(context)
                    .setTitle("过滤")
                    .setIcon(R.drawable.baseline_filter_alt_24)
                    .setMultiChoiceItems(
                            new String[]{"正常", "ShadowBan", "已删除", "其他", "类型：1(视频)", "类型：12(专栏)", "类型：11(动态)", "类型：17(动态)"},
                            new boolean[]{enableNormal.get(),
                                    enableShadowBan.get(),
                                    enableDeleted.get(),
                                    enableOther.get(),
                                    enableType1.get(),
                                    enableType12.get(),
                                    enableType11.get(),
                                    enableType17.get()},
                            (dialog, which, isChecked) -> {
                                if (which == 0) {
                                    enableNormal.set(isChecked);
                                } else if (which == 1) {
                                    enableShadowBan.set(isChecked);
                                } else if (which == 2) {
                                    enableDeleted.set(isChecked);
                                } else if (which == 3) {
                                    enableOther.set(isChecked);
                                } else if (which == 4) {
                                    enableType1.set(isChecked);
                                } else if (which == 5) {
                                    enableType12.set(isChecked);
                                } else if (which == 6) {
                                    enableType11.set(isChecked);
                                } else if (which == 7) {
                                    enableType17.set(isChecked);
                                }
                            }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onFilterRulerSet(enableNormal.get(),
                                    enableShadowBan.get(),
                                    enableDeleted.get(),
                                    enableOther.get(),
                                    enableType1.get(),
                                    enableType12.get(),
                                    enableType11.get(),
                                    enableType17.get());
                        }
                    }).setNegativeButton(android.R.string.cancel, new VoidDialogInterfaceOnClickListener()).show();
        } else if (itemId == R.id.item_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            intent.putExtra(Intent.EXTRA_TITLE, "历史评论记录_" + sdf.format(new Date()) + ".zip");
            exportLauncher.launch(intent);
        } else if (itemId == R.id.item_import) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/comma-separated-values", "application/zip"});
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        } else if (itemId == R.id.花里胡哨) {
            boolean enable = !item.isChecked();
            config.set花里胡哨Enable(enable);
            item.setChecked(enable);
            adapter.set花里胡哨Enable(enable);
        } else if (itemId == R.id.batch_recheck) {
            View dialogView = View.inflate(context, R.layout.dialog_batch_recheck_start, null);
            EditText editText = dialogView.findViewById(R.id.edit_text);
            Spinner spinner = dialogView.findViewById(R.id.spinner_before_by);
            spinner.setSelection(0);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("批量复查")
                    .setView(dialogView)
                    .setMessage("请注意：设置的过滤与排序不会在此起作用")
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, new VoidDialogInterfaceOnClickListener())
                    .show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (TextUtils.isEmpty(editText.getText().toString())) {
                    editText.setError("请输入数字");
                    return;
                }
                int inputNumber = Integer.parseInt(editText.getText().toString());
                switch (spinner.getSelectedItemPosition()) {
                    case 0:
                        batchCheck(statisticsDBOpenHelper.queryHistoryCommentsByDateGT(
                                getPreviousNDaysTimestamp(inputNumber)));
                        break;
                    case 1:
                        batchCheck(statisticsDBOpenHelper.queryHistoryCommentsByDateGT(
                                System.currentTimeMillis() -
                                        (long) inputNumber * 60 * 60 * 1000));
                        break;
                    case 2:
                        batchCheck(statisticsDBOpenHelper.queryHistoryCommentsCountLimit(inputNumber));
                        break;
                }
                dialog.dismiss();
            });
        } else if (itemId == R.id.statistics) {

            String message = "按照最后状态\n"+generateStat(statisticsDBOpenHelper.countingLastStatus())+
                    "\n按初始状态\n"+generateStat(statisticsDBOpenHelper.countingFirstStatus());


            new AlertDialog.Builder(context)
                    .setTitle("统计")
                    .setPositiveButton("关闭",null)
                    .setNegativeButton("复制", (dialog, which) -> {
                        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData mClipData = ClipData.newPlainText("统计信息",message);
                        cm.setPrimaryClip(mClipData);
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                    })
                    .setMessage(message)
                    .show();
            System.out.println();
        }
        return true;
    }

    private long getPreviousNDaysTimestamp(int day) {
        // 创建 Calendar 对象并设置为当前日期
        Calendar calendar = Calendar.getInstance();
        // 将日期减去一天
        calendar.add(Calendar.DAY_OF_MONTH, -day);
        // 设置时间部分为零点
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // 获取前一天的日期
        Date previousDate = calendar.getTime();
        // 将日期转换为时间戳（以毫秒为单位）
        return previousDate.getTime();
    }

    private void batchCheck(List<HistoryComment> pendingCheckComments) {
        System.out.println(pendingCheckComments);
        if (pendingCheckComments.isEmpty()) {
            Toast.makeText(context, "没有要检查的评论", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = View.inflate(context, R.layout.dialog_batch_check, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_batch_checking_comments);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        BatchCheckAdapter adapter = new BatchCheckAdapter(context);
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(String.format("检查中[%s/%s]……", 0, pendingCheckComments.size()))
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("取消", null)
                .show();

        ReviewCommentStatusTask task = new ReviewCommentStatusTask(context, pendingCheckComments.toArray(new HistoryComment[]{}), null, new ReviewCommentStatusTask.EventHandler() {

            @Override
            public void onCookieFailed(Account account) {
                dialog.dismiss();
                DialogUtil.dialogMessage(context, "错误", String.format("账号：%s(%s)的cookie已失效！", account.uname, account.uid));
            }

            @Override
            public void onNoAccount(long uid) {
                dialog.dismiss();
                DialogUtil.dialogMessage(context, "错误", "没有对应UID：" + uid + " 的账号");
            }

            @Override
            public void onAreaDead(HistoryComment historyComment, int index) {
                adapter.overCheckComment("评论区失效");
                recyclerView.scrollToPosition(adapter.getItemCount());
            }

            @Override
            public void onRootCommentFailed(HistoryComment historyComment, int index) {
                adapter.overCheckComment("根评论失效");
                recyclerView.scrollToPosition(adapter.getItemCount());
            }

            @Override
            public void onStartCheck(HistoryComment checkingComment, int index) {
                adapter.setCheckingComment(checkingComment);
                dialog.setTitle(String.format("检查中[%s/%s]……", adapter.getItemCount(), pendingCheckComments.size()));
            }

            @Override
            public void onCheckResult(HistoryComment historyComment, int index) {
                adapter.overCheckComment(historyComment.lastState);
                recyclerView.scrollToPosition(adapter.getItemCount());
            }

            @Override
            public void onError(Throwable th) {
                dialog.dismiss();
                reloadData(null);
                DialogUtil.dialogError(context, th);
            }

            @Override
            public void onComplete() {
                dialog.setTitle(pendingCheckComments.size() + "条评论已检查完毕");
                reloadData(null);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("关闭");
            }
        });
        task.execute();

        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setOnClickListener(v -> {
            task.breakRun();
            dialog.dismiss();
        });
    }

    public void reloadData(String searchText) {
        replaceFragment(loadingHistoryCommentFragment);
        new HistoryCommentSearchTask(new HistoryCommentSearchTask.EventHandler() {
            @Override
            public void onResult(List<HistoryComment> historyComments) {
                if (!TextUtils.isEmpty(searchText)) {
                    if (!searchText.startsWith("[rpid]:")) {
                        Toast.makeText(context, "已搜索到 " + historyComments.size() + " 历史评论", Toast.LENGTH_SHORT).show();
                    }

                }
                adapter.reloadData(historyComments);
                replaceFragment(historyCommentFragment);
            }

            @Override
            public void onMatchError(String errorMsg) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
            }
        }, searchText, context).execute();
    }


    protected void onFilterRulerSet(boolean enableNormal, boolean enableShadowBan, boolean enableDeleted, boolean enableOther,
                                    boolean e1, boolean e12, boolean e11, boolean e17) {
        config.setFilterRulerEnableNormal(enableNormal);
        config.setFilterRulerEnableShadowBan(enableShadowBan);
        config.setFilterRulerEnableDeleted(enableDeleted);
        config.setFilterRulerEnableOther(enableOther);
        config.setFilterRulerEnableType1(e1);
        config.setFilterRulerEnableType12(e12);
        config.setFilterRulerEnableType11(e11);
        config.setFilterRulerEnableType17(e17);
        reloadData(null);
    }

    protected void onSortTypeSet(int sortRuler) {
        config.setSortRuler(sortRuler);
        reloadData(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CODE_IMPORT:
                ProgressDialog progressDialog1 = new ProgressDialog(context);
                progressDialog1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog1.setMessage("导入数据中");
                progressDialog1.setCancelable(false);
                progressDialog1.show();
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (intent == null || intent.getData() == null) {
                            error("没有选择文件");
                            return;
                        }
                        String type = getContentResolver().getType(intent.getData());
                        try {
                            List<HistoryComment> readComments = null;
                            InputStream inputStream = getContentResolver().openInputStream(intent.getData());
                            if (inputStream == null) {
                                error("保存失败，无法打开输出流");
                                return;
                            }
                            if ("application/zip".equals(type)) {
                                ZipInputStream zis = new ZipInputStream(inputStream);
                                ZipEntry nextEntry;
                                while ((nextEntry = zis.getNextEntry()) != null) {
                                    String name = nextEntry.getName();
                                    System.out.println(name);
                                    if (name.equals("comments.csv")) {
                                        readComments = HistoryCommentCsvSerializer.readCSVToHistoryComments
                                                (new CSVReader(new InputStreamReader(zis)));
                                    } else if (name.startsWith("pictures/")) {
                                        String[] split = name.split("/");
                                        PictureStorage.save(context, zis, split[split.length - 1]);
                                    }
                                }
                                zis.close();
                            } else if ("text/comma-separated-values".equals(type)) {
                                readComments = HistoryCommentCsvSerializer.readCSVToHistoryComments
                                        (new CSVReader(new InputStreamReader(inputStream)));
                                inputStream.close();
                            } else {
                                error("不支持的文件类型");
                                return;
                            }

                            int successCount = 0;
                            int failCount = 0;

                            if (readComments == null) {
                                error("CSV导入失败，字段不匹配或空文件！");
                                return;
                            }

                            List<HistoryComment> newHistoryCommentList = new ArrayList<>();
                            for (HistoryComment comment : readComments) {
                                if (statisticsDBOpenHelper.insertHistoryComment(comment) > 0) {
                                    System.out.println("已添加：" + comment);
                                    newHistoryCommentList.add(comment);
                                    successCount++;
                                } else {
                                    System.out.println("未添加：" + comment);
                                    failCount++;
                                }
                            }

                            int finalSuccessCount = successCount;
                            int finalFailCount = failCount;
                            runOnUiThread(() -> {
                                progressDialog1.dismiss();
                                adapter.addSomeData(newHistoryCommentList);
                                Toast.makeText(context, "成功导入" + finalSuccessCount + "条数据，失败" + finalFailCount + "条", Toast.LENGTH_LONG).show();
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            error("发生IO异常，导入失败，异常：" + e.getMessage());
                        } catch (CsvValidationException e) {
                            error("CsvValidationException：" + e.getMessage());
                        }
                    }

                    public void error(String msg) {
                        runOnUiThread(() -> {
                            progressDialog1.dismiss();
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        });
                    }
                });
        }
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

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame, fragment);
        fragmentTransaction.commit();
    }

    public static List<HistoryComment> filterCommentsWithinRange
            (List<HistoryComment> historyCommentList, Date startDate, Date endDate) {
        List<HistoryComment> filteredComments = new ArrayList<>();
        for (HistoryComment comment : historyCommentList) {
            if (comment.date.after(startDate) && comment.date.before(endDate)) {
                filteredComments.add(comment);
            }
        }
        return filteredComments;
    }

    public void toastInUi(String message) {
        runOnUiThread(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    private String generateStat(Map<String,Integer> map){
        int normal = getOrZero(map, "normal");//正常
        int shadowBan = getOrZero(map, "shadowBan");//仅自己可见
        int deleted = getOrZero(map, "deleted");//已删除
        int underReview = getOrZero(map, "underReview");//疑似审核中
        int invisible = getOrZero(map, "invisible");//invisible
        int sensitive = getOrZero(map, "sensitive");//发送时报敏感
        int suspectedNoProblem = getOrZero(map, "suspectedNoProblem");//疑似正常
        int unknown = getOrZero(map, "unknown");//未知
        int total = normal + shadowBan + deleted + underReview + invisible + sensitive + suspectedNoProblem + unknown;
        if (total == 0) {
            return "统计结果为空或总数为0\n";
        }
        return String.format(Locale.getDefault(), "正常：%d (%.2f%%)\n", normal, (normal * 100.0 / total)) +
                String.format(Locale.getDefault(), "仅自己可见：%d (%.2f%%)\n", shadowBan, (shadowBan * 100.0 / total)) +
                String.format(Locale.getDefault(), "已删除：%d (%.2f%%)\n", deleted, (deleted * 100.0 / total)) +
                String.format(Locale.getDefault(), "疑似审核中：%d (%.2f%%)\n", underReview, (underReview * 100.0 / total)) +
                String.format(Locale.getDefault(), "不可见：%d (%.2f%%)\n", invisible, (invisible * 100.0 / total)) +
                String.format(Locale.getDefault(), "敏感：%d (%.2f%%)\n", sensitive, (sensitive * 100.0 / total)) +
                String.format(Locale.getDefault(), "疑似正常：%d (%.2f%%)\n", suspectedNoProblem, (suspectedNoProblem * 100.0 / total)) +
                String.format(Locale.getDefault(), "未知：%d (%.2f%%)\n", unknown, (unknown * 100.0 / total)) +
                String.format(Locale.getDefault(), "总计：%d\n", total);
    }


    private static int getOrZero(Map<String,Integer> map, String key){
        Integer val = map.get(key);
        if (val == null){
            return 0;
        } else {
            return val;
        }
    }

}