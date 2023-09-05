package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class HistoryCommentActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    List<HistoryComment> historyCommentList;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    HistoryCommentAdapter historyCommentAdapter;
    RecyclerView recyclerView;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_comment);
        context = this;
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        historyCommentList = statisticsDBOpenHelper.queryAllHistoryComments();
        Collections.reverse(historyCommentList);
        historyCommentAdapter = new HistoryCommentAdapter(context,historyCommentList,statisticsDBOpenHelper);
        recyclerView = findViewById(R.id.rv_history_comment);
        recyclerView.setAdapter(historyCommentAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_export_and_input, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //工具栏返回上一级按钮
        if (item.getItemId() == 16908332) {
            finish();
        } else if (item.getItemId() == R.id.item_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINA);
            intent.putExtra(Intent.EXTRA_TITLE, "历史评论记录_"+sdf.format(new Date())+".csv");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
        } else if (item.getItemId() == R.id.item_import) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/comma-separated-values");
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_EXPORT:
                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("保存文件中");
                progressDialog.setCancelable(false);
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (data != null) {
                            try (OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                                 CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))) {
                                csvWriter.writeNext(HistoryComment.getCSVHeader());
                                for (int i = historyCommentList.size() - 1; i >= 0; i--) {
                                    csvWriter.writeNext(historyCommentList.get(i).toCSVStringArray());
                                }
                                csvWriter.close();
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "保存成功！", Toast.LENGTH_LONG).show();
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                error("保存失败：" + e.getMessage());
                            }
                        } else {
                            error("您取消了保存");
                        }
                    }

                    public void error(String msg) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
                break;
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
                        List<HistoryComment> historyCommentList = new ArrayList<>();
                        if (data != null) {
                            try (InputStreamReader inputStreamReader = new InputStreamReader(context.getContentResolver().openInputStream(data.getData()));
                                 CSVReader csvReader = new CSVReader(inputStreamReader);
                            ) {
                                String[] fields = HistoryComment.getCSVHeader();
                                String[] csvLine;
                                int successCount = 0;
                                int failCount = 0;
                                if ((csvLine = csvReader.readNext()) != null) {
                                    if (Arrays.equals(fields, csvLine)) {
                                        while ((csvLine = csvReader.readNext()) != null) {
                                            HistoryComment historyComments = new HistoryComment(Long.parseLong(csvLine[0]), csvLine[1], Integer.parseInt(csvLine[2]), Long.parseLong(csvLine[3]), Long.parseLong(csvLine[4]), Long.parseLong(csvLine[5]), csvLine[6], new Date(Long.parseLong(csvLine[7])),Integer.parseInt(csvLine[8]),Integer.parseInt(csvLine[9]),csvLine[10],new Date(Long.parseLong(csvLine[11])));
                                            System.out.println(historyComments);
                                            if (statisticsDBOpenHelper.insertHistoryComment(historyComments) > 0) {
                                                historyCommentList.add(historyComments);
                                                successCount++;
                                            } else {
                                                failCount++;
                                            }
                                        }
                                        int finalSuccessCount = successCount;
                                        int finalFailCount = failCount;
                                        runOnUiThread(() -> {
                                            progressDialog1.dismiss();
                                            historyCommentAdapter.addData(historyCommentList);
                                            Toast.makeText(context, "成功导入" + finalSuccessCount + "条数据，失败" + finalFailCount + "条", Toast.LENGTH_LONG).show();
                                        });
                                    } else {
                                        error("CSV字段不匹配！");
                                    }
                                } else {
                                    error("空文件！");
                                }
                            } catch (IOException | CsvValidationException e) {
                                e.printStackTrace();
                                error(e.getMessage());
                            }
                        } else {
                            error("没有选择文件");
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



}