package icu.freedomIntrovert.biliSendCommAntifraud.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BannedCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;

public class StatisticsDBOpenHelper extends SQLiteOpenHelper {
    public static final int VERSION = 5;
    public static final String DB_NAME = "statistics.db";
    public static final String TABLE_NAME_BANNED_COMMENT = "banned_comment";
    public static final String TABLE_NAME_MARTIAL_LAW_AREA = "martial_law_comment_area";
    public static final String TABLE_NAME_HISTORY_COMMENT = "history_comment";

    public StatisticsDBOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME_BANNED_COMMENT + " ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bannedType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL,checkedArea INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_MARTIAL_LAW_AREA + "( oid TEXT PRIMARY KEY NOT NULL UNIQUE, sourceId TEXT NOT NULL, areaType INTEGER NOT NULL, defaultDisposalMethod TEXT NOT NULL, title TEXT,up TEXT NOT NULL,coverImageData BLOB);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_HISTORY_COMMENT + " ( rpid INTEGER PRIMARY KEY UNIQUE NOT NULL, parent INTEGER NOT NULL, root INTEGER NOT NULL, oid INTEGER NOT NULL, area_type INTEGER NOT NULL, source_id TEXT, comment TEXT, [like] INTEGER NOT NULL, reply INTEGER NOT NULL, last_state TEXT, last_check_date INTEGER NOT NULL, date INTEGER NOT NULL );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1: //CREATE TABLE band_comment ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bandType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL );
                db.execSQL("ALTER TABLE band_comment ADD COLUMN checkedArea INTEGER NOT NULL default 0");
            case 2: //"CREATE TABLE band_comment ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bandType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL,checkedArea INTEGER NOT NULL);"
                db.execSQL("ALTER TABLE band_comment RENAME TO banned_comment");
            case 3:// "CREATE TABLE banned_comment ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bandType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL,checkedArea INTEGER NOT NULL);
                db.execSQL("ALTER TABLE banned_comment RENAME COLUMN bandType TO bannedType");
            case 4:
                db.execSQL("CREATE TABLE history_comment ( rpid INTEGER PRIMARY KEY UNIQUE NOT NULL, parent INTEGER NOT NULL, root INTEGER NOT NULL, oid INTEGER NOT NULL, area_type INTEGER NOT NULL, source_id TEXT, comment TEXT, [like] INTEGER NOT NULL, reply INTEGER NOT NULL, last_state TEXT, last_check_date INTEGER NOT NULL, date INTEGER NOT NULL );");
        }
    }

    public long insertBannedComment(BannedCommentBean bandCommentBean) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("rpid", bandCommentBean.rpid);
        values.put("oid", bandCommentBean.commentArea.oid);
        values.put("sourceId", bandCommentBean.commentArea.sourceId);
        values.put("comment", bandCommentBean.comment);
        values.put("bannedType", bandCommentBean.bannedType);
        values.put("commentAreaType", bandCommentBean.commentArea.areaType);
        values.put("checkedArea", bandCommentBean.checkedArea);
        values.put("date", bandCommentBean.getTimeStampDate());
        return db.insert(TABLE_NAME_BANNED_COMMENT, null, values);
    }


    public ArrayList<BannedCommentBean> queryAllBannedComments() {
        ArrayList<BannedCommentBean> bannedCommentBeanArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        System.out.println(db.getVersion());
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME_BANNED_COMMENT, null);
        while (cursor.moveToNext()) {
            CommentArea commentArea = new CommentArea(cursor.getLong(1), cursor.getString(2), cursor.getInt(5));
            bannedCommentBeanArrayList.add(new BannedCommentBean(
                    commentArea,
                    cursor.getLong(0),
                    cursor.getString(3),
                    cursor.getString(4),
                    new Date(cursor.getLong(6)),
                    cursor.getInt(7))
            );
        }
        cursor.close();
        return bannedCommentBeanArrayList;
    }

    public boolean checkBannedCommentIsExists(long rpid){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME_BANNED_COMMENT + " WHERE rpid = ?", new String[]{String.valueOf(rpid)});
        cursor.moveToNext();
        boolean exists = cursor.getInt(0) > 0;
        cursor.close();
        return exists;
    }

    public long insertMartialLawCommentArea(MartialLawCommentArea area) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("oid", area.oid);
        values.put("sourceId", area.sourceId);
        values.put("areaType", area.areaType);
        values.put("defaultDisposalMethod", area.defaultDisposalMethod);
        values.put("title", area.title);
        values.put("up", area.up);
        values.put("coverImageData", area.coverImageData);
        return db.insert(TABLE_NAME_MARTIAL_LAW_AREA, null, values);
    }

    public long deleteMartialLawCommentArea(long oid) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_MARTIAL_LAW_AREA, "oid = ?", new String[]{String.valueOf(oid)});
    }

    public ArrayList<MartialLawCommentArea> queryMartialLawCommentAreas() {
        ArrayList<MartialLawCommentArea> martialLawCommentAreaArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select oid,sourceId,areaType,defaultDisposalMethod,title,up,coverImageData from " + TABLE_NAME_MARTIAL_LAW_AREA, null);
        while (cursor.moveToNext()) {
            martialLawCommentAreaArrayList.add(new MartialLawCommentArea(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    null  //为节约内存，暂不在查询所有评论区时加载图片
            ));
        }
        cursor.close();
        return martialLawCommentAreaArrayList;
    }

    public byte[] selectMartialLawCommentAreaCoverImage(long oid) {
        byte[] imageData = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select coverImageData from " + TABLE_NAME_MARTIAL_LAW_AREA + " where oid = ?", new String[]{String.valueOf(oid)});
        if (cursor.moveToNext()) {
            imageData = cursor.getBlob(0);
        }
        cursor.close();
        return imageData;
    }

    public int updateCheckedArea(String rpid, int areaChecked) {
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("checkedArea", areaChecked);
        return db.update(TABLE_NAME_BANNED_COMMENT, values, "rpid = ?", new String[]{rpid});
    }

    public int updateBannedCommentBannedType(String rpid, String bannedType) {
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("bannedType", bannedType);
        return db.update(TABLE_NAME_BANNED_COMMENT, values, "rpid = ?", new String[]{rpid});
    }

    public Boolean getCommentAreaIsMartialLaw(String oid, String rpid) {
        SQLiteDatabase db = getReadableDatabase();
        Boolean areaIsMartialLaw = null;
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME_MARTIAL_LAW_AREA + " where oid = ?", new String[]{oid});
        if (cursor.moveToNext()) {
            areaIsMartialLaw = true;
        }
        Cursor cursor1 = db.rawQuery("select checkedArea from " + TABLE_NAME_BANNED_COMMENT + " where rpid = ?", new String[]{rpid});
        if (cursor1.moveToNext()) {
            if (cursor1.getInt(0) == BannedCommentBean.CHECKED_NOT_MARTIAL_LAW) {
                areaIsMartialLaw = false;
            }
        }
        cursor.close();
        cursor1.close();
        return areaIsMartialLaw;
    }

    public Boolean getCommentIsOnlyBannedInThisArea(String rpid) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select checkedArea from " + TABLE_NAME_BANNED_COMMENT + " where rpid = ?", new String[]{rpid});
        Boolean commentIsOnlyBannedInThisArea = null;
        if (cursor.moveToNext()) {
            if (cursor.getInt(0) == BannedCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA) {
                commentIsOnlyBannedInThisArea = true;
            } else if (cursor.getInt(0) == BannedCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA) {
                commentIsOnlyBannedInThisArea = false;
            }
        }
        cursor.close();
        return commentIsOnlyBannedInThisArea;
    }


    public int deleteBannedComment(String rpid) {
        SQLiteDatabase db = getReadableDatabase();
        return db.delete(TABLE_NAME_BANNED_COMMENT, "rpid = ?", new String[]{rpid});
    }

    public int deleteBannedComment(long rpid) {
        return deleteBannedComment(String.valueOf(rpid));
    }


    public List<HistoryComment> queryAllHistoryComments() {
        SQLiteDatabase db = getReadableDatabase();
        List<HistoryComment> historyCommentList = new ArrayList<>();
        String[] columns = {
                "rpid", "parent","root","oid", "area_type", "source_id", "comment",
                "like", "reply", "last_state", "last_check_date", "date"
        };
        GreatCursor cursor = new GreatCursor(db.query(TABLE_NAME_HISTORY_COMMENT, columns, null, null, null, null, null));

        while (cursor.moveToNext()) {
            HistoryComment historyComment = new HistoryComment(
                    new CommentArea(cursor.getInt("oid"),cursor.getString("source_id"),cursor.getInt("area_type")),
                    cursor.getLong("rpid"),
                    cursor.getLong("parent"),
                    cursor.getLong("root"),
                            cursor.getString("comment"),
                    new Date(cursor.getLong("date")),
                    cursor.getInt("like"),
                    cursor.getInt("reply"),
                    cursor.getString("last_state"),
                    new Date(cursor.getLong("last_check_date")));
            historyCommentList.add(historyComment);
        }
        cursor.close();
        return historyCommentList;
    }

    public long insertHistoryComment(HistoryComment historyComment) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("rpid", historyComment.rpid);
        cv.put("parent",historyComment.parent);
        cv.put("root",historyComment.root);
        cv.put("oid", historyComment.commentArea.oid);
        cv.put("area_type", historyComment.commentArea.areaType);
        cv.put("source_id", historyComment.commentArea.sourceId);
        cv.put("comment", historyComment.comment);
        cv.put("like", historyComment.like);
        cv.put("reply", historyComment.replyCount);
        cv.put("last_state", historyComment.state);
        cv.put("last_check_date", historyComment.lastCheckDate.getTime());
        cv.put("date", historyComment.date.getTime());
        return db.insert(TABLE_NAME_HISTORY_COMMENT, null, cv);
    }

    public int updateHistoryComment(long rpid, String state, int like, int replyCount, Date last_check_date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_state", state);
        cv.put("like", like);
        cv.put("reply", replyCount);
        cv.put("last_check_date", last_check_date.getTime());
        return db.update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public int deleteHistoryComment(long rpid){
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_HISTORY_COMMENT,"rpid = ?",new String[]{String.valueOf(rpid)});
    }

    public HistoryComment getHistoryComment(long rpid){
        SQLiteDatabase db = getReadableDatabase();
        GreatCursor cursor = new GreatCursor(db.rawQuery("select * from "+TABLE_NAME_HISTORY_COMMENT+" where rpid = ?",new String[]{String.valueOf(rpid)}));
        if (cursor.moveToNext()) {
            HistoryComment historyComment = new HistoryComment(
                    new CommentArea(cursor.getInt("oid"),cursor.getString("source_id"),cursor.getInt("area_type")),
                    cursor.getLong("rpid"),
                    cursor.getLong("parent"),
                    cursor.getLong("root"),
                    cursor.getString("comment"),
                    new Date(cursor.getLong("date")),
                    cursor.getInt("like"),
                    cursor.getInt("reply"),
                    cursor.getString("last_state"),
                    new Date(cursor.getLong("last_check_date")));
            cursor.close();
            return historyComment;
        } else {
            cursor.close();
            return null;
        }
    }
}
