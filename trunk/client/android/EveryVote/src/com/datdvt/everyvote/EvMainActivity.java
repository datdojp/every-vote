package com.datdvt.everyvote;

import jp.co.mobilus.mobilib.base.MblBaseActivity;
import android.content.Intent;
import android.os.Bundle;

import com.datdvt.everyvote.activity.EvLoginActivity;
import com.datdvt.everyvote.activity.EvSnsAccountsActivity;
import com.datdvt.everyvote.api.EvApi;

public class EvMainActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ev_splash);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (EvApi.getInstance().isLoggedin()) {
            startActivity(new Intent(this, EvSnsAccountsActivity.class));
        } else {
            startActivity(new Intent(this, EvLoginActivity.class));
        }
    }
}
