package icu.freedomIntrovert.biliSendCommAntifraud.db;

import android.database.Cursor;

public class GreatCursor {
    Cursor cursor;

    public GreatCursor(Cursor cursor) {
        this.cursor = cursor;
    }


    public int getInt(String columnName){
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= -1){
            return cursor.getInt(columnIndex);
        } else {
            throw new RuntimeException("column name:"+columnName+" not exists!");
        }
    }

    public long getLong(String columnName){
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= -1){
            return cursor.getLong(columnIndex);
        } else {
            throw new RuntimeException("column name:"+columnName+" not exists!");
        }
    }

    public String getString(String columnName){
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= -1){
            return cursor.getString(columnIndex);
        } else {
            throw new RuntimeException("column name:"+columnName+" not exists!");
        }
    }

    public boolean moveToNext(){
        return cursor.moveToNext();
    }

    public void close(){
        cursor.close();
    }
}
