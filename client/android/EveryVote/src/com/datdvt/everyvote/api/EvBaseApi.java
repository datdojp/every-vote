package com.datdvt.everyvote.api;

import jp.co.mobilus.mobilib.api.MblApi;

public class EvBaseApi extends MblApi {
    private static final long _5_MIN = 1000l * 60l * 5l;
    private static final long _1_HOUR = 1000l * 60l * 60l;
    @Override
    protected long getCacheDuration(String url, boolean isBinaryRequest) {
        if (isBinaryRequest) {
            return _1_HOUR;
        } else {
            return _5_MIN;
        }
    }
    
    protected static interface EvApiCallback {
        public void onError();
    }

    public static interface EvSimpleCallback extends EvApiCallback {
        public void onSuccess();
    }
}
