package chat.app.manager.utils;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

public class TaskUtils {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static <Params, Progress, Result> void schedule(AsyncTask<Params, Progress, Result> task, Params... params) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }
}
