package com.datdvt.everyvote.activity;

import jp.co.mobilus.mobilib.base.MblBaseActivity;
import jp.co.mobilus.mobilib.util.MblUtils;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Toast;

import com.datdvt.everyvote.R;
import com.datdvt.everyvote.api.EvApi;
import com.datdvt.everyvote.api.EvBaseApi.EvSimpleCallback;
import com.datdvt.everyvote.api.EvFacebookApi;
import com.datdvt.everyvote.api.EvFacebookApi.EvOauthCallback;
import com.datdvt.everyvote.db.EvSnsAccount;

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
        EvFacebookApi.getInstance().startOauth(mWebView, new EvOauthCallback() {

            @Override
            public void onSuccess() {
                mWebView.setVisibility(View.GONE);

                EvFacebookApi.getInstance().loadBasicInfo(new EvSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        mWebView.setVisibility(View.GONE);
                        loginEveryVoteServer(EvFacebookApi.getInstance().getSnsAccount());
                    }
                    @Override
                    public void onError() {
                        mWebView.setVisibility(View.GONE);
                        MblUtils.showToast(R.string.login_facebook_failed, Toast.LENGTH_SHORT);
                    }
                });
            }

            @Override
            public void onFailure() {
                mWebView.setVisibility(View.GONE);
                MblUtils.showToast(R.string.login_facebook_failed, Toast.LENGTH_SHORT);
            }
        });
    }

    private void loginEveryVoteServer(EvSnsAccount fbAccount) {
        MblUtils.showProgressDialog(R.string.login_everyvote_server);
        EvApi.getInstance().login(fbAccount, new EvSimpleCallback() {

            @Override
            public void onSuccess() {
                MblUtils.hideProgressDialog();
                finish();
            }

            @Override
            public void onError() {
                MblUtils.hideProgressDialog();
                MblUtils.showToast(
                        R.string.login_everyvote_server_failed,
                        Toast.LENGTH_SHORT);
            }
        });
    }
}
