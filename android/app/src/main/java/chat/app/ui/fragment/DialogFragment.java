package chat.app.ui.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.thrift.TException;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import chat.app.R;
import chat.app.manager.RemoteManager;
import chat.app.manager.UserManager;
import chat.app.manager.utils.BundleUtils;
import chat.app.manager.utils.TaskUtils;
import chat.app.ui.activity.DialogsActivity;
import chat.app.ui.adapter.MessagesAdapter;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.Dialog;
import thrift.entity.Message;
import thrift.entity.User;

public class DialogFragment extends Fragment {

    @InjectView(R.id.list_message)
    ListView mListMessages;

    @InjectView(R.id.edit_message)
    EditText mEditMessage;

    public static DialogFragment newInstance(Dialog dialog) {
        DialogFragment fragment = new DialogFragment();
        fragment.setArguments(BundleUtils.writeToBundle(Dialog.class, dialog));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Dialog dialog = BundleUtils.fetchFromBundle(Dialog.class, getArguments());
        ((DialogsActivity) activity).onDialogAttached(dialog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_refresh, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            requestMessages();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.image_send)
    @SuppressWarnings("unused")
    void onSendClicked() {
        if (TextUtils.isEmpty(mEditMessage.getText()) && UserManager.INSTANCE.registered()) {
            User me = UserManager.INSTANCE.getSavedUser();
            String data = mEditMessage.getText().toString();
            Dialog dialog = BundleUtils.fetchFromBundle(Dialog.class, getArguments());
            dialog.setLastMessage(new Message(data, me, null));
            TaskUtils.schedule(new SendMessageTask(this, me), dialog);
        } else {
            Toast.makeText(getActivity(), R.string.error_no_empty_messages, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mListMessages.getAdapter() == null) {
            requestMessages();
        }
    }

    /**
     * Reloads messages from server if current user is registered.
     */
    private void requestMessages() {
        if (UserManager.INSTANCE.registered()) {
            Dialog dialog = BundleUtils.fetchFromBundle(Dialog.class, getArguments());
            TaskUtils.schedule(new GetMessagesTask(this, UserManager.INSTANCE.getSavedUser()), dialog);
        }
    }

    private static class GetMessagesTask extends AsyncTask<Dialog, Void, List<Message>> {

        private final WeakReference<DialogFragment> mFragmentRef;
        protected final User mUser;
        private ChatException mError;

        private GetMessagesTask(DialogFragment fragment, User user) {
            mFragmentRef = new WeakReference<DialogFragment>(fragment);
            mUser = user;
        }

        public DialogFragment getFragment() {
            return mFragmentRef.get();
        }

        @Override
        protected List<Message> doInBackground(Dialog... params) {
            try {
                Fragment fragment = getFragment();
                if (fragment == null) {
                    return null;
                }
                final Dialog dialog = params[0];
                return makeCall(dialog);
            } catch (ChatException e) {
                mError = e;
            }
            return null;
        }

        /**
         * Makes actual call to server.
         * @param dialog to make call with
         * @return updated list of messages due to docs
         * @throws ChatException
         */
        List<Message> makeCall(final Dialog dialog) throws ChatException {
            return RemoteManager.INSTANCE.perform(getFragment().getActivity(), new RemoteManager.RemoteCall<List<Message>>() {
                @Override
                public List<Message> call(Chat.Client client) throws TException {
                    return client.getMessages(mUser, dialog.getPartner());
                }
            });
        }

        @Override
        protected void onPostExecute(List<Message> messages) {
            final DialogFragment fragment = getFragment();
            if (fragment != null) {
                if (messages != null) {
                    fragment.mListMessages.setAdapter(new MessagesAdapter(fragment.getActivity(), messages, mUser));
                } else if (mError != null) {
                    Toast.makeText(fragment.getActivity(), mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class SendMessageTask extends GetMessagesTask {

        private SendMessageTask(DialogFragment fragment, User user) {
            super(fragment, user);
        }

        /**
         * Sends new message to user.
         * {@inheritDoc}
         */
        @Override
        List<Message> makeCall(final Dialog dialog) throws ChatException {
            return RemoteManager.INSTANCE.perform(getFragment().getActivity(), new RemoteManager.RemoteCall<List<Message>>() {
                @Override
                public List<Message> call(Chat.Client client) throws TException {
                    return client.sendMessage(mUser, dialog);
                }
            });
        }
    }
}