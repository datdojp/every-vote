package com.datdvt.everyvote;

import android.content.Intent;
import android.os.Bundle;
import jp.co.mobilus.mobilib.base.MlBaseActivity;

public class EvMainActivity extends MlBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ev_splash);

        startActivity(new Intent(this, EvLoginActivity.class));
    }
}
