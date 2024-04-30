package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResult;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResultCallbackForSaveDoc;
import icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper.ActivityResultForFile;
import icu.freedomIntrovert.biliSendCommAntifraud.picturestorage.PictureStorage;

public class HistoryCommentActivity extends AppCompatActivity {
    private static final String[] csv_header_after_v500 = new String[]{
            "rpid",
            "parent",
            "root",
            "oid",
            "area_type",
            "source_id",
            "comment",
            "like",
            "reply",
            "last_state",
            "last_check_date",
            "date",
            "checked_area",
            "first_state",
            "pictures",
            "sensitive_scan_result"};

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_comment);
        ByXposedLaunchedActivity.lastActivity = this;
        context = this;
        config = new Config(context);
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        adapter = new HistoryCommentAdapter(this, statisticsDBOpenHelper);
        loadingHistoryCommentFragment = new LoadingHistoryCommentFragment();
        historyCommentFragment = new HistoryCommentFragment(adapter);
        reloadData(null);
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

                List<HistoryComment> historyCommentList = statisticsDBOpenHelper.queryAllHistoryComments(StatisticsDBOpenHelper.ORDER_BY_DATE_ASC);
                ZipEntry csvEntry = new ZipEntry("comments.csv");
                zos.putNextEntry(csvEntry);
                CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zos));
                //写入csv头
                csvWriter.writeNext(csv_header_after_v500);
                for (HistoryComment comment : historyCommentList) {
                    csvWriter.writeNext(new String[]{
                            String.valueOf(comment.rpid),
                            String.valueOf(comment.parent),
                            String.valueOf(comment.root),
                            String.valueOf(comment.commentArea.oid),
                            String.valueOf(comment.commentArea.type),
                            comment.commentArea.sourceId,
                            comment.comment,
                            String.valueOf(comment.like),
                            String.valueOf(comment.replyCount),
                            comment.lastState,
                            String.valueOf(comment.lastCheckDate.getTime()), // Assuming lastCheckDate is stored as milliseconds
                            String.valueOf(comment.date.getTime()), // Assuming date is stored as milliseconds
                            String.valueOf(comment.checkedArea),
                            comment.firstState,
                            comment.pictures,
                            comment.sensitiveScanResult != null ? JSON.toJSONString(comment.sensitiveScanResult) : null
                    });
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
        if (itemId == 16908332) {
            finish();
        } else if (itemId == R.id.item_sort) {
            //这要求sortRuler数字与选项位置一致
            AtomicInteger sortRuler = new AtomicInteger(config.getSortRuler());
            new AlertDialog.Builder(context).setTitle("排序").setIcon(R.drawable.baseline_sort_24).setSingleChoiceItems(new String[]{"发送日期(新-旧)", "发送日期(旧-新)", "点赞数降序", "评论数降序"}, sortRuler.get(), (dialog, which) -> {
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
                            new String[]{"正常", "ShadowBan", "已删除", "其他", "类型：1(视频)", "类型：12(专栏)","类型：11(动态)","类型：17(动态)"},
                            new boolean[]{enableNormal.get(), enableShadowBan.get(), enableDeleted.get(), enableOther.get(),enableType1.get(),enableType12.get(),enableType11.get(),enableType17.get()},
                            (dialog, which, isChecked) -> {
                                if (which == 0) {
                                    enableNormal.set(isChecked);
                                } else if (which == 1) {
                                    enableShadowBan.set(isChecked);
                                } else if (which == 2) {
                                    enableDeleted.set(isChecked);
                                } else if (which == 3) {
                                    enableOther.set(isChecked);
                                } else if (which == 4){
                                    enableType1.set(isChecked);
                                } else if (which == 5){
                                    enableType12.set(isChecked);
                                } else if (which == 6){
                                    enableType11.set(isChecked);
                                } else if (which == 7){
                                    enableType17.set(isChecked);
                                }
                            }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onFilterRulerSet(enableNormal.get(), enableShadowBan.get(), enableDeleted.get(), enableOther.get(),enableType1.get(), enableType12.get(), enableType11.get(), enableType17.get());
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
        }
        return true;
    }

    private void reloadData(String searchText) {
        replaceFragment(loadingHistoryCommentFragment);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sortRuler = null;
                switch (config.getSortRuler()) {
                    case Config.SORT_RULER_DATE_ASC:
                        sortRuler = StatisticsDBOpenHelper.ORDER_BY_DATE_ASC;
                        break;
                    case Config.SORT_RULER_DATE_DESC:
                        sortRuler = StatisticsDBOpenHelper.ORDER_BY_DATE_DESC;
                        break;
                    case Config.SORT_RULER_LIKE_DESC:
                        sortRuler = StatisticsDBOpenHelper.ORDER_BY_LIKE_DESC;
                        break;
                    case Config.SORT_RULER_REPLY_COUNT_DESC:
                        sortRuler = StatisticsDBOpenHelper.ORDER_BY_REPLY_COUNT_DESC;
                        break;
                    default:
                        throw new RuntimeException("sp config error: Unknown sort rule: " + config.getSortRuler());
                }
                List<HistoryComment> historyCommentList = statisticsDBOpenHelper.queryAllHistoryComments(sortRuler);
                // historyCommentList = statisticsDBOpenHelper.getDemoHistoryComments();
                List<HistoryComment> sortedCommentList = new ArrayList<>(historyCommentList.size());
                boolean enableNormal = config.getFilterRulerEnableNormal();
                boolean enableShadowBan = config.getFilterRulerEnableShadowBan();
                boolean enableDelete = config.getFilterRulerEnableDelete();
                boolean enableOther = config.getFilterRulerEnableOther();
                boolean enableType1 = config.getFilterRulerEnableType1();
                boolean enableType12 = config.getFilterRulerEnableType12();
                boolean enableType11 = config.getFilterRulerEnableType11();
                boolean enableType17 = config.getFilterRulerEnableType17();
                if (!TextUtils.isEmpty(searchText) && searchText.startsWith("[date]:")) {
                    Pattern pattern = Pattern.compile("\\[date]:(\\d{4}\\.\\d{2}\\.\\d{2})-(\\d{4}\\.\\d{2}\\.\\d{2})");
                    // Match the pattern against the text
                    Matcher matcher = pattern.matcher(searchText);
                    // Check if the text matches the pattern
                    if (matcher.find()) {
                        String startDateStr = matcher.group(1);
                        String endDateStr = matcher.group(2);

                        try {
                            // Parse start and end dates
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
                            if (startDateStr != null && endDateStr != null) {
                                Date startDate = dateFormat.parse(startDateStr);
                                Date endDate = dateFormat.parse(endDateStr);
                                // Filter comments within the time range
                                historyCommentList = filterCommentsWithinRange(historyCommentList, startDate, endDate);
                            } else {
                                toastInUi("解析日期时出错，");
                            }
                            // Do something with filteredComments
                        } catch (ParseException e) {
                            toastInUi("解析日期时出错");
                        }
                    } else {
                        toastInUi("格式不正确，正确示例：\n[date]:2023.06.04-2023.10.24");
                    }
                }
                boolean continueToSearching = !TextUtils.isEmpty(searchText) && !searchText.startsWith("[date]:");
                for (HistoryComment historyComment : historyCommentList) {
                    if (continueToSearching && !(historyComment.comment.contains(searchText) || historyComment.commentArea.sourceId.contains(searchText))) {
                        continue;
                    }
                    int type = historyComment.commentArea.type;
                    if (type == CommentArea.AREA_TYPE_VIDEO) {
                        if (!enableType1){
                            continue;
                        }
                    } else if (type == CommentArea.AREA_TYPE_ARTICLE) {
                        if (!enableType12){
                            continue;
                        }
                    } else if (type == CommentArea.AREA_TYPE_DYNAMIC11) {
                        if (!enableType11){
                            continue;
                        }
                    } else if (type == CommentArea.AREA_TYPE_DYNAMIC17){
                        if (!enableType17){
                            continue;
                        }
                    }
                    if (historyComment.lastState.equals(HistoryComment.STATE_NORMAL)) {
                        if (enableNormal) {
                            sortedCommentList.add(historyComment);
                        }
                    } else if (historyComment.lastState.equals(HistoryComment.STATE_SHADOW_BAN)) {
                        if (enableShadowBan) {
                            sortedCommentList.add(historyComment);
                        }
                    } else if (historyComment.lastState.equals(HistoryComment.STATE_DELETED)) {
                        if (enableDelete) {
                            sortedCommentList.add(historyComment);
                        }
                    } else if (enableOther) {
                        sortedCommentList.add(historyComment);
                    }
                }


                runOnUiThread(() -> {
                    if (!TextUtils.isEmpty(searchText)) {
                        Toast.makeText(context, "已搜索到 " + sortedCommentList.size() + " 历史评论", Toast.LENGTH_SHORT).show();
                    }
                    adapter.reloadData(sortedCommentList);
                    replaceFragment(historyCommentFragment);
                });
            }
        }).start();
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
                                        readComments = readCSVToHistoryComments(new CSVReader(new InputStreamReader(zis)));
                                    } else if (name.startsWith("pictures/")) {
                                        String[] split = name.split("/");
                                        PictureStorage.save(context, zis, split[split.length - 1]);
                                    }
                                    zis.closeEntry();
                                }
                                zis.close();
                            } else if ("text/comma-separated-values".equals(type)) {
                                readComments = readCSVToHistoryComments(new CSVReader(new InputStreamReader(inputStream)));
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


    private List<HistoryComment> readCSVToHistoryComments(CSVReader csvReader) throws CsvValidationException, IOException {
        String[] header_before_v500 = new String[]{"oid", "sourceId", "type", "rpid", "parent", "root", "comment", "date", "like", "replyCount", "state", "lastCheckDate"};
        String[] header_banned = new String[]{"rpid", "oid", "sourceId", "comment", "bannedType", "commentAreaType", "checkedArea", "date"};
        String[] data;
        if ((data = csvReader.readNext()) == null) {
            return null;
        }

        if (Arrays.equals(header_before_v500, data)) {
            //5.0.0及之前版本到导出的数据导入方式
            return readCSVToHistoryComments_before_v500(csvReader);
        } else if (Arrays.equals(csv_header_after_v500, data)) {
            return readCSVToHistoryComments_after_v500(csvReader);
        } else if (Arrays.equals(header_banned, data)) {
            return readCSVToHistoryComments_banned_to_history(csvReader);
        } else {
            return null;
        }
    }

    private List<HistoryComment> readCSVToHistoryComments_before_v500(CSVReader csvReader) throws CsvValidationException, IOException {
        List<HistoryComment> historyCommentList = new ArrayList<>();
        String[] data;
        while ((data = csvReader.readNext()) != null) {
            HistoryComment historyComment = new HistoryComment(Long.parseLong(data[0]),
                    data[1], Integer.parseInt(data[2]), Long.parseLong(data[3]),
                    Long.parseLong(data[4]), Long.parseLong(data[5]),
                    data[6], new Date(Long.parseLong(data[7])), Integer.parseInt(data[8]),
                    Integer.parseInt(data[9]), data[10], new Date(Long.parseLong(data[11])));
            historyCommentList.add(historyComment);
        }
        return historyCommentList;
    }

    private List<HistoryComment> readCSVToHistoryComments_after_v500(CSVReader csvReader) throws CsvValidationException, IOException {
        List<HistoryComment> historyCommentList = new ArrayList<>();
        String[] data;
        while ((data = csvReader.readNext()) != null) {
            CommentArea commentArea = new CommentArea(Long.parseLong(data[3]), data[5], Integer.parseInt(data[4]));
            HistoryComment comment = new HistoryComment(
                    commentArea,
                    Long.parseLong(data[0]),//rpid
                    Long.parseLong(data[1]),//parent
                    Long.parseLong(data[2]),//root
                    data[6],//comment(text)
                    new Date(Long.parseLong(data[11])), //sentDate, Assuming timestamp in milliseconds
                    Integer.parseInt(data[7]),//like
                    Integer.parseInt(data[8]),//replyCount
                    data[9],//lastState
                    new Date(Long.parseLong(data[10])), //lastCheckDate, Assuming timestamp in milliseconds
                    Integer.parseInt(data[12]),//checkedArea
                    data[13],//firstState
                    data[14],//pictures
                    JSON.parseObject(data[15], SensitiveScanResult.class)//sensitiveScanResult
            );
            historyCommentList.add(comment);
        }
        return historyCommentList;
    }

    private List<HistoryComment> readCSVToHistoryComments_banned_to_history(CSVReader csvReader) throws CsvValidationException, IOException {
        String[] data;
        List<HistoryComment> historyCommentList = new ArrayList<>();
        while ((data = csvReader.readNext()) != null) {
            CommentArea commentArea = new CommentArea(Long.parseLong(data[1]), data[2], Integer.parseInt(data[5]));
            String state = data[4];//bannedType --> state

            HistoryComment comment = new HistoryComment(
                    commentArea,
                    Long.parseLong(data[0]),//rpid
                    0,//parent
                    0,//root
                    data[3],//comment(text)
                    new Date(Long.parseLong(data[7])), //sentDate, Assuming timestamp in milliseconds
                    0,//like
                    0,//replyCount
                    state,//lastState
                    new Date(Long.parseLong(data[7])), //lastCheckDate, Assuming timestamp in milliseconds
                    Integer.parseInt(data[6]),//checkedArea
                    state,//firstState
                    null,//pictures
                    null//sensitiveScanResult
            );
            if (comment.firstState.equals("shadowBanRecking")) {
                comment.firstState = HistoryComment.STATE_NORMAL;
            }
            if (comment.firstState.equals("quickDelete")) {
                comment.firstState = HistoryComment.STATE_DELETED;
            }
            if (comment.lastState.equals("shadowBanRecking")) {
                comment.lastState = HistoryComment.STATE_SHADOW_BAN;
            }
            if (comment.lastState.equals("quickDelete")) {
                comment.lastState = HistoryComment.STATE_DELETED;
            }
            historyCommentList.add(comment);
        }
        return historyCommentList;
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

}