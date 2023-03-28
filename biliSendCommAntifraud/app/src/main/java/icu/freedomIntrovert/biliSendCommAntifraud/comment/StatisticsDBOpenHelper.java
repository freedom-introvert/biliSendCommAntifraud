package icu.freedomIntrovert.biliSendCommAntifraud.comment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.BandCommentBean;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;

public class StatisticsDBOpenHelper extends SQLiteOpenHelper {
    public static final int VERSION = 2;
    public static final String DB_NAME = "statistics.db";
    public static final String TABLE_NAME_BAND_COMMENT = "band_comment";
    public static final String TABLE_NAME_MARTIAL_LAW_AREA = "martial_law_comment_area";

    public StatisticsDBOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME_BAND_COMMENT + " ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bandType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL,checkedArea INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_MARTIAL_LAW_AREA + "( oid TEXT PRIMARY KEY NOT NULL UNIQUE, sourceId TEXT NOT NULL, areaType INTEGER NOT NULL, defaultDisposalMethod TEXT NOT NULL, title TEXT,up TEXT NOT NULL,coverImageData BLOB);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion){
            case 1 : //CREATE TABLE " + TABLE_NAME_BAND_COMMENT + " ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bandType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL );
                db.execSQL("ALTER TABLE " + TABLE_NAME_BAND_COMMENT + " ADD COLUMN checkedArea INTEGER NOT NULL default 0");
            case 2 :
        }
    }

    public long insertBannedComment(BandCommentBean bandCommentBean) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("rpid", bandCommentBean.rpid);
        values.put("oid", bandCommentBean.commentArea.oid);
        values.put("sourceId", bandCommentBean.commentArea.sourceId);
        values.put("comment", bandCommentBean.comment);
        values.put("bandType", bandCommentBean.bannedType);
        values.put("commentAreaType", bandCommentBean.commentArea.areaType);
        values.put("checkedArea",bandCommentBean.checkedArea);
        values.put("date", bandCommentBean.getTimeStampDate());
        return db.insert(TABLE_NAME_BAND_COMMENT, null, values);
    }

    public long deleteBandComment(String rpid) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_BAND_COMMENT, "rpid = ?", new String[]{rpid});
    }

    public ArrayList<BandCommentBean> queryAllBandComments() {
        ArrayList<BandCommentBean> bandCommentBeanArrayList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        System.out.println(db.getVersion());
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME_BAND_COMMENT, null);
        while (cursor.moveToNext()) {
            CommentArea commentArea = new CommentArea(cursor.getString(1), cursor.getString(2), cursor.getInt(5));
            bandCommentBeanArrayList.add(new BandCommentBean(
                    commentArea,
                    cursor.getString(0),
                    cursor.getString(3),
                    cursor.getString(4),
                    new Date(cursor.getLong(6)),
                    cursor.getInt(7))
            );
        }
        cursor.close();
        return bandCommentBeanArrayList;
    }

    public long insertMartialLawCommentArea(MartialLawCommentArea area) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("oid", area.oid);
        values.put("sourceId", area.sourceId);
        values.put("areaType", area.areaType);
        values.put("defaultDisposalMethod", area.defaultDisposalMethod);
        values.put("title", area.title);
        values.put("up",area.up);
        values.put("coverImageData", area.coverImageData);
        return db.insert(TABLE_NAME_MARTIAL_LAW_AREA, null, values);
    }

    public long deleteMartialLawCommentArea(String oid){
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_MARTIAL_LAW_AREA, "oid = ?", new String[]{oid});
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

    public byte[] selectMartialLawCommentAreaCoverImage(String oid){
        byte[] imageData = null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select coverImageData from " + TABLE_NAME_MARTIAL_LAW_AREA + " where oid = ?", new String[]{oid});
        if (cursor.moveToNext()){
            imageData = cursor.getBlob(0);
        }
        cursor.close();
        return imageData;
    }

    public int updateCheckedArea(String rpid, int areaChecked) {
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("checkedArea",areaChecked);
        return db.update(TABLE_NAME_BAND_COMMENT,values,"rpid = ?",new String[]{rpid});
    }

    public Boolean getCommentAreaIsMartialLaw(String oid,String rpid){
        SQLiteDatabase db = getReadableDatabase();
        Boolean areaIsMartialLaw = null;
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME_MARTIAL_LAW_AREA + " where oid = ?", new String[]{oid});
        if (cursor.moveToNext()){
            areaIsMartialLaw = true;
        }
        Cursor cursor1 = db.rawQuery("select checkedArea from " + TABLE_NAME_BAND_COMMENT + " where rpid = ?",new String[]{rpid});
        if(cursor1.moveToNext()){
            if (cursor1.getInt(0) == BandCommentBean.CHECKED_NOT_MARTIAL_LAW){
                areaIsMartialLaw = false;
            }
        }
        cursor.close();
        cursor1.close();
        return areaIsMartialLaw;
    }

    public Boolean getCommentIsOnlyBannedInThisArea(String rpid){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select checkedArea from " + TABLE_NAME_BAND_COMMENT + " where rpid = ?",new String[]{rpid});
        Boolean commentIsOnlyBannedInThisArea = null;
        if(cursor.moveToNext()){
            if (cursor.getInt(0) == BandCommentBean.CHECKED_ONLY_BANNED_IN_THIS_AREA) {
                commentIsOnlyBannedInThisArea = true;
            } else if (cursor.getInt(0) == BandCommentBean.CHECKED_NOT_ONLY_BANNED_IN_THIS_AREA){
                commentIsOnlyBannedInThisArea = false;
            }
        }
        cursor.close();
        return commentIsOnlyBannedInThisArea;
    }
    

}
