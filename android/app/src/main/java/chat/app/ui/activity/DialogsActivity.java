package chat.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import chat.app.R;
import chat.app.manager.UserManager;
import chat.app.ui.fragment.DialogFragment;
import chat.app.ui.fragment.DialogsDrawerFragment;
import thrift.entity.Dialog;

public class DialogsActivity extends ActionBarActivity
        implements DialogsDrawerFragment.DialogCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DialogsDrawerFragment mDialogsDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dialogs);

        mDialogsDrawerFragment = (DialogsDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mDialogsDrawerFragment.setUp(R.id.navigation_drawer
                , (DrawerLayout) findViewById(R.id.drawer_layout)
                , getIntent().getExtras());

        if (!UserManager.INSTANCE.registered()) {
            startActivity(new Intent(this, AuthActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    public void onDialogSelected(Dialog dialog) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, DialogFragment.newInstance(dialog))
                .commit();
    }

    @Override
    public void onNoDialogs() {
        // when there are dialogs, nothing to do here
        startActivity(new Intent(this, AllUsersActivity.class));
        finish();
    }

    public void onDialogAttached(Dialog dialog) {
        mTitle = dialog.getPartner().getUsername();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDialogsDrawerFragment.isDrawerOpen()) {
            // when drawer is open, it should decide what to show
            getMenuInflater().inflate(R.menu.menu_dialogs, menu);
            getMenuInflater().inflate(R.menu.menu_settings, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_all_users:
                onNoDialogs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}