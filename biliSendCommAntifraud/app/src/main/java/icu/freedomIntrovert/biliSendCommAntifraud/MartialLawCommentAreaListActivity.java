package icu.freedomIntrovert.biliSendCommAntifraud;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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

import icu.freedomIntrovert.biliSendCommAntifraud.comment.StatisticsDBOpenHelper;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;

public class MartialLawCommentAreaListActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXPORT = 2;
    private static final int REQUEST_CODE_IMPORT = 1;
    RecyclerView recyclerView;
    StatisticsDBOpenHelper dbOpenHelper;
    Context context;
    ArrayList<MartialLawCommentArea> martialLawCommentAreaArrayList;
    MartialLawCommentAreaListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_martial_law_comment_area_list);
        context = this;
        dbOpenHelper = new StatisticsDBOpenHelper(context);
        martialLawCommentAreaArrayList = dbOpenHelper.queryMartialLawCommentAreas();
        recyclerView = findViewById(R.id.rv_martial_law_comment_area);
        adapter = new MartialLawCommentAreaListAdapter(martialLawCommentAreaArrayList, context);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_EXPORT:
                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("保存文件中");
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (data != null) {
                            try (OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                                 CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))) {
                                List<String[]> csvData = new ArrayList<>();
                                String[] csvHeader = new String[]{"oid", "sourceId", "areaType", "defaultDisposalMethod", "title", "up", "coverImageData"};
                                csvData.add(csvHeader);
                                Collections.reverse(martialLawCommentAreaArrayList);
                                for (MartialLawCommentArea area : martialLawCommentAreaArrayList) {
                                    byte[] imageData = dbOpenHelper.selectMartialLawCommentAreaCoverImage(area.oid);
                                    String[] csvRow = area.toStringArrays();
                                    if (imageData != null) {
                                        csvRow[6] = Base64.encodeToString(imageData, Base64.DEFAULT);
                                    }
                                    csvData.add(csvRow);
                                }
                                Collections.reverse(martialLawCommentAreaArrayList);
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
                        List<MartialLawCommentArea> martialLawCommentAreas = new ArrayList<>();
                        if (data != null) {
                            try (InputStreamReader inputStreamReader = new InputStreamReader(context.getContentResolver().openInputStream(data.getData()));
                                 CSVReader csvReader = new CSVReader(inputStreamReader);
                            ) {
                                String[] fileds = new String[]{"oid", "sourceId", "areaType", "defaultDisposalMethod", "title", "up", "coverImageData"};
                                String[] csvLine;
                                int successCount = 0;
                                int failCount = 0;
                                if ((csvLine = csvReader.readNext()) != null) {
                                    if (Arrays.equals(fileds, csvLine)) {
                                        while ((csvLine = csvReader.readNext()) != null) {
                                            byte[] coverImageData = Base64.decode(csvLine[6],Base64.DEFAULT);
                                            MartialLawCommentArea martialLawCommentArea = new MartialLawCommentArea(csvLine[0], csvLine[1], Integer.parseInt(csvLine[2]), csvLine[3], csvLine[4], csvLine[5], coverImageData);
                                            System.out.println(martialLawCommentArea);
                                            if (dbOpenHelper.insertMartialLawCommentArea(martialLawCommentArea) > 0){
                                                martialLawCommentAreas.add(martialLawCommentArea);
                                                successCount++;
                                            } else {
                                                failCount++;
                                            }

                                        }
                                        int finalSuccessCount = successCount;
                                        int finalFailCount = failCount;
                                        runOnUiThread(() -> {
                                            progressDialog1.dismiss();
                                            adapter.addData(martialLawCommentAreas);
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
            intent.putExtra(Intent.EXTRA_TITLE, "戒严评论区列表.csv");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
        } else if (item.getItemId() == R.id.item_import) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/comma-separated-values");
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        }
        return true;
    }


}
