package com.datdvt.everyvote.activity;

import android.os.Bundle;
import android.widget.ListView;

import com.datdvt.everyvote.EvSideMenuActivity;
import com.datdvt.everyvote.R;
import com.datdvt.everyvote.adapter.EvSnsAccountAdapter;
import com.datdvt.everyvote.db.EvSnsAccount;

public class EvSnsAccountsActivity extends EvSideMenuActivity {

    private EvSnsAccountAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ev_sns_accounts_activity);

        ListView listView = (ListView) findViewById(R.id.ev_list_view);
        mAdapter = new EvSnsAccountAdapter(listView);
        listView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.changeData(EvSnsAccount.readAll());
    }
}
