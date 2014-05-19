package chat.app;

import android.app.Application;

import chat.app.manager.InfoManager;
import chat.app.manager.LocalStorage;
import chat.app.manager.UserManager;
import chat.app.manager.gcm.GcmManager;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    private void init() {
        LocalStorage.INSTANCE.init(this);
        InfoManager.INSTANCE.init(this);
        GcmManager.INSTANCE.init(this);
        UserManager.INSTANCE.init();
    }
}
