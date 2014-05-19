package chat.app.manager.gcm;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

import chat.app.manager.InfoManager;
import chat.app.manager.LocalStorage;

public enum GcmManager {

    INSTANCE;

    private static final String SENDER_ID = "8454848996";
    private static final String TAG = GcmManager.class.getSimpleName();

    private static final String KEY_GCM_REGISTER_ID = "gcm_register_id";
    private static final String KEY_GCM_REGISTERED_VERSION = "gcm_registered_version";

    private GoogleCloudMessaging mGcm;

    public void init(Context context) {
        mGcm = GoogleCloudMessaging.getInstance(context);
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getSavedGsmId() {
        String registrationId = LocalStorage.INSTANCE.getString(KEY_GCM_REGISTER_ID);
        if (TextUtils.isEmpty(registrationId)) {
            Log.d(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = LocalStorage.INSTANCE.getInt(KEY_GCM_REGISTERED_VERSION);
        int currentVersion = InfoManager.INSTANCE.getVersionCode();
        if (registeredVersion != currentVersion) {
            Log.d(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param registerId registration ID
     */
    public void saveGcmId(String registerId) {
        int appVersion = InfoManager.INSTANCE.getVersionCode();
        LocalStorage.INSTANCE.setString(KEY_GCM_REGISTER_ID, registerId);
        LocalStorage.INSTANCE.setInt(KEY_GCM_REGISTERED_VERSION, appVersion);
    }

    /**
     * Removes registration ID from the device. Along with the app version.
     */
    public void removeGcmId() {
        LocalStorage.INSTANCE.remove(KEY_GCM_REGISTER_ID);
        LocalStorage.INSTANCE.remove(KEY_GCM_REGISTERED_VERSION);
    }

    public String getGcmId() throws IOException {
        return mGcm.register(SENDER_ID);
    }
}
