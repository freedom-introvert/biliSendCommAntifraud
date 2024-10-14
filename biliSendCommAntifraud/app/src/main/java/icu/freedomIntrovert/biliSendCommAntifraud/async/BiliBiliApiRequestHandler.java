package icu.freedomIntrovert.biliSendCommAntifraud.async;

import android.content.DialogInterface;

import java.io.IOException;

import icu.freedomIntrovert.async.EventHandler;

public abstract class BiliBiliApiRequestHandler extends EventHandler {

    ErrorHandle errorHandle;

    public BiliBiliApiRequestHandler(ErrorHandle errorHandle) {
        this.errorHandle = errorHandle;
    }

    public void setErrorHandle(ErrorHandle errorHandle) {
        this.errorHandle = errorHandle;
    }

    @Override
    public void handleError(Throwable th) {
        th.printStackTrace();
        if (errorHandle != null) {
            if (th instanceof CookieFailedException) {
                errorHandle.handleCookieFiledException((CookieFailedException) th);
            } else if (th instanceof BiliBiliApiException) {
                errorHandle.handleBiliBiliApiException((BiliBiliApiException) th);
            } else if (th instanceof IOException) {
                errorHandle.handleNetIOException((IOException) th);
            } else {
                errorHandle.handleOtherExceptions(th);
            }
        }
    }


    public interface ErrorHandle {
        //发生网络错误，若有发送测试评论的行为请留意删除
        void handleNetIOException(IOException e);
        void handleCookieFiledException(CookieFailedException e);
        void handleBiliBiliApiException(BiliBiliApiException e);
        void handleOtherExceptions(Throwable th);
    }

    public static class DialogErrorHandle implements ErrorHandle{
        DialogInterface toDismissDialog;
        public interface OnDialogMessageListener{
            void dialogMessage(String title,String message);
        }

        public OnDialogMessageListener listener;

        public DialogErrorHandle(DialogInterface toDismissDialog, OnDialogMessageListener listener) {
            this.toDismissDialog = toDismissDialog;
            this.listener = listener;
        }

        @Override
        public void handleNetIOException(IOException e) {
            toDismissDialog.dismiss();
            listener.dialogMessage("网络异常",e.getMessage());
        }

        @Override
        public void handleCookieFiledException(CookieFailedException e) {
            toDismissDialog.dismiss();
            listener.dialogMessage("登陆失效", "cookie已失效，请重新登陆获取cookie！");
        }

        @Override
        public void handleBiliBiliApiException(BiliBiliApiException e) {
            toDismissDialog.dismiss();
            String msg;
            if (e.tipsMessage != null){
                msg = String.format("Tips:%s\ncode:%s\nmessage:%s",e.tipsMessage,e.code,e.message);
            } else {
                msg = String.format("code:%s\nmessage:%s",e.code,e.message);
            }
            listener.dialogMessage("API调用错误", msg);
        }

        @Override
        public void handleOtherExceptions(Throwable th) {
            toDismissDialog.dismiss();
            listener.dialogMessage("无法处理该错误", th.toString());
        }
    }
}
