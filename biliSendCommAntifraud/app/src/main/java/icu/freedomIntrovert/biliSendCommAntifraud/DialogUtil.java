package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtil {
    public static Dialog dialogMessage(Context context, String title, String message) {
        return dialogMessage(context,title,message,null);
    }

    public static Dialog dialogMessage(Context context, String title, String message, DialogInterface.OnClickListener onClose){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                .setMessage(message);
        if (onClose == null){
            dialogBuilder.setPositiveButton("关闭", new VoidDialogInterfaceOnClickListener());
        } else {
            dialogBuilder.setPositiveButton("关闭",onClose);
        }

        if (title != null) {
            dialogBuilder.setTitle(title);
        }
        return dialogBuilder.show();
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
