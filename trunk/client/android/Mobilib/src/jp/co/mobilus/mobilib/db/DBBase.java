package jp.co.mobilus.mobilib.db;

import jp.co.mobilus.mobilib.util.MlUtils;
import android.database.sqlite.SQLiteDatabase;

/**
 * DBxxxクラスのスーパークラス。
 */
public abstract class DBBase {
    private static SQLiteDatabase sDb;
    protected static SQLiteDatabase getDatabase() {
        if (sDb == null) {
            sDb = DBHelper.getInstance(MlUtils.getCurrentContext()).getWritableDatabase();
        }
        return sDb;
    }
}
