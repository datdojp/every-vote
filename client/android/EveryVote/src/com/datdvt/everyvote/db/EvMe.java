package com.datdvt.everyvote.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EvMe extends DBBase {

    private String mEvId;
    private String mEvAccessToken;

    public String getEvId() {
        return mEvId;
    }

    public void setEvId(String evId) {
        mEvId = evId;
    }

    public String getEvAccessToken() {
        return mEvAccessToken;
    }

    public void setEvAccessToken(String evAccessToken) {
        mEvAccessToken = evAccessToken;
    }

    /**
     * DATABASE
     */

    private static final String TABLE = "me";

    public static void createTable(SQLiteDatabase db) {
        dropTable(db);
        db.execSQL("CREATE TABLE " + TABLE + "("
                + "ev_id            TEXT NOT NULL,"
                + "ev_access_token  TEXT)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    public static boolean create(String evId, String evAccessToken) {
        getDatabase().beginTransaction();
        getDatabase().delete(TABLE, null, null);
        ContentValues values = new ContentValues();
        values.put("ev_id", evId);
        values.put("ev_access_token", evAccessToken);
        boolean success = getDatabase().insert(TABLE, null, values) > 0;
        if (success) {
            getDatabase().setTransactionSuccessful();
        }
        getDatabase().endTransaction();
        return success;
    }

    public static EvMe read() {
        Cursor cur = getDatabase().query(TABLE, null, null, null, null, null, null, "1");
        EvMe ret = null;
        if (cur.moveToNext()) {
            ret = parse(cur);
        }
        cur.close();
        return ret;
    }

    private static EvMe parse(Cursor cur) {
        EvMe ret = new EvMe();
        int i = 0;
        ret.setEvId(cur.getString(i++));
        ret.setEvAccessToken(cur.getString(i++));
        return ret;
    }
}
