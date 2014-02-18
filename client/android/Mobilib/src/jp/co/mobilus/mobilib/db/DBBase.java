package jp.co.mobilus.mobilib.db;

import jp.co.mobilus.mobilib.util.MlInternal;
import android.database.sqlite.SQLiteDatabase;

/**
 * DBxxxクラスのスーパークラス。
 */
public abstract class DBBase {
    private static SQLiteDatabase sDb;
    protected static SQLiteDatabase getDatabase() {
        if (sDb == null) {
            sDb = DBHelper.getInstance(MlInternal.getInstance().getCurrentContext()).getWritableDatabase();
        }
        return sDb;
    }
}
