package com.datdvt.everyvote.sns.facebook;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.mobilus.mobilib.api.MlApi;
import jp.co.mobilus.mobilib.util.MlUtils;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.datdvt.everyvote.R;
import com.datdvt.everyvote.util.EvConstants;

public class EvFacebookApi extends MlApi {

    //    private static final String TAG = MlUtils.getTag(EvFacebookApi.class);
    private static final String OAUTH_REDIRECT_URI = "http://redirect.everyvote.com";

    private static EvFacebookApi sInstance;

    public static EvFacebookApi getInstance() {
        if (sInstance == null) {
            sInstance = new EvFacebookApi();
        }
        return sInstance;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void startOauth(WebView webView, final OauthCallback cb) {
        String url = Uri.parse("https://www.facebook.com").buildUpon()
                .path("/dialog/oauth")
                .appendQueryParameter("client_id", EvConstants.FACEBOOK_CLIENT_ID)
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
                    MlUtils.showProgressDialog(R.string.loading);
                }
                super.onPageStarted(view, url, favicon);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.startsWith(OAUTH_REDIRECT_URI)) {
                    MlUtils.clearAllProgressDialogs();
                }
                super.onPageFinished(view, url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(OAUTH_REDIRECT_URI)) {
                    Matcher m = Pattern.compile("^" + Pattern.quote(OAUTH_REDIRECT_URI) + ".*#access_token=([^&]+).*$").matcher(url);
                    if (m.find()) {
                        String accessToken = m.group(1);
                        cb.onSuccess(accessToken);
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

    public static interface OauthCallback {
        public void onSuccess(String accessToken);
        public void onFailure();
    }

}
