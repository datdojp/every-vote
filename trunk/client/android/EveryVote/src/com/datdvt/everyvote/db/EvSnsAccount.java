package com.datdvt.everyvote.db;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class EvSnsAccount extends DBBase {
    public static final String TYPE_FACEBOOK = "facebook";

    private String mSnsId;
    private String mType;
    private String mAccessToken;
    private String mName;
    private String mAvatarUrl;

    public EvSnsAccount() {
    }
    
    public EvSnsAccount(
            String snsId,
            String type,
            String accessToken,
            String name,
            String avatarUrl) {
        super();
        mSnsId = snsId;
        mType = type;
        mAccessToken = accessToken;
        mName = name;
        mAvatarUrl = avatarUrl;
    }

    public String getSnsId() {
        return mSnsId;
    }

    public void setSnsId(String id) {
        mSnsId = id;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }


    /**
     * DATABASE
     */
    private static final String TABLE = "sns_account";

    public static void createTable(SQLiteDatabase db) {
        dropTable(db);
        db.execSQL("CREATE TABLE " + TABLE + "("
                + "sns_id           TEXT,"
                + "type             TEXT NOT NULL,"
                + "access_token     TEXT,"
                + "name             TEXT,"
                + "avatar_url       TEXT)");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    public static List<EvSnsAccount> readAll() {
        Cursor cur = getDatabase().query(TABLE, null, null, null, null, null, "type asc");
        List<EvSnsAccount> ret = new ArrayList<EvSnsAccount>();
        while (cur.moveToNext()) {
            ret.add(parse(cur));
        }
        cur.close();
        return ret;
    }
    
    public static EvSnsAccount read(String type) {
        Cursor cur = getDatabase().query(
                TABLE,
                null,
                "type=?",
                new String[] {type},
                null,
                null,
                null,
                "1");
        EvSnsAccount ret = null;
        if (cur.moveToFirst()) {
            ret = parse(cur);
        }
        cur.close();
        return ret;
    }

    public static boolean createOrUpdate(EvSnsAccount snsAccount) {
        Assert.assertNotNull(snsAccount);
        Assert.assertNotNull(snsAccount.getType());
        ContentValues values = new ContentValues();
        values.put("sns_id", snsAccount.getSnsId());
        values.put("type", snsAccount.getType());
        values.put("access_token", snsAccount.getAccessToken());
        values.put("name", snsAccount.getName());
        values.put("avatar_url", snsAccount.getAvatarUrl());
        
        Cursor cur = getDatabase().query(
                TABLE,
                new String[] {"type"},
                "type=?",
                new String[] {snsAccount.getType()},
                null,
                null,
                null,
                "1");
        boolean ret;
        if (cur.moveToFirst()) {
            ret = getDatabase().update(TABLE, values, "type=?", new String[]{snsAccount.getType()}) > 0;
        } else {
            ret = getDatabase().insert(TABLE, null, values) != -1;
        }
        cur.close();
        return ret;
    }

    private static EvSnsAccount parse(Cursor cur) {
        EvSnsAccount ret = new EvSnsAccount();
        int i = 0;
        ret.setSnsId(cur.getString(i++));
        ret.setType(cur.getString(i++));
        ret.setAccessToken(cur.getString(i++));
        ret.setName(cur.getString(i++));
        ret.setAvatarUrl(cur.getString(i++));
        return ret;
    }
}
