package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class BannedCommentListActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_IMPORT = 1;
    RecyclerView recyclerView;
    Context context;
    StatisticsDBOpenHelper statisticsDBOpenHelper;
    public static final int REQUEST_CODE_EXPORT = 2;
    ArrayList<BannedCommentBean> bandCommentBeanArrayList;
    BannedCommentListAdapter bannedCommentListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band_comment_list);
        context = this;
        statisticsDBOpenHelper = new StatisticsDBOpenHelper(context);
        recyclerView = findViewById(R.id.rv_band_comment);
        /*
        ArrayList<BannedCommentBean> bandCommentBeanArrayList = new ArrayList<>();
        bandCommentBeanArrayList.add(new BannedCommentBean("00000","BV1U8411t73A","这是一条评论",BannedCommentBean.BAND_TYPE_SHADOW_BAN,BannedCommentBean.AREA_VIDEO,new Date()));
        bandCommentBeanArrayList.add(new BannedCommentBean("00000","BV1U8411t73A","这是一条评论",BannedCommentBean.BAND_TYPE_QUICK_DELETE,BannedCommentBean.AREA_VIDEO,new Date()));
        bandCommentBeanArrayList.add(new BannedCommentBean("00000","BV1U8411t73A","这是一条评论",BannedCommentBean.BAND_TYPE_SENSITIVE,BannedCommentBean.AREA_VIDEO,new Date()));
        */
        bandCommentBeanArrayList = statisticsDBOpenHelper.queryAllBannedComments();
        bannedCommentListAdapter = new BannedCommentListAdapter(bandCommentBeanArrayList, context);
        recyclerView.setAdapter(bannedCommentListAdapter);
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
            intent.putExtra(Intent.EXTRA_TITLE, "被ban评论列表.csv");
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
                                List<String[]> csvData = new ArrayList<>();
                                String[] csvHeader = new String[]{"rpid", "oid", "sourceId", "comment", "bannedType", "commentAreaType", "checkedArea", "date"};
                                csvData.add(csvHeader);
                                Collections.reverse(bandCommentBeanArrayList);
                                for (BannedCommentBean commentBean : bandCommentBeanArrayList) {
                                    csvData.add(commentBean.toCSVStringArray());
                                }
                                Collections.reverse(bandCommentBeanArrayList);
                                csvWriter.writeAll(csvData);
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
                        List<BannedCommentBean> bannedCommentBeans = new ArrayList<>();
                        if (data != null) {
                            try (InputStreamReader inputStreamReader = new InputStreamReader(context.getContentResolver().openInputStream(data.getData()));
                                 CSVReader csvReader = new CSVReader(inputStreamReader);
                            ) {
                                String[] fileds = new String[]{"rpid", "oid", "sourceId", "comment", "bannedType", "commentAreaType", "checkedArea", "date"};
                                String[] csvLine;
                                int successCount = 0;
                                int failCount = 0;
                                if ((csvLine = csvReader.readNext()) != null) {
                                    if (Arrays.equals(fileds, csvLine)) {
                                        while ((csvLine = csvReader.readNext()) != null) {
                                            BannedCommentBean bannedCommentBean = new BannedCommentBean(csvLine[0], Long.parseLong(csvLine[1]), csvLine[2], csvLine[3], csvLine[4], csvLine[5], csvLine[6], csvLine[7]);
                                            System.out.println(bannedCommentBean);
                                            if (statisticsDBOpenHelper.insertBannedComment(bannedCommentBean) > 0){
                                                bannedCommentBeans.add(bannedCommentBean);
                                                successCount++;
                                            } else {
                                                failCount++;
                                            }
                                        }
                                        int finalSuccessCount = successCount;
                                        int finalFailCount = failCount;
                                        runOnUiThread(() -> {
                                            progressDialog1.dismiss();
                                            bannedCommentListAdapter.addData(bannedCommentBeans);
                                            Toast.makeText(context, "成功导入" + finalSuccessCount + "条数据，失败"+ finalFailCount +"条", Toast.LENGTH_LONG).show();
                                        });
                                    } else {
                                        error("CSV字段不匹配！");
                                    }
                                } else {
                                    error("空文件！");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                error(e.getMessage());
                            } catch (CsvValidationException e) {
                                e.printStackTrace();
                                error(e.getMessage());
                            }
                        } else {
                            error("没有选择文件");
                        }
                    }
                    public void error (String msg) {
                        runOnUiThread(() -> {
                            progressDialog1.dismiss();
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        });
                    }
                });



            }

    }



}