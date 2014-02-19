package jp.co.mobilus.mobilib.util;


public abstract class MblControllableAsyncTask extends MblAsyncTask {
    
    public void cancel() {
        super.cancel(true);
        handleCancel();
    }

    protected void handleCancel() {}

    public abstract static class MlController {

        private MblControllableAsyncTask mAsyncTask;

        protected abstract MblControllableAsyncTask generate();

        public MlController execute() {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mAsyncTask = generate();
                    mAsyncTask.execute();
                }
            });
            return this;
        }

        public MlController cancel() {
            if (mAsyncTask != null) {
                mAsyncTask.cancel();
            }
            return this;
        }
    }

}
