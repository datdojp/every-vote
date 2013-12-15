package jp.co.mobilus.mobilib.util;

import android.os.AsyncTask;

public abstract class ControllableAsyncTask extends AsyncTask<Void, Void, Void> {
    
    public void cancel() {
        super.cancel(true);
        handleCancel();
    }

    protected void handleCancel() {}

    public abstract static class Controller {

        private ControllableAsyncTask mAsyncTask;

        protected abstract ControllableAsyncTask generate();

        public Controller execute() {
            Utils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mAsyncTask = generate();
                    mAsyncTask.execute();
                }
            });
            return this;
        }

        public Controller cancel() {
            if (mAsyncTask != null) {
                mAsyncTask.cancel();
            }
            return this;
        }
    }

}
