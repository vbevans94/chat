package chat.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import chat.app.R;
import chat.app.ui.fragment.DialogsDrawerFragment;
import chat.app.ui.fragment.MessagesFragment;
import thrift.entity.Dialog;

public class DialogsActivity extends BaseActivity
        implements DialogsDrawerFragment.DialogCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DialogsDrawerFragment mDialogsDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dialogs);

        mDialogsDrawerFragment = (DialogsDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mDialogsDrawerFragment.setUp(R.id.navigation_drawer
                , (DrawerLayout) findViewById(R.id.drawer_layout)
                , getIntent().getExtras());
    }

    @Override
    public void onDialogSelected(Dialog dialog) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, MessagesFragment.newInstance(dialog))
                .commit();
    }

    @Override
    public void onNoDialogs() {
        // when there are dialogs, nothing to do here
        startActivity(new Intent(this, AllUsersActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_users, menu);
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_all_users:
                onNoDialogs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
