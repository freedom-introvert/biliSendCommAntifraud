package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;

public class DialogUtil {
    public static void dialogMessage(Context context, String title, String message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener());
        if (title != null) {
            dialogBuilder.setTitle(title);
        }
        dialogBuilder.show();
    }

    public static ProgressDialog newProgressDialog(Context context,String title,String message){
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        if (title != null) {
            progressDialog.setTitle(title);
        }
        return progressDialog;
    }
}
