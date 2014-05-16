package chat.app.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.thrift.TException;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import chat.app.R;
import chat.app.manager.RemoteManager;
import chat.app.manager.UserManager;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.User;

public class RegisterActivity extends ActionBarActivity implements DialogInterface.OnClickListener {

    @InjectView(R.id.edit_name)
    EditText mEditName;
    @InjectView(R.id.edit_password)
    EditText mEditPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ButterKnife.inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return true;
    }

    @OnClick(R.id.button_register)
    @SuppressWarnings("unused")
    void onRegisterClicked() {
        String username = null;
        String password = null;
        if (!TextUtils.isEmpty(mEditName.getText())) {
            username = mEditName.getText().toString();
        } else {
            mEditName.setError(getString(R.string.error_enter_name));
        }

        if (!TextUtils.isEmpty(mEditPassword.getText())) {
            password = mEditPassword.getText().toString();
        } else {
            mEditName.setError(getString(R.string.error_enter_password));
        }

        if (username != null && password != null) {
            new RegisterTask(this).execute(username, password);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

    private void response(String errorMessage) {
        if (errorMessage != null) {
            mEditName.setError(errorMessage);
        } else {
            Toast.makeText(this, R.string.message_welcome, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static class RegisterTask extends AsyncTask<String, Void, String> {

        private final WeakReference<RegisterActivity> mActivityRef;

        public RegisterTask(RegisterActivity activity) {
            mActivityRef = new WeakReference<RegisterActivity>(activity);
        }

        /**
         * Makes call in worker thread.
         * @param params username and password
         * @return null if registration succeeded and string error message otherwise
         */
        @Override
        protected String doInBackground(String... params) {
            try {
                String username = params[0];
                String password = params[1];
                final User user = new User(username, UserManager.INSTANCE.md5(password));
                RemoteManager.INSTANCE.perform(mActivityRef.get(), new RemoteManager.RemoteCall<Void>() {
                    @Override
                    public Void call(Chat.Client client) throws TException {
                        client.registerUser(user);
                        UserManager.INSTANCE.saveUser(user);
                        return null;
                    }
                });
            } catch (ChatException e) {
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String errorMessage) {
            final RegisterActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.response(errorMessage);
            }
        }
    }
}
