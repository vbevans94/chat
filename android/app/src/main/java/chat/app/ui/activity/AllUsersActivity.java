package chat.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import chat.app.R;
import chat.app.manager.utils.BundleUtils;
import chat.app.ui.fragment.AllUsersFragment;
import thrift.entity.User;

public class AllUsersActivity extends BaseActivity implements AllUsersFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onUserSelected(User user) {
        startActivity(new Intent(this, DialogsActivity.class)
                .putExtras(BundleUtils.writeToBundle(User.class, user)));
    }
}
