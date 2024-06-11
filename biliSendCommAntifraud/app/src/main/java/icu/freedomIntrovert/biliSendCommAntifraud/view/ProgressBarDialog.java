package icu.freedomIntrovert.biliSendCommAntifraud.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import icu.freedomIntrovert.biliSendCommAntifraud.R;

public class ProgressBarDialog implements DialogInterface {
    public static final int DEFAULT_MAX_PROGRESS = 1000;
    public final AlertDialog alertDialog;
    final ProgressBar progressBar;

    ProgressBarDialog(AlertDialog alertDialog, ProgressBar progressBar) {
        this.alertDialog = alertDialog;
        this.progressBar = progressBar;
    }

    public void setProgress(int progress){
        progressBar.setProgress(progress);
    }

    public void setMax(int max){
        progressBar.setMax(max);
    }

    public void setIndeterminate(boolean indeterminate){
        progressBar.setIndeterminate(indeterminate);
    }

    public void setMessage(String message){
        alertDialog.setMessage(message);
    }

    public void setTitle(String title){
        alertDialog.setTitle(title);
    }

    public Button getButton(int whichButton){
        return alertDialog.getButton(whichButton);
    }

    @Override
    public void cancel() {
        alertDialog.cancel();
    }

    @Override
    public void dismiss() {
        alertDialog.dismiss();
    }

    public static class Builder {

        private final AlertDialog.Builder dialogBuilder;
        private final ProgressBar progressBar;


        public Builder(Context context) {
            dialogBuilder = new AlertDialog.Builder(context);
            View view = View.inflate(context, R.layout.dialog_wait_progress,null);
            progressBar = view.findViewById(R.id.wait_progress_bar);
            dialogBuilder.setView(view);
            progressBar.setMax(DEFAULT_MAX_PROGRESS);
        }

        public Builder setTitle(String title) {
            dialogBuilder.setTitle(title);
            return this;
        }

        public Builder setMessage(String message) {
            dialogBuilder.setMessage(message);
            return this;
        }

        public Builder setPositiveButton(String text, DialogInterface.OnClickListener listener) {
            dialogBuilder.setPositiveButton(text, listener);
            return this;
        }

        public Builder setNegativeButton(String text, DialogInterface.OnClickListener listener) {
            dialogBuilder.setNegativeButton(text, listener);
            return this;
        }

        public Builder setNeutralButton (String text,DialogInterface.OnClickListener listener){
            dialogBuilder.setNeutralButton(text, listener);
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            dialogBuilder.setCancelable(cancelable);
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener) {
            dialogBuilder.setOnCancelListener(listener);
            return this;
        }

        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener) {
            dialogBuilder.setOnDismissListener(listener);
            return this;
        }

        public Builder setProgress(int progress){
            progressBar.setProgress(progress);
            return this;
        }

        public Builder setMax(int max){
            progressBar.setMax(max);
            return this;
        }

        public Builder setIndeterminate(boolean indeterminate){
            progressBar.setIndeterminate(indeterminate);
            return this;
        }

        public ProgressBarDialog show() {
            return new ProgressBarDialog(dialogBuilder.show(),progressBar);
        }
    }


}
