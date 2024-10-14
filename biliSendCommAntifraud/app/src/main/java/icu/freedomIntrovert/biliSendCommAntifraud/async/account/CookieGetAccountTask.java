package icu.freedomIntrovert.biliSendCommAntifraud.async.account;

import android.text.TextUtils;

import icu.freedomIntrovert.async.BackstageTaskByMVP;
import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.account.AccountManger;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.AccountCommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.CommentManipulator;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;

public class CookieGetAccountTask extends BackstageTaskByMVP<CookieGetAccountTask.EventHandler> {

    public String cookie;
    public String commentAreaLocation;

    public CookieGetAccountTask(EventHandler uiHandler, String cookie, String commentAreaLocation) {
        super(uiHandler);
        this.cookie = cookie;
        this.commentAreaLocation = commentAreaLocation;
    }

    @Override
    protected void onStart(EventHandler handler) throws Throwable {
        CommentManipulator commentManipulator = CommentManipulator.getInstance();
        //handler.onGettingAccountInfo();
        Account account = AccountManger.cookieToAccount(cookie);
        if (account == null) {
            handler.onOnSuccess(null);
            return;
        }
        if (!TextUtils.isEmpty(commentAreaLocation)) {
            handler.onGettingCommentArea();
            CommentArea commentArea = commentManipulator.matchCommentArea(commentAreaLocation,account);
            if (commentArea != null) {
                account.accountCommentArea = new AccountCommentArea(commentArea, commentAreaLocation);
            } else {
                handler.onCommentAreaNull();
                return;
            }
        }
        handler.onOnSuccess(account);
    }

    public interface EventHandler extends BaseEventHandler{
        //void onGettingAccountInfo();
        void onGettingCommentArea();
        void onCommentAreaNull();
        void onOnSuccess(Account account);
    }
}
