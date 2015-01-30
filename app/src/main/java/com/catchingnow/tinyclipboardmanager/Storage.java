package com.catchingnow.tinyclipboardmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by heruoxin on 14/12/9.
 */
public class Storage {
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    private static final String TABLE_NAME = "clipHistory";
    private static final String CLIP_STRING = "history";
    private static final String CLIP_DATE = "date";
    private StorageHelper dbHelper;
    private SQLiteDatabase db;
    private Context c;
    private List<ClipObject> clipsInMemory;
    private boolean isClipsInMemoryChanged = true;

    public Storage(Context context) {
        c = context;
        dbHelper = new StorageHelper(c);
    }

    public String sqliteEscape(String keyWord) {
        if ("".equals(keyWord) || keyWord == null) {
            return keyWord;
        }
        return keyWord
                .replace("'", "''")
//                .replace("/", "//")
//                .replace("[", "/[")
//                .replace("]", "/]")
//                .replace("%", "/%")
//                .replace("&", "/&")
//                .replace("_", "/_")
//                .replace("(", "/(")
//                .replace(")", "/)")
                ;
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (db != null) {
            if (db.isOpen()) {
                db.close();
            }
        }
    }

    public List<ClipObject> getClipHistory() {
        return getClipHistory("");
    }

    public List<ClipObject> getClipHistory(String queryString) {
        if (isClipsInMemoryChanged) {
            open();
            String sortOrder = CLIP_DATE + " DESC";
            String[] COLUMNS = {CLIP_STRING, CLIP_DATE};
            Cursor c;
            if (queryString == null) {
                c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, sortOrder);
            } else {
                c = db.query(TABLE_NAME, COLUMNS, CLIP_STRING + " LIKE '%" + sqliteEscape(queryString) + "%'", null, null, null, sortOrder);
            }
            clipsInMemory = new ArrayList<ClipObject>();
            while (c.moveToNext()) {
                //clipsInMemory.add(c.getString(0));
                clipsInMemory.add(new ClipObject(c.getString(0), new Date(c.getLong(1))));
            }
            c.close();
            close();
            isClipsInMemoryChanged = false;
        }
        return clipsInMemory;
    }

    public List<ClipObject> getClipHistory(int n) {
        List<ClipObject> ClipHistory = getClipHistory();
        List<ClipObject> thisClips = new ArrayList<ClipObject>();
        n = (n > ClipHistory.size() ? ClipHistory.size() : n);
        for (int i = 0; i < n; i++) {
            thisClips.add(ClipHistory.get(i));
        }
        return thisClips;
    }

    public boolean deleteClipHistory(String query) {
        open();
        int row_id = db.delete(TABLE_NAME, CLIP_STRING + "='" + sqliteEscape(query) + "'", null);
        close();
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistory " + query);
            return false;
        }
        CBWatcherService.startCBService(c, true);
        return true;
    }

    public boolean deleteClipHistoryBefore(float days) {
        Date date = new Date();
        long timeStamp = (long) (date.getTime() - days * 86400000);
        open();
        int row_id = db.delete(TABLE_NAME, CLIP_DATE + "<'" + timeStamp + "'", null);
        close();
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistoryBefore " + days);
            return false;
        }
        return true;
    }

    public boolean addClipHistory(String currentString) {
        Log.v(PACKAGE_NAME, "ADD CLIP:" + currentString);
        List<ClipObject> tmpClips = getClipHistory();
        for (ClipObject thisClip : tmpClips) {
            String str = thisClip.text;
            if (str.equals(currentString)) {
                deleteClipHistory(str);
            }
        }
        open();
        Date date = new Date();
        long timeStamp = date.getTime();
        ContentValues values = new ContentValues();
        values.put(CLIP_DATE, timeStamp);
        values.put(CLIP_STRING, currentString);
        long row_id = db.insert(TABLE_NAME, null, values);
        close();
        if (row_id == -1) {
            Log.e("Storage", "write db error: addClipHistory " + currentString);
            return false;
        }
        isClipsInMemoryChanged = true;
        return true;
    }

    public class StorageHelper extends SQLiteOpenHelper {
        private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
        private static final int DATABASE_VERSION = 2;
        private static final String DATABASE_NAME = "clippingnow.db";
        private static final String TABLE_NAME = "cliphistory";
        private static final String CLIP_STRING = "history";
        private static final String CLIP_DATE = "date";
        private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        CLIP_DATE + " TIMESTAMP, " +
                        CLIP_STRING + " TEXT);";

        public StorageHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(PACKAGE_NAME, "SQL updated from" + oldVersion + "to" + newVersion);
        }
    }

//    public void printClips(int n) {
//        for (int i=0; i<n; i++){
//            String s = getClipHistory(n);
//            Log.v("printClips", s);
//        }
//    }

}