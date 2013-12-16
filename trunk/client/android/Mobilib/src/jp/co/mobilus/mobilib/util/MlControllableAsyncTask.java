package jp.co.mobilus.mobilib.util;


public abstract class MlControllableAsyncTask extends MlAsyncTask {
    
    public void cancel() {
        super.cancel(true);
        handleCancel();
    }

    protected void handleCancel() {}

    public abstract static class MlController {

        private MlControllableAsyncTask mAsyncTask;

        protected abstract MlControllableAsyncTask generate();

        public MlController execute() {
            MlInternal.executeOnMainThread(new Runnable() {
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
