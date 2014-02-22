package com.datdvt.everyvote.api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import jp.co.mobilus.mobilib.util.MblUtils;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.datdvt.everyvote.R;
import com.datdvt.everyvote.db.EvSnsAccount;
import com.datdvt.everyvote.util.EvConfigs;
import com.datdvt.everyvote.util.EvConstants;

public class EvFacebookApi extends EvBaseApi {

    private static final String TAG = EvConstants.TAG_PREFIX + EvFacebookApi.class.getSimpleName();

    private static final String OAUTH_REDIRECT_URI = "http://redirect.everyvote.com";
    private static final String GRASH_API_BASE_URL = "https://graph.facebook.com/";

    private static final String ERROR = "error";

    private static EvFacebookApi sInstance;
    private EvSnsAccount mSnsAccount;

    private EvFacebookApi() {
        mSnsAccount = EvSnsAccount.read(EvSnsAccount.TYPE_FACEBOOK);
    }

    public static EvFacebookApi getInstance() {
        if (sInstance == null) {
            sInstance = new EvFacebookApi();
        }
        return sInstance;
    }

    public EvSnsAccount getSnsAccount() {
        return mSnsAccount;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void startOauth(WebView webView, final EvOauthCallback cb) {
        String url = Uri.parse("https://www.facebook.com").buildUpon()
                .path("/dialog/oauth")
                .appendQueryParameter("client_id", EvConfigs.FACEBOOK_CLIENT_ID)
                .appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("state", "login")
                .build().toString();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("about:blank");

        // http://stackoverflow.com/questions/4200259/tapping-form-field-in-webview-does-not-show-soft-keyboard
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
                }
                return false;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!url.startsWith(OAUTH_REDIRECT_URI)) {
                    MblUtils.showProgressDialog(R.string.loading);
                }
                super.onPageStarted(view, url, favicon);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.startsWith(OAUTH_REDIRECT_URI)) {
                    MblUtils.clearAllProgressDialogs();
                }
                super.onPageFinished(view, url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(OAUTH_REDIRECT_URI)) {
                    Matcher m = Pattern.compile("^" + Pattern.quote(OAUTH_REDIRECT_URI) + ".*#access_token=([^&]+).*$").matcher(url);
                    if (m.find()) {
                        String accessToken = m.group(1);
                        mSnsAccount = new EvSnsAccount(
                                null,
                                EvSnsAccount.TYPE_FACEBOOK,
                                accessToken,
                                null,
                                null);
                        EvSnsAccount.createOrUpdate(mSnsAccount); 

                        cb.onSuccess();
                    } else {
                        cb.onFailure();
                    }

                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                cb.onFailure();
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        webView.loadUrl(url);
    }

    public static interface EvOauthCallback {
        public void onSuccess();
        public void onFailure();
    }

    public void loadBasicInfo(final EvSimpleCallback callback) {
        Map<String, String> params = getBaseParams();
        params.put("fields", "id,name,picture");
        get(
                getFullUrl("me"),
                params,
                null,
                true,
                false,
                false,
                new MlApiGetCallback() {

                    @Override
                    public void onSuccess(String data) {
                        try {
                            JSONObject json = new JSONObject(data);
                            JSONObject error = json.optJSONObject(ERROR);
                            if (error == null) {
                                String id = json.getString("id");
                                String name = json.getString("name");
                                String avatarUrl =
                                        json.getJSONObject("picture")
                                        .getJSONObject("data")
                                        .getString("url");
                                mSnsAccount.setSnsId(id);
                                mSnsAccount.setName(name);
                                mSnsAccount.setAvatarUrl(avatarUrl);
                                EvSnsAccount.createOrUpdate(mSnsAccount);

                                callback.onSuccess();
                            } else {
                                Log.e(TAG, "Facebook load basic info failed: "
                                        + "error=" + error.toString());
                                callback.onError();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Facebook load basic info failed due to JSON error: data=" + data, e);
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(int error, String errorMessage) {
                        Log.e(TAG, "Failed to log basic info: error="
                                + error + ", errorMessage=" + errorMessage);
                        callback.onError();
                    }
                });
    }

    private String getFullUrl(String path) {
        return GRASH_API_BASE_URL + path;
    }

    private Map<String, String> getBaseParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        params.put("access_token", mSnsAccount.getAccessToken());
        return params;
    }
}
