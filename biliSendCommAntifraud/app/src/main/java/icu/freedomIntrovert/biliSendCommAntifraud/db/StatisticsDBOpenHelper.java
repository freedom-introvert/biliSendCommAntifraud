package icu.freedomIntrovert.biliSendCommAntifraud.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;

public class StatisticsDBOpenHelper extends SQLiteOpenHelper {
    public static final int VERSION = 9;

    public static final String ORDER_BY_DESC = "DESC";
    public static final String ORDER_BY_ASC = "ASC";
    public static final String DB_NAME = "statistics.db";
    //public static final String TABLE_NAME_BANNED_COMMENT = "banned_comment";
    public static final String TABLE_NAME_MARTIAL_LAW_AREA = "martial_law_comment_area";
    public static final String TABLE_NAME_HISTORY_COMMENT = "history_comment";

    long count = 0;

    public StatisticsDBOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //      db.execSQL("CREATE TABLE " + TABLE_NAME_BANNED_COMMENT + " ( rpid TEXT NOT NULL PRIMARY KEY,oid TEXT NOT NULL, sourceId TEXT NOT NULL, comment TEXT, bannedType TEXT NOT NULL, commentAreaType INTEGER NOT NULL, date INTEGER NOT NULL,checkedArea INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_MARTIAL_LAW_AREA + "( oid TEXT PRIMARY KEY NOT NULL UNIQUE, sourceId TEXT NOT NULL, areaType INTEGER NOT NULL, defaultDisposalMethod TEXT NOT NULL, title TEXT,up TEXT NOT NULL,coverImageData BLOB);");
        db.execSQL("CREATE TABLE history_comment (\n" +
                "    rpid                  INTEGER PRIMARY KEY\n" +
                "                                  UNIQUE\n" +
                "                                  NOT NULL,\n" +
                "    parent                INTEGER NOT NULL,\n" +
                "    root                  INTEGER NOT NULL,\n" +
                "    oid                   INTEGER NOT NULL,\n" +
                "    area_type             INTEGER NOT NULL,\n" +
                "    source_id             TEXT,\n" +
                "    comment               TEXT,\n" +
                "    [like]                INTEGER NOT NULL,\n" +
                "    reply                 INTEGER NOT NULL,\n" +
                "    last_state            TEXT,\n" +
                "    last_check_date       INTEGER NOT NULL,\n" +
                "    date                  INTEGER NOT NULL,\n" +
                "    checked_area          INTEGER NOT NULL\n" +
                "                                  DEFAULT 0,\n" +
                "    first_state           TEXT,\n" +
                "    pictures              TEXT,\n" +
                "    sensitive_scan_result TEXT\n" +
                ")");
        db.execSQL("CREATE TABLE pending_check_comments (\n" +
                "    rpid INTEGER PRIMARY KEY,\n" +
                "    parent INTEGER NOT NULL,\n" +
                "    root INTEGER NOT NULL,\n" +
                "    comment TEXT NOT NULL,\n" +
                "    pictures TEXT,\n" +
                "    date INTEGER NOT NULL,\n" +
                "    area_oid INTEGER NOT NULL,\n" +
                "    area_source_id TEXT NOT NULL,\n" +
                "    area_type INTEGER NOT NULL\n" +
                ");\n");
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
            case 5:
                db.execSQL("UPDATE banned_comment SET rpid = REPLACE(rpid, 'st', '-') WHERE rpid LIKE 'st%'");
            case 6:// old:       db.execSQL("CREATE TABLE " + TABLE_NAME_HISTORY_COMMENT + " ( rpid INTEGER PRIMARY KEY UNIQUE NOT NULL, parent INTEGER NOT NULL, root INTEGER NOT NULL, oid INTEGER NOT NULL, area_type INTEGER NOT NULL, source_id TEXT, comment TEXT, [like] INTEGER NOT NULL, reply INTEGER NOT NULL, last_state TEXT, last_check_date INTEGER NOT NULL, date INTEGER NOT NULL );");
                db.execSQL("ALTER TABLE history_comment ADD COLUMN checked_area INTEGER NOT NULL DEFAULT 0;");
                db.execSQL("ALTER TABLE history_comment ADD COLUMN first_state TEXT;");
                db.execSQL("ALTER TABLE history_comment ADD COLUMN pictures TEXT;");
                db.execSQL("INSERT\n" +
                        "OR IGNORE INTO history_comment (\n" +
                        "  rpid, parent, root, oid, area_type,\n" +
                        "  source_id, comment, [like], reply,\n" +
                        "  last_state, last_check_date, date,\n" +
                        "  checked_area\n" +
                        ")\n" +
                        "SELECT\n" +
                        "  bc.rpid,\n" +
                        "  0 AS parent,\n" +
                        "  0 AS root,\n" +
                        "  bc.oid,\n" +
                        "  bc.commentAreaType AS area_type,\n" +
                        "  bc.sourceId AS source_id,\n" +
                        "  bc.comment,\n" +
                        "  0 AS [like],\n" +
                        "  0 AS reply,\n" +
                        "  bc.bannedType AS last_state,\n" +
                        "  bc.date AS last_check_date,\n" +
                        "  bc.date,\n" +
                        "  bc.checkedArea AS checked_area\n" +
                        "FROM\n" +
                        "  banned_comment bc;");
                db.execSQL("UPDATE history_comment\n" +
                        "SET first_state = (SELECT bannedType FROM banned_comment WHERE history_comment.rpid = banned_comment.rpid),\n" +
                        "    checked_area = (SELECT checkedArea FROM banned_comment WHERE history_comment.rpid = banned_comment.rpid)\n" +
                        "WHERE EXISTS (SELECT 1 FROM banned_comment WHERE history_comment.rpid = banned_comment.rpid);\n");
                db.execSQL("UPDATE history_comment SET first_state = 'normal' WHERE first_state = 'shadowBanRecking';");
                db.execSQL("UPDATE history_comment SET first_state = 'deleted' WHERE first_state = 'quickDelete';");
                db.execSQL("UPDATE history_comment SET last_state = 'shadowBan' WHERE last_state = 'shadowBanRecking';");
                db.execSQL("UPDATE history_comment SET last_state = 'deleted' WHERE last_state = 'quickDelete';");
            case 7:
                db.execSQL("CREATE TABLE pending_check_comments (\n" +
                        "    rpid INTEGER PRIMARY KEY,\n" +
                        "    parent INTEGER NOT NULL,\n" +
                        "    root INTEGER NOT NULL,\n" +
                        "    comment TEXT NOT NULL,\n" +
                        "    pictures TEXT,\n" +
                        "    date INTEGER NOT NULL,\n" +
                        "    area_oid INTEGER NOT NULL,\n" +
                        "    area_source_id TEXT NOT NULL,\n" +
                        "    area_type INTEGER NOT NULL\n" +
                        ");\n");
            case 8:
                db.execSQL("ALTER TABLE history_comment ADD COLUMN sensitive_scan_result TEXT;");
        }
    }


    public long insertMartialLawCommentArea(MartialLawCommentArea area) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("oid", area.oid);
        values.put("sourceId", area.sourceId);
        values.put("areaType", area.type);
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

    public int updateCheckedArea(long rpid, int areaChecked) {
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("checked_area", areaChecked);
        return db.update(TABLE_NAME_HISTORY_COMMENT, values, "rpid = ?", new String[]{String.valueOf(rpid)});
    }


    public List<HistoryComment> getDemoHistoryComments(){
        List<HistoryComment> historyCommentList = new ArrayList<>();
        CommentArea commentArea = new CommentArea(1,"BV1GJ411x7h7",CommentArea.AREA_TYPE_VIDEO);
        historyCommentList.add(new HistoryComment(commentArea,count,0,0,"普通且正常的评论",new Date(count),0,0,HistoryComment.STATE_NORMAL,new Date(count),HistoryComment.CHECKED_NO_CHECK,
                HistoryComment.STATE_NORMAL,null,null));
        count++;
        historyCommentList.add(new HistoryComment(commentArea,count,114,114,"回复别人的评论",new Date(count),0,0,HistoryComment.STATE_NORMAL,new Date(count),HistoryComment.CHECKED_NO_CHECK,
                HistoryComment.STATE_NORMAL,null,null));
        count++;
        historyCommentList.add(new HistoryComment(commentArea,count,0,0,"带图片的评论",new Date(count),0,0,HistoryComment.STATE_NORMAL,new Date(count),HistoryComment.CHECKED_NO_CHECK,
                HistoryComment.STATE_NORMAL,"[{\"img_height\":800,\"img_size\":114,\"img_src\":\"https://album.biliimg.com/bfs/new_dyn/404.jpg\",\"img_width\":800}]",null));
        count++;
        historyCommentList.add(newDemoComment(HistoryComment.STATE_SHADOW_BAN));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_DELETED));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_SENSITIVE));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_INVISIBLE));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_UNDER_REVIEW));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_SUSPECTED_NO_PROBLEM));
        historyCommentList.add(newDemoComment(HistoryComment.STATE_UNKNOWN));
        return historyCommentList;
    }

    private HistoryComment newDemoComment(String state){
        CommentArea commentArea = new CommentArea(1,"BV1GJ411x7h7",CommentArea.AREA_TYPE_VIDEO);
        HistoryComment historyComment = new HistoryComment(commentArea,count,0,0, "网络上重拳出击，现实中唯唯诺诺",new Date(count),0,0,state,new Date(count),HistoryComment.CHECKED_NO_CHECK,
                state,null,null);
        count++;
        return historyComment;
    }

    public List<HistoryComment> queryAllHistoryComments(String dateOrderBy) {
        SQLiteDatabase db = getReadableDatabase();
        List<HistoryComment> historyCommentList = new ArrayList<>();
        GreatCursor cursor = new GreatCursor(db.rawQuery("select * from " + TABLE_NAME_HISTORY_COMMENT + " ORDER BY date " + dateOrderBy, null));
        while (cursor.moveToNext()) {
            //System.out.println(cursor.getLong("root"));
            HistoryComment historyComment = new HistoryComment(
                    new CommentArea(cursor.getLong("oid"), cursor.getString("source_id"), cursor.getInt("area_type")),
                    cursor.getLong("rpid"),
                    cursor.getLong("parent"),
                    cursor.getLong("root"),
                    cursor.getString("comment"),
                    new Date(cursor.getLong("date")),
                    cursor.getInt("like"),
                    cursor.getInt("reply"),
                    cursor.getString("last_state"),
                    new Date(cursor.getLong("last_check_date")),
                    cursor.getInt("checked_area"),
                    cursor.getString("first_state"),
                    cursor.getString("pictures"),
                    JSON.parseObject(cursor.getString("sensitive_scan_result"),SensitiveScanResult.class));
            //System.out.println(historyComment.root);
            historyCommentList.add(historyComment);
        }
        cursor.close();
        return historyCommentList;
    }

    public long insertHistoryComment(HistoryComment historyComment) {
        //插入历史评论时说明该rpid的评论已经完成检查，删除待检查评论
        deletePendingCheckComment(historyComment.rpid);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("rpid", historyComment.rpid);
        cv.put("parent", historyComment.parent);
        cv.put("root", historyComment.root);
        cv.put("oid", historyComment.commentArea.oid);
        cv.put("area_type", historyComment.commentArea.type);
        cv.put("source_id", historyComment.commentArea.sourceId);
        cv.put("comment", historyComment.comment);
        cv.put("like", historyComment.like);
        cv.put("reply", historyComment.replyCount);
        cv.put("last_state", historyComment.lastState);
        cv.put("last_check_date", historyComment.lastCheckDate.getTime());
        cv.put("date", historyComment.date.getTime());
        cv.put("checked_area", historyComment.checkedArea);
        cv.put("first_state", historyComment.firstState);
        cv.put("pictures", historyComment.pictures);
        if (historyComment.sensitiveScanResult != null){
            cv.put("sensitive_scan_result",JSON.toJSONString(historyComment.sensitiveScanResult));
        }
        return db.insert(TABLE_NAME_HISTORY_COMMENT, null, cv);
    }

    public int updateHistoryCommentLastState(long rpid, String state) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_state", state);
        cv.put("last_check_date", System.currentTimeMillis());
        return db.update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public int updateHistoryCommentStates(long rpid, String state, int like, int replyCount, Date last_check_date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_state", state);
        cv.put("like", like);
        cv.put("reply", replyCount);
        cv.put("last_check_date", last_check_date.getTime());
        return db.update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public int deleteHistoryComment(long rpid) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_HISTORY_COMMENT, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public HistoryComment getHistoryComment(long rpid) {
        SQLiteDatabase db = getReadableDatabase();
        GreatCursor cursor = new GreatCursor(db.rawQuery("select * from " + TABLE_NAME_HISTORY_COMMENT + " where rpid = ?", new String[]{String.valueOf(rpid)}));
        if (cursor.moveToNext()) {
            HistoryComment historyComment = new HistoryComment(
                    new CommentArea(cursor.getLong("oid"), cursor.getString("source_id"), cursor.getInt("area_type")),
                    cursor.getLong("rpid"),
                    cursor.getLong("parent"),
                    cursor.getLong("root"),
                    cursor.getString("comment"),
                    new Date(cursor.getLong("date")),
                    cursor.getInt("like"),
                    cursor.getInt("reply"),
                    cursor.getString("last_state"),
                    new Date(cursor.getLong("last_check_date")),
                    cursor.getInt("checked_area"),
                    cursor.getString("first_state"),
                    cursor.getString("pictures"),
                    JSON.parseObject(cursor.getString("sensitive_scan_result"),SensitiveScanResult.class));
            cursor.close();
            return historyComment;
        } else {
            cursor.close();
            return null;
        }
    }

    public void insertPendingCheckComment(Comment comment) {
        ContentValues values = new ContentValues();
        values.put("rpid", comment.rpid);
        values.put("parent", comment.parent);
        values.put("root", comment.root);
        values.put("comment", comment.comment);
        values.put("pictures", comment.pictures);
        values.put("date", comment.date.getTime()); // 存储时间戳
        values.put("area_oid", comment.commentArea.oid);
        values.put("area_source_id", comment.commentArea.sourceId);
        values.put("area_type", comment.commentArea.type);

        long newRowId = getWritableDatabase().insert("pending_check_comments", null, values);
        Log.d("INSERT", "New Row ID: " + newRowId);
    }

    public List<Comment> getAllPendingCheckComments() {
        String[] projection = {
                "rpid",
                "parent",
                "root",
                "comment",
                "pictures",
                "date",
                "area_oid",
                "area_source_id",
                "area_type"
        };

        Cursor cursor = getReadableDatabase().query(
                "pending_check_comments",
                projection,
                null,
                null,
                null,
                null,
                null
        );

        List<Comment> commentList = new ArrayList<>();
        while (cursor.moveToNext()) {
            long rpid = cursor.getLong(cursor.getColumnIndexOrThrow("rpid"));
            long parent = cursor.getLong(cursor.getColumnIndexOrThrow("parent"));
            long root = cursor.getLong(cursor.getColumnIndexOrThrow("root"));
            String commentText = cursor.getString(cursor.getColumnIndexOrThrow("comment"));
            String pictures = cursor.getString(cursor.getColumnIndexOrThrow("pictures"));
            long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
            Date date = new Date(dateMillis);
            long areaOid = cursor.getLong(cursor.getColumnIndexOrThrow("area_oid"));
            String areaSourceId = cursor.getString(cursor.getColumnIndexOrThrow("area_source_id"));
            int areaType = cursor.getInt(cursor.getColumnIndexOrThrow("area_type"));

            CommentArea area = new CommentArea(areaOid, areaSourceId, areaType);
            Comment comment = new Comment(area, rpid, parent, root, commentText, pictures, date);
            commentList.add(comment);
        }
        cursor.close();
        return commentList;
    }

    public Comment getPendingCheckCommentByRpid(long rpid) {
        String[] projection = {
                "parent",
                "root",
                "comment",
                "pictures",
                "date",
                "area_oid",
                "area_source_id",
                "area_type"
        };

        String selection = "rpid = ?";
        String[] selectionArgs = { String.valueOf(rpid) };

        Cursor cursor = getReadableDatabase().query(
                "pending_check_comments",
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        Comment comment = null;
        if (cursor.moveToFirst()) {
            long parent = cursor.getLong(cursor.getColumnIndexOrThrow("parent"));
            long root = cursor.getLong(cursor.getColumnIndexOrThrow("root"));
            String commentText = cursor.getString(cursor.getColumnIndexOrThrow("comment"));
            String pictures = cursor.getString(cursor.getColumnIndexOrThrow("pictures"));
            long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
            Date date = new Date(dateMillis);
            long areaOid = cursor.getLong(cursor.getColumnIndexOrThrow("area_oid"));
            String areaSourceId = cursor.getString(cursor.getColumnIndexOrThrow("area_source_id"));
            int areaType = cursor.getInt(cursor.getColumnIndexOrThrow("area_type"));
            CommentArea area = new CommentArea(areaOid, areaSourceId, areaType);
            comment = new Comment(area, rpid, parent, root, commentText, pictures, date);
        }
        cursor.close();
        return comment;
    }

    public void deletePendingCheckComment(long rpid) {
        getWritableDatabase().delete("pending_check_comments", "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public void addSensitiveScanResultToHistoryComment(long rpid, SensitiveScanResult result){
        ContentValues cv = new ContentValues();
        cv.put("sensitive_scan_result", JSON.toJSONString(result));
        getWritableDatabase().update(TABLE_NAME_HISTORY_COMMENT,cv,"rpid = ?",new String[]{String.valueOf(rpid)});
    }
}
