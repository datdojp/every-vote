package com.datdvt.everyvote;

import jp.co.mobilus.mobilib.base.MblBaseActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import com.datdvt.everyvote.sns.facebook.EvFacebookApi;
import com.datdvt.everyvote.sns.facebook.EvFacebookApi.OauthCallback;

public class EvLoginActivity extends MblBaseActivity {
    
    private WebView mWebView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ev_login_activity);
        
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setVisibility(View.GONE);
        
        findViewById(R.id.bt_login_fb).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginFacebook();
            }
        });
    }
    
    private void loginFacebook() {
        mWebView.setVisibility(View.VISIBLE);
        EvFacebookApi.getInstance().startOauth(mWebView, new OauthCallback() {
            
            @Override
            public void onSuccess(String accessToken) {
                Log.d("%%%", "accessToken=" + accessToken);
                mWebView.setVisibility(View.GONE);
                finish();
            }
            
            @Override
            public void onFailure() {
                Log.d("%%%", "failed");
                mWebView.setVisibility(View.GONE);
            }
        });
    }
}
