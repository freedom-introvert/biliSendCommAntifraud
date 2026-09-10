package icu.freedomIntrovert.biliSendCommAntifraud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import icu.freedomIntrovert.biliSendCommAntifraud.db.StatisticsDBOpenHelper;

public class PendingCheckCommentsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PendingCommentListAdapter pendingCommentListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_check_comments);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        pendingCommentListAdapter = new PendingCommentListAdapter(this);
        recyclerView.setAdapter(pendingCommentListAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.delete_all_pending_check_comments) {
            showDeleteAllComment();
        }
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showDeleteAllComment() {
        new AlertDialog.Builder(this)
                .setTitle("确认删除吗？")
                .setMessage("这会删除所有待检查评论")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Toast.makeText(this, "已删除" + StatisticsDBOpenHelper
                            .getInstance(this)
                            .deleteAllPendingCheckComment() +
                            "条评论", Toast.LENGTH_SHORT).show();

                    pendingCommentListAdapter.clearAll();
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pending_check_comments, menu);
        return super.onCreateOptionsMenu(menu);
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