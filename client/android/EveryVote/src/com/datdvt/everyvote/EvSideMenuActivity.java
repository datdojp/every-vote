package com.datdvt.everyvote;

import com.datdvt.everyvote.activity.EvFriendsActivity;
import com.datdvt.everyvote.activity.EvRequestsActivity;
import com.datdvt.everyvote.activity.EvSnsAccountsActivity;

import jp.co.mobilus.mobilib.base.MblBaseActivity;
import jp.co.mobilus.mobilib.widget.MblSideMenuEnabledLayout;
import jp.co.mobilus.mobilib.widget.MblSideMenuEnabledLayout.MlSideMenuEnabledLayoutDelegate;
import jp.co.mobilus.mobilib.widget.MblSideMenuEnabledLayout.SidePosition;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class EvSideMenuActivity extends MblBaseActivity implements MlSideMenuEnabledLayoutDelegate {

    private MblSideMenuEnabledLayout mSideMenuEnabledLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSideMenuEnabledLayout = new MblSideMenuEnabledLayout(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        View layout = getLayoutInflater().inflate(layoutResID, null);

        View rightMenu = getLayoutInflater().inflate(R.layout.ev_right_menu_layout, null);
        rightMenu.findViewById(R.id.ev_requests_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(EvSideMenuActivity.this instanceof EvRequestsActivity)) {
                    EvSideMenuActivity.this.startActivity(
                            new Intent(EvSideMenuActivity.this, EvRequestsActivity.class));
                }
            }
        });
        rightMenu.findViewById(R.id.ev_friends_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(EvSideMenuActivity.this instanceof EvFriendsActivity)) {
                    EvSideMenuActivity.this.startActivity(
                            new Intent(EvSideMenuActivity.this, EvFriendsActivity.class));
                }
            }
        });
        rightMenu.findViewById(R.id.ev_sns_accounts_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(EvSideMenuActivity.this instanceof EvSnsAccountsActivity)) {
                    EvSideMenuActivity.this.startActivity(
                            new Intent(EvSideMenuActivity.this, EvSnsAccountsActivity.class));
                }
            }
        });
        rightMenu.findViewById(R.id.ev_logout_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        mSideMenuEnabledLayout.init(this, null, layout, rightMenu, -1, -1, -1, this);
        super.setContentView(mSideMenuEnabledLayout);
    }

    private void logout() {
        // %%%
    }

    protected MblSideMenuEnabledLayout getSideMenuEnabledLayout() {
        return mSideMenuEnabledLayout;
    }

    @Override
    public void handleCurrentSideChange(SidePosition pos) {}

    @Override
    public boolean shouldAllowSwipeToOpenSideView(MotionEvent event) {
        return true;
    }
}
