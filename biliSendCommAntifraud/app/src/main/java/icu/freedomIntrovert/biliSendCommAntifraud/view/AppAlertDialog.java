package icu.freedomIntrovert.biliSendCommAntifraud.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

/**
 * 统一弹窗工具：横屏（pad）模式下，所有弹窗自动整体上移
 * （顶边对齐屏幕顶部、高度保持内容自适应），竖屏不处理。
 */
public final class AppAlertDialog {

    private AppAlertDialog() {
    }

    /**
     * 对弹窗窗口应用横屏上移调整；非横屏不做任何处理。
     */
    public static void applyTopAlignInLandscape(Dialog dialog) {
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        if (dialog.getContext().getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
        attributes.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        window.setAttributes(attributes);
    }

    /**
     * 项目通用弹窗构建器：show() 后自动应用横屏上移调整。
     */
    public static class Builder extends AlertDialog.Builder {

        public Builder(Context context) {
            super(context);
        }

        public Builder(Context context, int themeResId) {
            super(context, themeResId);
        }

        @Override
        public AlertDialog show() {
            AlertDialog dialog = super.show();
            applyTopAlignInLandscape(dialog);
            return dialog;
        }
    }
}
