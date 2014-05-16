package chat.app.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import chat.app.ui.SettingsActivity;
import thrift.entity.Chat;
import thrift.entity.ChatException;

public enum RemoteManager {

    INSTANCE;

    private static final String TAG = RemoteManager.class.getSimpleName();

    public <T> T perform(Context context, RemoteCall<T> remoteCall) throws ChatException{
        T result = null;
        try {
            String ipPref = SettingsActivity.DEFAULT_IP;
            int portPref = Integer.parseInt(SettingsActivity.DEFAULT_PORT);
            if (context != null) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                ipPref = sharedPref.getString(SettingsActivity.KEY_PREF_IP, SettingsActivity.DEFAULT_IP);
                portPref = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_PORT, SettingsActivity.DEFAULT_PORT));
            }
            TTransport transport = new TSocket(ipPref, portPref);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Chat.Client client = new Chat.Client(protocol);
            result = remoteCall.call(client);
            transport.close();
        } catch (TException e) {
            Log.e(TAG, "Exception during remote call.", e);
            if (e instanceof ChatException) {
                throw (ChatException) e;
            }
        }
        return result;
    }

    public interface RemoteCall<T> {

        T call(Chat.Client client) throws TException;
    }
}
