package chat.app.manager.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import chat.app.R;
import chat.app.manager.utils.BundleUtils;
import chat.app.ui.activity.DialogsActivity;
import thrift.entity.User;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    public static final String KEY_MESSAGE_AUTHOR = "message_author";
    public static final String KEY_MESSAGE_DATA = "message_data";

    public GcmIntentService() {
        super("ArtGcmIntentService");
    }

    public static final String TAG = GcmIntentService.class.getSimpleName();

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (extras != null && !extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                showNotification(extras);
                Log.d(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    /**
     * Creates push notification of the messsage.
     * @param msg tto be pushed
     */
    private void showNotification(Bundle msg) {
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DialogsActivity.class)
                        .putExtras(BundleUtils.writeToBundle(User.class, new User(0, msg.getString(KEY_MESSAGE_AUTHOR), null)))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.getString(KEY_MESSAGE_AUTHOR)))
                        .setContentText(msg.getString(KEY_MESSAGE_DATA));

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}