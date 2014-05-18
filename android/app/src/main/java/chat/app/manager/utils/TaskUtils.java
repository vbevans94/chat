package chat.app.manager.utils;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;

import java.lang.ref.WeakReference;

public class TaskUtils {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static <Params, Progress, Result> void schedule(AsyncTask<Params, Progress, Result> task, Params... params) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    public abstract static class BaseTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

        private final WeakReference<ActionBarActivity> mActivityRef;

        public BaseTask(ActionBarActivity activity) {
            mActivityRef = new WeakReference<ActionBarActivity>(activity);
        }

        public ActionBarActivity getActivity() {
            return mActivityRef.get();
        }

        @Override
        protected void onPreExecute() {
            if (getActivity() != null) {
                getActivity().setSupportProgressBarIndeterminateVisibility(true);
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            if (getActivity() != null) {
                getActivity().setSupportProgressBarIndeterminateVisibility(false);
            }
        }
    }
}
