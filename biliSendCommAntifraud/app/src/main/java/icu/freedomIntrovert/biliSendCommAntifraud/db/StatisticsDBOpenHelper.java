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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.Comment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.CommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.HistoryComment;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.MartialLawCommentArea;
import icu.freedomIntrovert.biliSendCommAntifraud.comment.bean.SensitiveScanResult;

public class StatisticsDBOpenHelper extends SQLiteOpenHelper {
    private static StatisticsDBOpenHelper instance;
    public static final int VERSION = 11;
    public static final String ORDER_BY_DATE_DESC = "date DESC";
    public static final String ORDER_BY_DATE_ASC = "date ASC";
    public static final String ORDER_BY_LIKE_DESC = "like DESC";
    public static final String ORDER_BY_REPLY_COUNT_DESC = "reply DESC";
    public static final String DB_NAME = "statistics.db";
    //public static final String TABLE_NAME_BANNED_COMMENT = "banned_comment";
    public static final String TABLE_NAME_MARTIAL_LAW_AREA = "martial_law_comment_area";
    public static final String TABLE_NAME_HISTORY_COMMENT = "history_comment";

    long count = 0;

    private StatisticsDBOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    public synchronized static StatisticsDBOpenHelper getInstance(Context context) {
        if (instance == null){
            instance = new StatisticsDBOpenHelper(context.getApplicationContext());
        }
        return instance;
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
                "    sensitive_scan_result TEXT,\n" +
                "    uid                   INTEGER NOT NULL\n" +
                "                                  DEFAULT 0,\n" +
                "    appeal_state          INTEGER NOT NULL DEFAULT 0\n" + // 新增字段
                ");");
        db.execSQL("CREATE TABLE pending_check_comments (\n" +
                "    rpid           INTEGER PRIMARY KEY,\n" +
                "    parent         INTEGER NOT NULL,\n" +
                "    root           INTEGER NOT NULL,\n" +
                "    comment        TEXT    NOT NULL,\n" +
                "    pictures       TEXT,\n" +
                "    date           INTEGER NOT NULL,\n" +
                "    area_oid       INTEGER NOT NULL,\n" +
                "    area_source_id TEXT    NOT NULL,\n" +
                "    area_type      INTEGER NOT NULL,\n" +
                "    uid            INTEGER NOT NULL\n" +
                "                           DEFAULT 0\n" +
                ");");
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
            case 9:
                db.execSQL("ALTER TABLE history_comment ADD COLUMN uid INTEGER NOT NULL DEFAULT 0;");
                db.execSQL("DELETE FROM pending_check_comments");//由于未记录UID，旧版还未检查的评论将被移除！
                db.execSQL("ALTER TABLE pending_check_comments ADD COLUMN uid INTEGER NOT NULL DEFAULT 0;");
            case 10:
                db.execSQL("ALTER TABLE history_comment ADD COLUMN appeal_state INTEGER NOT NULL DEFAULT 0;");
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
    public List<HistoryComment> queryAllHistoryComments(String[][] unequalFields,String dateOrderBy) {
        StringBuilder sb = new StringBuilder();
        if (unequalFields != null && unequalFields.length >= 1) {
            //WHERE a != 0,b != 0
            sb.append("WHERE ");
            int length = unequalFields.length;
            for (int i = 0; i < length - 1; i++) {
                sb.append(unequalFields[i][0]).append(" != \"").append(unequalFields[i][1]).append("\" AND ");
            }
            sb.append(unequalFields[length - 1][0]).append(" != \"").append(unequalFields[length - 1][1]).append("\" ");
        }
        sb.append("ORDER BY ").append(dateOrderBy);
        return selectHistoryComments(sb.toString());
    }

    public List<HistoryComment> exportAllHistoryComment(){
        return selectHistoryComments("ORDER BY "+ORDER_BY_DATE_ASC);
    }

    public List<HistoryComment> queryHistoryCommentsByDateGT(long timestamp) {
        return selectHistoryComments("WHERE date > " + timestamp + " ORDER BY date DESC");
    }


    public List<HistoryComment> queryHistoryCommentsCountLimit(int limit) {
        return selectHistoryComments("ORDER BY date DESC LIMIT " + limit);
    }

    public HistoryComment getHistoryComment(long rpid) {
        SQLiteDatabase db = getReadableDatabase();
        GreatCursor cursor = new GreatCursor(db.rawQuery("select * from " + TABLE_NAME_HISTORY_COMMENT + " where rpid = ?", new String[]{String.valueOf(rpid)}));
        if (cursor.moveToNext()) {
            HistoryComment historyComment = loadHistoryComment(cursor);
            cursor.close();
            return historyComment;
        } else {
            cursor.close();
            return null;
        }
    }

