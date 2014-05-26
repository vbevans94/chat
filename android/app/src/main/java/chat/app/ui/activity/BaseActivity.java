package chat.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.Window;

import chat.app.R;
import chat.app.manager.UserManager;

public class BaseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (!UserManager.INSTANCE.registered() && !(this instanceof AuthActivity)) {
            startActivity(new Intent(this, AuthActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.action_sign_out:
                UserManager.INSTANCE.clearSavedUser();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
