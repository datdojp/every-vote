package jp.co.mobilus.mobilib.api;

import jp.co.mobilus.mobilib.util.MblUtils;
import android.database.sqlite.SQLiteDatabase;

abstract class DBBase {
    private static SQLiteDatabase sDb;
    protected static SQLiteDatabase getDatabase() {
        if (sDb == null) {
            sDb = DBHelper.getInstance(MblUtils.getCurrentContext()).getWritableDatabase();
        }
        return sDb;
    }
}
