package chat.app.ui.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
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
import chat.app.manager.utils.BundleUtils;
import chat.app.manager.utils.TaskUtils;
import chat.app.ui.adapter.DialogsAdapter;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.Dialog;
import thrift.entity.Message;
import thrift.entity.User;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class DialogsDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private DialogCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;

    @InjectView(R.id.list_drawer_dialogs)
    ListView mListDialogs;

    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dialogs_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);
    }

    @OnItemClick(R.id.list_drawer_dialogs)
    @SuppressWarnings("unused")
    void onDialogClicked(int position) {
        selectItem(position);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mListDialogs.getAdapter() == null) {
            requestDialogs();
        }
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     * @param extras The extras received in activity. Commonly here goes user to start dialog with.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout, Bundle extras) {
        mUser = BundleUtils.fetchFromBundle(User.class, extras);

        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mListDialogs != null) {
            mListDialogs.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null && mListDialogs != null) {
            Dialog dialog = (Dialog) mListDialogs.getItemAtPosition(position);
            mCallbacks.onDialogSelected(dialog);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (DialogCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            requestDialogs();
        }
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void requestDialogs() {
        TaskUtils.schedule(new GetDialogsTask(this), UserManager.INSTANCE.getSavedUser());
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface DialogCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onDialogSelected(Dialog dialog);

        /**
         * Called when there is no dialogs for current user.
         */
        void onNoDialogs();
    }

    private static class GetDialogsTask extends TaskUtils.BaseTask<User, Void, List<Dialog>> {

        private final WeakReference<DialogsDrawerFragment> mFragmentRef;
        private ChatException mError;

        public GetDialogsTask(DialogsDrawerFragment fragment) {
            super((ActionBarActivity) fragment.getActivity());
            mFragmentRef = new WeakReference<DialogsDrawerFragment>(fragment);
        }

        /**
         * Makes call in worker thread.
         * @param params username and password
         * @return null if registration succeeded and string error message otherwise
         */
        @Override
        protected List<Dialog> doInBackground(User... params) {
            try {
                Fragment fragment = mFragmentRef.get();
                if (fragment == null) {
                    return null;
                }
                final User user = params[0];
                return RemoteManager.INSTANCE.perform(fragment.getActivity(), new RemoteManager.RemoteCall<List<Dialog>>() {
                    @Override
                    public List<Dialog> call(Chat.Client client) throws TException {
                        return client.getDialogs(user);
                    }
                });
            } catch (ChatException e) {
                mError = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Dialog> dialogs) {
            super.onPostExecute(dialogs);

            final DialogsDrawerFragment fragment = mFragmentRef.get();
            if (fragment != null && fragment.isAdded()) {
                if (dialogs != null) {
                    if (fragment.mUser != null) {
                        dialogs.add(0, new Dialog(fragment.mUser, new Message()));
                    }
                    if (dialogs.size() == 0) {
                        // there are no dialogs yet, hence nothing to do here
                        fragment.mCallbacks.onNoDialogs();
                    } else {
                        fragment.mListDialogs.setAdapter(new DialogsAdapter(fragment.getActivity(), dialogs));
                        fragment.mListDialogs.setItemChecked(fragment.mCurrentSelectedPosition, true);
                        fragment.mDrawerLayout.openDrawer(fragment.mFragmentContainerView);
                    }
                } else if (mError != null) {
                    Toast.makeText(fragment.getActivity(), mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
