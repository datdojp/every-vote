package com.datdvt.everyvote.api;

import java.util.HashMap;
import java.util.Map;

import jp.co.mobilus.mobilib.util.MblUtils;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.datdvt.everyvote.db.EvMe;
import com.datdvt.everyvote.db.EvSnsAccount;
import com.datdvt.everyvote.util.EvConfigs;
import com.datdvt.everyvote.util.EvConstants;

public class EvApi extends EvBaseApi {
    private static final String TAG = EvConstants.TAG_PREFIX + EvApi.class.getSimpleName();

    private static final String ERR_CODE = "err_code";
    private static final String ERR_MSG = "err_msg";
    private static final String RESULT = "result";

    private static EvApi sInstance;
    private EvMe sMe;

    public static EvApi getInstance() {
        if (sInstance == null) {
            sInstance = new EvApi();
        }
        return sInstance;
    }

    private EvApi() {
        loadMe();
    }

    private void loadMe() {
        sMe = EvMe.read();
    }

    public boolean isLoggedin() {
        return 
                sMe != null &&
                !MblUtils.isEmpty(sMe.getEvId()) &&
                !MblUtils.isEmpty(sMe.getEvAccessToken());
    }

    public void login(EvSnsAccount snsAccount, final EvSimpleCallback callback) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", snsAccount.getType());
        params.put("sns_id", snsAccount.getSnsId());
        params.put("access_token", snsAccount.getAccessToken());

        post(
                getFullUrl("login"),
                params,
                null,
                false,
                new MlApiPostCallback() {
                    @Override
                    public void onSuccess(int statusCode, String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            int err_code = json.getInt(ERR_CODE);
                            if (err_code == 0) {
                                JSONObject result = json.getJSONObject(RESULT);
                                String evId = result.getString("id");
                                String evAccessToken = result.getString("access_token");
                                EvMe.create(evId, evAccessToken);

                                loadMe();

                                callback.onSuccess();
                            } else {
                                Log.e(TAG, "Login failed with error: err_code="
                                        + err_code + ", err_msg=" + json.optString(ERR_MSG));
                                callback.onError();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Login failed due to json error: " + data, e);
                            callback.onError();
                        }

                    }

                    @Override
                    public void onFailure(int statusCode, String data) {
                        Log.e(TAG, "Login failed: statusCode=" + statusCode + ", data=" + data);
                        callback.onError();
                    }
                });
    }

    private String getFullUrl(String path) {
        return EvConfigs.EVERYVOTE_SERVER + path;
    }
}
