package jp.co.mobilus.mobilib.api;

import jp.co.mobilus.mobilib.util.MlUtils;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class MlCache extends DBBase {
    private static final String TABLE = "ml_cache";
    private String mKey;
    private String mFileName;
    private long mDate;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "("
                + "key      TEXT NOT NULL,"
                + "date     LONG)");
        db.execSQL("CREATE INDEX " + TABLE + "_index ON " + TABLE + "(key)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    public static void deleteAll() {
        getDatabase().delete(TABLE, null, null);
    }

    public static boolean insert(MlCache cache) {
        ContentValues values = new ContentValues();
        values.put("key", cache.getKey());
        values.put("date", cache.getDate());
        return -1 != getDatabase().insert(TABLE, null, values);
    }

    public static boolean update(MlCache cache) {
        ContentValues values = new ContentValues();
        values.put("date", cache.getDate());
        return 0 != getDatabase().update(TABLE, values, "key = ?", new String[] { cache.getKey() });
    }

    public static MlCache get(String key) {
        Cursor cur = getDatabase().query(
                TABLE,
                new String[] {"date"}, 
                "key = ?",
                new String[] { key },
                null, null, null);
        MlCache ret = null;

        if (cur.moveToNext()) {
            ret = new MlCache();
            ret.setKey(key);
            ret.setDate(cur.getLong(0));
        }
        cur.close();
        return ret;
    }


    // GENERATED

    public String getFileName() {
        return mFileName;
    }

    public String getKey() {
        return mKey;
    }
    public void setKey(String mKey) {
        this.mKey = mKey;
        this.mFileName = MlUtils.md5(mKey);
    }
    public long getDate() {
        return mDate;
    }
    public void setDate(long mDate) {
        this.mDate = mDate;
    }
}