    private List<HistoryComment> selectHistoryComments(String selectAddition) {
        SQLiteDatabase db = getReadableDatabase();
        List<HistoryComment> historyCommentList = new ArrayList<>();
        String sql = "select * from " + TABLE_NAME_HISTORY_COMMENT + " " + selectAddition;
        System.out.println(sql);
        GreatCursor cursor = new GreatCursor(db.rawQuery(sql, null));
        while (cursor.moveToNext()) {
            historyCommentList.add(loadHistoryComment(cursor));
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
        cv.put("uid", historyComment.uid);
        if (historyComment.sensitiveScanResult != null) {
            cv.put("sensitive_scan_result", JSON.toJSONString(historyComment.sensitiveScanResult));
        }
        cv.put("appeal_state",historyComment.appealState);
        return db.insert(TABLE_NAME_HISTORY_COMMENT, null, cv);
    }

    public int updateHistoryCommentLastState(long rpid, String state) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_state", state);
        cv.put("last_check_date", System.currentTimeMillis());
        return db.update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public int updateHistoryCommentStates(long rpid, String lastState, int like, int replyCount, Date lastCheckDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("last_state", lastState);
        cv.put("like", like);
        cv.put("reply", replyCount);
        cv.put("last_check_date", lastCheckDate.getTime());
        return db.update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public int deleteHistoryComment(long rpid) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NAME_HISTORY_COMMENT, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    private HistoryComment loadHistoryComment(GreatCursor cursor) {
        return new HistoryComment(
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
                JSON.parseObject(cursor.getString("sensitive_scan_result"), SensitiveScanResult.class),
                cursor.getLong("uid"),
                cursor.getInt("appeal_state"));
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
        values.put("uid",comment.uid);
        long newRowId = getWritableDatabase().insert("pending_check_comments", null, values);
        Log.d("INSERT", "New Row ID: " + newRowId);
    }

    public List<Comment> getAllPendingCheckComments() {
        String query = "SELECT * FROM pending_check_comments ORDER BY date DESC";

        Cursor cursor = getReadableDatabase().rawQuery(query, null);

        List<Comment> commentList = new ArrayList<>();
        while (cursor.moveToNext()) {
            commentList.add(loadPendingCheckComment(cursor));
        }
        cursor.close();
        return commentList;
    }


    public Comment getPendingCheckCommentByRpid(long rpid) {
        String query = "SELECT * FROM pending_check_comments WHERE rpid = ?";
        Cursor cursor = getReadableDatabase().rawQuery(query, new String[]{String.valueOf(rpid)});

        Comment comment = null;
        if (cursor.moveToFirst()) {
            comment = loadPendingCheckComment(cursor);
        }
        cursor.close();
        return comment;
    }

    private Comment loadPendingCheckComment(Cursor cursor){
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
        long uid = cursor.getLong(cursor.getColumnIndexOrThrow("uid"));
        CommentArea area = new CommentArea(areaOid, areaSourceId, areaType);
        return new Comment(area, rpid, parent, root, commentText, pictures, date, uid);
    }

    public void deletePendingCheckComment(long rpid) {
        getWritableDatabase().delete("pending_check_comments", "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public void addSensitiveScanResultToHistoryComment(long rpid, SensitiveScanResult result) {
        ContentValues cv = new ContentValues();
        cv.put("sensitive_scan_result", JSON.toJSONString(result));
        getWritableDatabase().update(TABLE_NAME_HISTORY_COMMENT, cv, "rpid = ?", new String[]{String.valueOf(rpid)});
    }

    public void updateCommentAreaAppealState(int type,long oid,Integer state){
        ContentValues cv = new ContentValues();
        cv.put("appeal_state",state);
        getWritableDatabase().update(TABLE_NAME_HISTORY_COMMENT,cv,"area_type = ? AND oid = ?",new String[]{String.valueOf(type),String.valueOf(oid)});
    }

    public Map<String, Integer> countingLastStatus() {
        Map<String, Integer> map = new HashMap<>();
        Cursor cursor = getReadableDatabase()
                .rawQuery("select last_state,count(last_state) AS count from history_comment group by last_state order by count desc;",
                        null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(0), cursor.getInt(1));
        }
        cursor.close();
        return map;
    }

    public Map<String, Integer> countingFirstStatus() {
        Map<String, Integer> map = new HashMap<>();
        Cursor cursor = getReadableDatabase()
                .rawQuery("select last_state,count(last_state) AS count from history_comment group by first_state order by count desc;",
                        null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(0), cursor.getInt(1));
        }
        cursor.close();
        return map;
    }

}
