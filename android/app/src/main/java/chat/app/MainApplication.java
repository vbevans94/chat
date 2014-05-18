package chat.app;

import android.app.Application;

import chat.app.manager.LocalStorage;
import chat.app.manager.UserManager;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    private void init() {
        LocalStorage.INSTANCE.init(getApplicationContext());
        UserManager.INSTANCE.init();
    }
}
