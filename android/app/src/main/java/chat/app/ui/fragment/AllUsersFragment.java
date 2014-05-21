package chat.app.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import chat.app.manager.utils.TaskUtils;
import chat.app.ui.adapter.UsersAdapter;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.User;

public class AllUsersFragment extends Fragment {

    @InjectView(R.id.list_users)
    ListView mListUsers;
    private Listener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof Listener) {
            mListener = (Listener) activity;
        } else {
            throw new ClassCastException("Parent should implement listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_users, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                requestAllUsers();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
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
        mListener.onUserSelected(user);
    }

    private static class GetAllUsersTask extends TaskUtils.BaseTask<User, Void, List<User>> {

        private final WeakReference<AllUsersFragment> mFragment;
        private ChatException mError;

        public GetAllUsersTask(AllUsersFragment fragment) {
            super((ActionBarActivity) fragment.getActivity());

            mFragment = new WeakReference<AllUsersFragment>(fragment);
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
                return RemoteManager.INSTANCE.perform(getActivity(), new RemoteManager.RemoteCall<List<User>>() {
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
            super.onPostExecute(users);

            final AllUsersFragment fragment = mFragment.get();
            if (fragment != null && fragment.isAdded()) {
                if (users != null) {
                    fragment.mListUsers.setAdapter(new UsersAdapter(fragment.getActivity(), users));
                } else if (mError != null) {
                    Toast.makeText(fragment.getActivity(), mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public interface Listener {

        void onUserSelected(User user);
    }
}
