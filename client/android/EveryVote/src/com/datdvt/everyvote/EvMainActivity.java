package com.datdvt.everyvote;

import jp.co.mobilus.mobilib.base.MblBaseActivity;
import android.content.Intent;
import android.os.Bundle;

public class EvMainActivity extends MblBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ev_splash);

        startActivity(new Intent(this, EvLoginActivity.class));
    }
}
