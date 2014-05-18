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

import java.util.ArrayList;
import java.util.List;

import chat.app.ui.activity.SettingsActivity;
import thrift.entity.Chat;
import thrift.entity.ChatException;
import thrift.entity.ErrorType;

public enum RemoteManager {

    INSTANCE;

    private static final String TAG = RemoteManager.class.getSimpleName();
    private final List<ExceptionHandler> mExceptionHandlers = new ArrayList<ExceptionHandler>();

    public <T> T perform(Context context, RemoteCall<T> remoteCall) throws ChatException{
        T result;
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
            ChatException chatException;
            if (e instanceof ChatException) {
                chatException = (ChatException) e;
            } else {
                // any of connection errors
                chatException = new ChatException(ErrorType.SYSTEM_ERROR, "Connection failed");
            }
            // propagate through exception handlers
            for (ExceptionHandler handler : mExceptionHandlers) {
                handler.handleError(chatException);
            }
            throw chatException;
        }
        return result;
    }

    public void addExceptionHandler(ExceptionHandler handler) {
        mExceptionHandlers.add(handler);
    }

    public void removeExceptionHandler(ExceptionHandler handler) {
        mExceptionHandlers.remove(handler);
    }

    public interface ExceptionHandler {

        void handleError(ChatException e);
    }

    public interface RemoteCall<T> {

        T call(Chat.Client client) throws TException;
    }
}
