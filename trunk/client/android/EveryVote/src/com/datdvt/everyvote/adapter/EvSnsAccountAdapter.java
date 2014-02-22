package com.datdvt.everyvote.adapter;

import java.util.ArrayList;
import java.util.List;

import jp.co.mobilus.mobilib.api.MblApi.MlApiGetCallback;
import jp.co.mobilus.mobilib.util.MblDataLoader;
import jp.co.mobilus.mobilib.util.MblImageLoader;
import jp.co.mobilus.mobilib.util.MblUtils;
import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.datdvt.everyvote.R;
import com.datdvt.everyvote.api.EvApi;
import com.datdvt.everyvote.api.EvBaseApi.EvSimpleCallback;
import com.datdvt.everyvote.api.EvFacebookApi;
import com.datdvt.everyvote.db.EvSnsAccount;

public class EvSnsAccountAdapter extends BaseAdapter {

    private final List<EvSnsAccount> mData = new ArrayList<EvSnsAccount>();
    private MblDataLoader<EvSnsAccount> mDataLoader;
    private MblImageLoader<EvSnsAccount> mImageLoader;

    public void changeData(List<EvSnsAccount> data) {
        mData.clear();
        if (data != null) {
            mData.addAll(data);
        }
    }

    public EvSnsAccountAdapter(final ListView listView) {
        mDataLoader = new MblDataLoader<EvSnsAccount>() {

            @Override
            protected ViewGroup getContainerLayout() {
                return listView;
            }

            @Override
            protected boolean shouldLoadOneByOne() {
                return false;
            }

            @Override
            protected EvSnsAccount getItemFromView(View view) {
                return (EvSnsAccount) view.getTag();
            }

            @Override
            protected boolean shouldLoadDataForItem(EvSnsAccount item) {
                return true;
            }

            @Override
            protected void retrieveData(
                    final EvSnsAccount item,
                    final DataRetrievedCallback<EvSnsAccount> cb) {
                if (TextUtils.equals(EvSnsAccount.TYPE_FACEBOOK, item.getType())) {
                    EvFacebookApi.getInstance().loadBasicInfo(new EvSimpleCallback() {

                        @Override
                        public void onSuccess() {
                            cb.onRetrieved(EvSnsAccount.read(item.getType()));
                        }

                        @Override
                        public void onError() {
                            cb.onRetrieved(null);
                        }
                    });
                } else {
                    // %%%
                }
            }

            @Override
            protected boolean matchViewAndItem(View view, EvSnsAccount item) {
                EvSnsAccount snsAccount = (EvSnsAccount) view.getTag();
                return TextUtils.equals(snsAccount.getType(), item.getType());
            }

            @Override
            protected void updateView(View view) {
                EvSnsAccount snsAccount = (EvSnsAccount) view.getTag();

                TextView nameText = (TextView) view.findViewById(R.id.ev_name_txt);
                nameText.setText(snsAccount.getName());

                ImageView snsTypeImage = (ImageView) view.findViewById(R.id.ev_sns_type_image);
                if (EvSnsAccount.TYPE_FACEBOOK.equals(snsAccount.getType())) {
                    snsTypeImage.setImageResource(R.drawable.facebook);
                } else {
                    // %%%
                }

                mImageLoader.loadImage(view);
            }

            @Override
            protected void applyDataOnItem(EvSnsAccount item, EvSnsAccount data) {
                item.setSnsId(data.getSnsId());
                item.setName(data.getName());
                item.setAvatarUrl(data.getAvatarUrl());
            }
        };

        mImageLoader = new MblImageLoader<EvSnsAccount>() {

            @Override
            protected ViewGroup getContainerLayout() {
                return listView;
            }

            @Override
            protected int getImageWidth() {
                return -1;
            }

            @Override
            protected int getImageHeight() {
                return -1;
            }

            @Override
            protected String getItemPrefix() {
                return "sns_account_avatar";
            }

            @Override
            protected String getItemImageId(EvSnsAccount item) {
                return item.getType();
            }

            @Override
            protected int getDefaultImageResource(EvSnsAccount item) {
                if (EvSnsAccount.TYPE_FACEBOOK.equals(item.getType())) {
                    return R.drawable.facebook_default_avatar;
                } else {
                    return 0; // %%%
                }
            }

            @Override
            protected boolean shouldLoadImage(EvSnsAccount item) {
                return !MblUtils.isEmpty(item.getAvatarUrl());
            }

            @Override
            protected boolean matchViewAndItem(View view, EvSnsAccount item) {
                EvSnsAccount snsAccount = (EvSnsAccount) view.getTag();
                return TextUtils.equals(snsAccount.getType(), item.getType());
            }

            @Override
            protected ImageView getImageView(View view) {
                return (ImageView) view.findViewById(R.id.ev_avatar_img);
            }

            @Override
            protected EvSnsAccount getItem(View view) {
                return (EvSnsAccount) view.getTag();
            }

            @Override
            protected void retrieveImage(
                    final EvSnsAccount item,
                    boolean isCacheEnabled,
                    final ImageRetrievingCallback cb) {
                EvApi.getInstance().get(
                        item.getAvatarUrl(),
                        null, 
                        null,
                        true,
                        true,
                        false,
                        new MlApiGetCallback() {
                            @Override
                            public void onSuccess(byte[] data) {
                                cb.onRetrievedByteArrray(data);
                            }

                            @Override
                            public void onFailure(int error, String errorMessage) {
                                cb.onRetrievedBitmap(null);
                            }
                        });

            }
        };
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        EvSnsAccount snsAccount = (EvSnsAccount) getItem(position);

        if (view == null) {
            view = ((Activity) MblUtils.getCurrentContext()).getLayoutInflater()
                    .inflate(R.layout.ev_sns_account_item, null);
        }

        view.setTag(snsAccount);
        mDataLoader.loadData(view);

        return view;
    }
}
