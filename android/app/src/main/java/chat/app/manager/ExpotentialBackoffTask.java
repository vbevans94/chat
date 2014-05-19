package chat.app.manager;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.util.Random;

import chat.app.manager.utils.TaskUtils;

public abstract class ExpotentialBackoffTask<Param, Progress, Result> extends TaskUtils.BaseTask<Param, Progress, Result> {

    private static final String TAG = ExpotentialBackoffTask.class.getSimpleName();
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    public ExpotentialBackoffTask(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    protected Result doInBackground(Param... params) {
        long backoff = BACKOFF_MILLI_SECONDS + new Random().nextInt(1000);
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Log.d(TAG, "Attempt #" + i + " to run");
            try {
                return doWork(params);
            } catch (Exception e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                Log.d(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return null;
    }

    public abstract Result doWork(Param... params) throws Exception;
}
