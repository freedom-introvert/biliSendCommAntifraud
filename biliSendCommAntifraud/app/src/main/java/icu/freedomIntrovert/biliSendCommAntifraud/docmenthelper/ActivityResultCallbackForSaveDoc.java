package icu.freedomIntrovert.biliSendCommAntifraud.docmenthelper;

import android.content.Context;
import android.net.Uri;

import androidx.activity.result.ActivityResultCallback;

import java.io.IOException;
import java.io.OutputStream;

import icu.freedomIntrovert.async.TaskManger;

public abstract class ActivityResultCallbackForSaveDoc<T extends ActivityResult> implements ActivityResultCallback<T> {
    public ActivityResultCallbackForSaveDoc(Context context) {
        this.context = context;
    }

    Context context;
    @Override
    public void onActivityResult(T result) {
        if (result.intent == null) {
            onVoidResult();
            return;
        }
        Uri data = result.intent.getData();
        if (data == null) {
            onVoidResult();
            return;
        }
        onHasResult();
        TaskManger.start(() -> {
            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(data);
                if (outputStream != null) {
                    onOpenOutputStream(outputStream,result);
                } else {
                    onNullOutputStream();
                }
            } catch (IOException e) {
                onIOException(e);
            }
        });
        
    }
    protected void onHasResult(){};
    protected abstract void onOpenOutputStream(OutputStream outputStream,T result) throws IOException;
    protected abstract void onNullOutputStream();
    protected void onVoidResult(){};
    protected abstract void onIOException(Exception e);
}
