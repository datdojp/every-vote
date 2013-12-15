package jp.co.mobilus.mobilib.api;

import jp.co.mobilus.mobilib.util.PcUtils;

public class PcCache {
    private String mKey;
    private String mFileName;
    private long mDate;

    public String getFileName() {
        return mFileName;
    }

    public String getKey() {
        return mKey;
    }
    public void setKey(String mKey) {
        this.mKey = mKey;
        this.mFileName = PcUtils.md5(mKey);
    }
    public long getDate() {
        return mDate;
    }
    public void setDate(long mDate) {
        this.mDate = mDate;
    }
}
