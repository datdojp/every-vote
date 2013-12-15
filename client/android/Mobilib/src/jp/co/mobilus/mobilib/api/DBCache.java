package jp.co.mobilus.mobilib.api;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBCache {
    public static void createTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS pc_cache");
        db.execSQL("CREATE TABLE IF NOT EXISTS pc_cache("
                + "key      TEXT NOT NULL,"
                + "date     LONG)");
        db.execSQL("CREATE INDEX pc_cache_index ON pc_cache(key)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS pc_cache");
    }

    public static void deleteAll(SQLiteDatabase db) {
        db.delete("pc_cache", null, null);
    }

    public static boolean insert(SQLiteDatabase db, PcCache cache) {
        ContentValues values = new ContentValues();
        values.put("key", cache.getKey());
        values.put("date", cache.getDate());
        return -1 != db.insert("pc_cache", null, values);
    }

    public static boolean update(SQLiteDatabase db, PcCache cache) {
        ContentValues values = new ContentValues();
        values.put("date", cache.getDate());
        return 0 != db.update("pc_cache", values, "key = ?", new String[] { cache.getKey() });
    }

    public static PcCache get(SQLiteDatabase db, String key) {
        Cursor cur = db.query(
                "pc_cache",
                new String[] {"date"}, 
                "key = ?",
                new String[] { key },
                null, null, null);
        PcCache ret = null;

        if (cur.moveToNext()) {
            ret = new PcCache();
            ret.setKey(key);
            ret.setDate(cur.getLong(0));
        }
        cur.close();
        return ret;
    }
}
