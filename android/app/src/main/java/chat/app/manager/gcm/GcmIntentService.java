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
import chat.app.ui.activity.AllUsersActivity;
import chat.app.ui.activity.DialogsActivity;
import thrift.entity.Message;
import thrift.entity.User;

public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 30194;
    public static final String KEY_AUTHOR_ID = "author_id";
    public static final String KEY_AUTHOR_USERNAME = "author_username";
    public static final String KEY_DATA = "data";
    public static final String KEY_PUBLIC_MESSAGE = "public_message";

    public GcmIntentService() {
        super("ChatGcmIntentService");
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

        PendingIntent contentIntent;
        if (BundleUtils.contains(msg, KEY_PUBLIC_MESSAGE)) {
            contentIntent = PendingIntent.getActivity(this
                    , (int) System.currentTimeMillis()
                    , new Intent(this, AllUsersActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    , 0);
        } else {
            contentIntent = PendingIntent.getActivity(this
                    , (int) System.currentTimeMillis()
                    , new Intent(this, DialogsActivity.class)
                    .putExtras(msg)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    , 0);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(msg.getString(KEY_AUTHOR_USERNAME))
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentText(msg.getString(KEY_DATA))
                        .setAutoCancel(true);

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Retrieves message sent from user and dismisses the notification.
     * @param extras to get from
     * @return message object
     */
    public static Message getMessage(Bundle extras) {
        // it's from the notification
        int id = Integer.parseInt(BundleUtils.getString(extras, GcmIntentService.KEY_AUTHOR_ID));
        String username = BundleUtils.getString(extras, GcmIntentService.KEY_AUTHOR_USERNAME);
        String data = BundleUtils.getString(extras, GcmIntentService.KEY_DATA);
        User user = new User(id, username, null);
        return new Message(data, user, null);
    }
}