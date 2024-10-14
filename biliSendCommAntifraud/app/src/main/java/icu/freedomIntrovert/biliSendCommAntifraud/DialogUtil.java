package icu.freedomIntrovert.biliSendCommAntifraud;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.IOException;

import icu.freedomIntrovert.biliSendCommAntifraud.async.BiliBiliApiException;

public class DialogUtil {
    public static Dialog dialogMessage(Context context, String title, String message) {
        return dialogMessage(context, title, message, null);
    }

    public static Dialog dialogMessage(Context context, String title, String message, DialogInterface.OnDismissListener onClose) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                .setMessage(message);
        dialogBuilder.setPositiveButton("关闭", null);
        if (onClose != null){
            dialogBuilder.setOnDismissListener(onClose);
        }
        if (title != null) {
            dialogBuilder.setTitle(title);
        }
        return dialogBuilder.show();
    }

    public static void dialogError(Context context, Throwable th) {
        dialogError(context,th,null);
    }

    public static void dialogError(Context context, Throwable th, DialogInterface.OnDismissListener listener) {
        if (th instanceof BiliBiliApiException) {
            dialogMessage(context, "API错误", th.getMessage(),listener);
        } else if (th instanceof IOException) {
            dialogMessage(context, "网络错误", th.toString(),listener);
        } else {
            dialogMessage(context, "发生错误", th.toString(),listener);
        }
    }

    public static ProgressDialog newProgressDialog(Context context, String title, String message) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        if (title != null) {
            progressDialog.setTitle(title);
        }
        return progressDialog;
    }
}
