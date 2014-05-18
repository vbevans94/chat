package chat.app.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.thrift.TException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import chat.app.R;
import chat.app.manager.RemoteManager;
import chat.app.manager.UserManager;
import chat.app.manager.utils.TaskUtils;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.User;

public class AuthActivity extends BaseActivity {

    @InjectView(R.id.edit_name)
    EditText mEditName;
    @InjectView(R.id.edit_password)
    EditText mEditPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        ButterKnife.inject(this);
    }

    @OnClick(R.id.button_register)
    @SuppressWarnings("unused")
    void onRegisterClicked() {
        Credentials credentials = getAndValidate();
        if (credentials.valid()) {
            TaskUtils.schedule(new RegisterTask(this), credentials);
        }
    }

    @OnClick(R.id.button_login)
    @SuppressWarnings("unused")
    void onLoginClicked() {
        Credentials credentials = getAndValidate();
        if (credentials.valid()) {
            TaskUtils.schedule(new LoginTask(this), credentials);
        }
    }

    private Credentials getAndValidate() {
        Credentials credentials = new Credentials();
        if (!TextUtils.isEmpty(mEditName.getText())) {
            credentials.username = mEditName.getText();
        } else {
            mEditName.setError(getString(R.string.error_enter_name));
        }

        if (!TextUtils.isEmpty(mEditPassword.getText())) {
            credentials.password = mEditPassword.getText();
        } else {
            mEditPassword.setError(getString(R.string.error_enter_password));
        }
        return credentials;
    }

    private void response(ChatException error) {
        if (error != null) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.message_welcome, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static class RegisterTask extends TaskUtils.BaseTask<Credentials, Void, Void> {

        private ChatException mError;

        public RegisterTask(AuthActivity activity) {
            super(activity);
        }

        /**
         * Makes call in worker thread.
         * @param params username and password
         * @return null if registration succeeded and string error message otherwise
         */
        @Override
        protected Void doInBackground(Credentials... params) {
            try {
                if (getActivity() == null) {
                    return null;
                }
                User user = params[0].toUser();
                user.setId(makeAuthCall(user));
                UserManager.INSTANCE.saveUser(user);
            } catch (ChatException e) {
                mError = e;
            }
            return null;
        }

        int makeAuthCall(final User user) throws ChatException{
            return RemoteManager.INSTANCE.perform(getActivity(), new RemoteManager.RemoteCall<Integer>() {
                @Override
                public Integer call(Chat.Client client) throws TException {
                    return client.registerUser(user);
                }
            });
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            final AuthActivity activity = (AuthActivity) getActivity();
            if (activity != null) {
                activity.response(mError);
            }
        }
    }

    private static class LoginTask extends RegisterTask {

        private LoginTask(AuthActivity activity) {
            super(activity);
        }

        @Override
        int makeAuthCall(final User user) throws ChatException {
            return RemoteManager.INSTANCE.perform(getActivity(), new RemoteManager.RemoteCall<Integer>() {
                @Override
                public Integer call(Chat.Client client) throws TException {
                    return client.loginUser(user);
                }
            });
        }
    }

    private static class Credentials {

        CharSequence username;
        CharSequence password;

        boolean valid() {
            return username != null && password != null;
        }

        User toUser() {
            return valid() ? new User(0, username.toString(), UserManager.INSTANCE.md5(password.toString())) : new User();
        }
    }
}
