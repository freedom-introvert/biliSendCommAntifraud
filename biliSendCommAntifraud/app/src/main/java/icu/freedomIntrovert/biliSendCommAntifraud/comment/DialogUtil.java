package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.app.AlertDialog;
import android.content.Context;

import icu.freedomIntrovert.biliSendCommAntifraud.VoidDialogInterfaceOnClickListener;

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
}
