package icu.freedomIntrovert.biliSendCommAntifraud;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryCommentActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_comment);
        recyclerView = findViewById(R.id.rv_history_comment);

    }





}