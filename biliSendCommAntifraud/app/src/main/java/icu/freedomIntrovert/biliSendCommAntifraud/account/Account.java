package icu.freedomIntrovert.biliSendCommAntifraud.account;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.AccountCommentArea;

public class Account {
    public long uid;
    public String uname;
    public String cookie;
    public byte[] avatarData;
    public Bitmap avatarBitmap;
    public AccountCommentArea accountCommentArea;
    //public Long deputyUid;

    public Account() {
    }

    public Account(long uid, String uname, String cookie,byte[] avatarData,AccountCommentArea commentArea) {
        this.uid = uid;
        this.uname = uname;
        this.cookie = cookie;
        //this.deputyUid = deputyUid;
        this.avatarData = avatarData;
        this.accountCommentArea = commentArea;
        setAvatar(avatarData);
    }


    public void setAvatar(byte[] avatarData){
        this.avatarData = avatarData;
        this.avatarBitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length);
    }


    //更新
    public void update(Account account){
        assert account.uid == this.uid;
        this.uname = account.uname;
        this.cookie = account.cookie;
        this.avatarData = account.avatarData;
        this.avatarBitmap = account.avatarBitmap;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s(%s)",uname,uid);
    }
}
