package chat.app.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.thrift.TException;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import chat.app.R;
import chat.app.manager.RemoteManager;
import chat.app.manager.UserManager;
import chat.app.manager.utils.BundleUtils;
import chat.app.manager.utils.TaskUtils;
import chat.app.ui.adapter.UsersAdapter;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.User;

public class AllUsersActivity extends ActionBarActivity {

    @InjectView(R.id.list_users)
    ListView mListUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        ButterKnife.inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_refresh:
                requestAllUsers();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mListUsers.getAdapter() == null) {
            requestAllUsers();
        }
    }

    private void requestAllUsers() {
        if (UserManager.INSTANCE.registered()) {
            TaskUtils.schedule(new GetAllUsersTask(this), UserManager.INSTANCE.getSavedUser());
        }
    }

    @OnItemClick(R.id.list_users)
    @SuppressWarnings("unused")
    void onUserClicked(int position) {
        User user = (User) mListUsers.getItemAtPosition(position);
        startActivity(new Intent(this, DialogsActivity.class)
                .putExtras(BundleUtils.writeToBundle(User.class, user)));
    }

    private static class GetAllUsersTask extends AsyncTask<User, Void, List<User>> {

        private final WeakReference<AllUsersActivity> mActivityRef;
        private ChatException mError;

        public GetAllUsersTask(AllUsersActivity activity) {
            mActivityRef = new WeakReference<AllUsersActivity>(activity);
        }

        /**
         * Makes call in worker thread.
         * @param params username and password
         * @return null if registration succeeded and string error message otherwise
         */
        @Override
        protected List<User> doInBackground(User... params) {
            try {
                final User user = params[0];
                return RemoteManager.INSTANCE.perform(mActivityRef.get(), new RemoteManager.RemoteCall<List<User>>() {
                    @Override
                    public List<User> call(Chat.Client client) throws TException {
                        return client.getAllUsers(user);
                    }
                });
            } catch (ChatException e) {
                mError = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<User> users) {
            final AllUsersActivity activity = mActivityRef.get();
            if (activity != null) {
                if (users != null) {
                    activity.mListUsers.setAdapter(new UsersAdapter(activity, users));
                } else if (mError != null) {
                    Toast.makeText(activity, mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
