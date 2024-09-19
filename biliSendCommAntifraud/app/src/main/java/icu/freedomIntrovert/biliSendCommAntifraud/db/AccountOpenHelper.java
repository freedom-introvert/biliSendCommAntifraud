package icu.freedomIntrovert.biliSendCommAntifraud.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.account.Account;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.AccountCommentArea;

public class AccountOpenHelper extends SQLiteOpenHelper  {
    public AccountOpenHelper(@Nullable Context context) {
        super(context, "account.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (\n" +
                "    uid           INTEGER PRIMARY KEY,\n" +
                "    name          TEXT    NOT NULL,\n" +
                "    cookie        TEXT    NOT NULL,\n" +
                "    avatar_data   BLOB,\n" +
                "    cma_oid       INTEGER,\n" +
                "    cma_type      INTEGER,\n" +
                "    cma_source_id TEXT,\n" +
                "    cma_location  TEXT\n" +
                ");\n");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertOrUpdateAccount(Account account) {
        SQLiteDatabase db = getWritableDatabase();

        // Insert or update the account
        if (account.accountCommentArea != null) {
            String sql = "INSERT OR REPLACE INTO users (uid, name, cookie,avatar_data,cma_oid,cma_type,cma_source_id,cma_location) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            db.execSQL(sql, new Object[]{
                    account.uid,
                    account.uname,
                    account.cookie,
                    account.avatarData,
                    account.accountCommentArea.oid,
                    account.accountCommentArea.type,
                    account.accountCommentArea.sourceId,
                    account.accountCommentArea.commentAreaLocation
            });
        } else {
            String sql = "INSERT OR REPLACE INTO users (uid, name, cookie,avatar_data) " +
                    "VALUES (?, ?, ?, ?)";
            db.execSQL(sql, new Object[]{
                    account.uid, account.uname, account.cookie, account.avatarData});
        }
        db.close();
    }

    @SuppressLint("Range")
    public List<Account> getAllAccounts() {
        List<Account> accounts = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users", null);

        if (cursor.moveToFirst()) {
            do {
                //public Account(long uid, String uname, String cookie, long deputyUid,byte[] avatarData)
                long uid = cursor.getLong(cursor.getColumnIndexOrThrow("uid"));
                String uname = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String cookie = cursor.getString(cursor.getColumnIndexOrThrow("cookie"));
                byte[] avatarData = cursor.getBlob(cursor.getColumnIndexOrThrow("avatar_data"));
                long cmaOid = cursor.getLong(cursor.getColumnIndexOrThrow("cma_oid"));
                int cmaType = cursor.getInt(cursor.getColumnIndexOrThrow("cma_type"));
                String cmaSourceId = cursor.getString(cursor.getColumnIndexOrThrow("cma_source_id"));
                String cmaLocation = cursor.getString(cursor.getColumnIndexOrThrow("cma_location"));
                AccountCommentArea accountCommentArea = null;
                if (cmaOid != 0){
                    accountCommentArea = new AccountCommentArea(cmaOid, cmaSourceId, cmaType, cmaLocation);
                }
                Account account = new Account(uid, uname, cookie, avatarData, accountCommentArea);
                accounts.add(account);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return accounts;
    }


    public void deleteAccount(Account account) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("users", "uid = ?", new String[]{String.valueOf(account.uid)});
        db.close();
    }
}
