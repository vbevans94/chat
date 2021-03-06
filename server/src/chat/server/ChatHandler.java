package chat.server;

import chat.server.db.DB;
import chat.server.db.Query;
import chat.server.db.Tools;
import com.google.android.gcm.server.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thrift.entity.*;
import thrift.entity.Message;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatHandler implements Chat.Iface {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);
    private static final String KEY_DATA = "data";
    private static final String KEY_AUTHOR_USERNAME = "author_username";
    private static final String KEY_AUTHOR_ID = "author_id";
    private static final String API_KEY = "AIzaSyASbDTp4yHLeywWzC7uW2A2EY7OLI3yRvw";
    private static final int MULTICAST_SIZE = 1000;
    private static final Executor THREAD_POOL = Executors.newFixedThreadPool(5);
    public static final String KEY_PUBLIC_MESSAGE = "public_message";
    private final Sender sender = new Sender(API_KEY);

    @Override
    public int registerUser(final User user, final String registerId) throws TException {
        return DB.INSTANCE.run(new Query<Integer, ChatException>() {
            @Override
            public Integer query(Tools tools) throws SQLException, ChatException {
                tools.setStatement(tools.getConnection().prepareStatement("select username from users where username = ?"));
                tools.getPreparedStatement().setString(1, user.getUsername());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                if (tools.getResultSet().first()) {
                    // there is user
                    String error = String.format("Can't register %s: User already exists.", user.getUsername());
                    LOGGER.error(error);

                    throw new ChatException(ErrorType.USER_ALREADY_EXISTS, error);
                } else {
                    // get user with given register id and unset it to empty for him
                    tools.setStatement(tools.getConnection().prepareStatement("update users set register_id = '' where register_id = ?"));
                    tools.getPreparedStatement().setString(1, registerId);
                    tools.getPreparedStatement().executeUpdate();

                    tools.setStatement(tools.getConnection().prepareStatement("insert into users (username, passhash, register_id) values (?,?,?)", Statement.RETURN_GENERATED_KEYS));
                    tools.getPreparedStatement().setString(1, user.getUsername());
                    tools.getPreparedStatement().setString(2, user.getPasshash());
                    tools.getPreparedStatement().setString(3, registerId);
                    tools.getPreparedStatement().executeUpdate();
                    tools.setResultSet(tools.getPreparedStatement().getGeneratedKeys());

                    String info = String.format("User %s registered!", user.getUsername());
                    LOGGER.info(info);
                    return tools.getResultSet().first() ? (int) tools.getResultSet().getLong(1) : 0;
                }
            }
        });
    }

    @Override
    public int loginUser(final User user, final String registerId) throws ChatException, TException {
        return DB.INSTANCE.run(new Query<Integer, ChatException>() {
            @Override
            public Integer query(Tools tools) throws SQLException, ChatException {
                tools.setStatement(tools.getConnection().prepareStatement("select id from users where (username = ?) and (passhash = ?)"));
                tools.getPreparedStatement().setString(1, user.getUsername());
                tools.getPreparedStatement().setString(2, user.getPasshash());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                if (tools.getResultSet().first()) {
                    int userId = tools.getResultSet().getInt(1);

                    // get user with given register id and unset it to empty for him
                    tools.setStatement(tools.getConnection().prepareStatement("update users set register_id = '' where register_id = ?"));
                    tools.getPreparedStatement().setString(1, registerId);
                    tools.getPreparedStatement().executeUpdate();

                    // set register id for currently logged in user
                    tools.setStatement(tools.getConnection().prepareStatement("update users set register_id = ? where id = ?"));
                    tools.getPreparedStatement().setString(1, registerId);
                    tools.getPreparedStatement().setInt(2, userId);
                    tools.getPreparedStatement().executeUpdate();

                    return userId;
                } else {
                    // there is user
                    String error = String.format("Can't login %s: Wrong credentials.", user.getUsername());
                    LOGGER.error(error);

                    throw new ChatException(ErrorType.USER_ALREADY_EXISTS, error);
                }
            }
        });
    }

    @Override
    public List<User> getAllUsers(final User user) throws ChatException, TException {
        authenticate(user);

        return DB.INSTANCE.run(new Query<List<User>, ChatException>() {
            @Override
            public List<User> query(Tools tools) throws SQLException, ChatException {
                // get all users except me
                tools.setStatement(tools.getConnection().prepareStatement("select id, username from users where (id != ?)"));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                List<User> users = new ArrayList<User>();
                if (tools.getResultSet().first()) {
                    for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                        int id = tools.getResultSet().getInt(1);
                        String username = tools.getResultSet().getString(2);
                        users.add(new User(id, username, null));
                    }
                }
                return users;
            }
        });
    }

    @Override
    public List<Dialog> getDialogs(final User user) throws ChatException, TException {
        authenticate(user);

        return DB.INSTANCE.run(new Query<List<Dialog>, ChatException>() {
            @Override
            public List<Dialog> query(Tools tools) throws SQLException, ChatException {
                // get all users except me
                String sql = "select ltm.created_at, ltm.data, ltm.author_id, ltm.username, lfm.created_at, lfm.data, " +
                        "lfm.receiver_id, lfm.username from ((select m.created_at, m.data, m.author_id, m.receiver_id, " +
                        "last.username as username from messages m inner join (select author_id, max(created_at) as " +
                        "max_created_at, users.username as username from messages, users where " +
                        "users.id = messages.author_id and receiver_id = ? group by author_id) last on " +
                        "(m.author_id = last.author_id) and m.created_at = last.max_created_at) ltm left join " +
                        "(select m.created_at, m.data, m.author_id, m.receiver_id, last.username as username from " +
                        "messages m inner join (select receiver_id, max(created_at) as max_created_at, users.username " +
                        "as username from messages, users where users.id = receiver_id and author_id = ? group by " +
                        "receiver_id) last on (m.receiver_id = last.receiver_id) and m.created_at = last.max_created_at) " +
                        "lfm on ltm.author_id = lfm.receiver_id) union select ltm.created_at, ltm.data, ltm.author_id, " +
                        "ltm.username, lfm.created_at, lfm.data, lfm.receiver_id, lfm.username from ((select m.created_at, " +
                        "m.data, m.author_id, m.receiver_id, last.username as username from messages m inner join " +
                        "(select author_id, max(created_at) as max_created_at, users.username as username from messages, " +
                        "users where users.id = messages.author_id and receiver_id = ? group by author_id) last on " +
                        "(m.author_id = last.author_id) and m.created_at = last.max_created_at) ltm right join " +
                        "(select m.created_at, m.data, m.author_id, m.receiver_id, last.username as username from " +
                        "messages m inner join (select receiver_id, max(created_at) as max_created_at, users.username " +
                        "as username from messages, users where users.id = receiver_id and author_id = ? group by " +
                        "receiver_id) last on (m.receiver_id = last.receiver_id) and m.created_at = last.max_created_at) " +
                        "lfm on ltm.author_id = lfm.receiver_id);";
                tools.setStatement(tools.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.getPreparedStatement().setInt(2, user.getId());
                tools.getPreparedStatement().setInt(3, user.getId());
                tools.getPreparedStatement().setInt(4, user.getId());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                List<Dialog> dialogs = new ArrayList<Dialog>();
                if (tools.getResultSet().first()) {
                    for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                        Timestamp ltmCreatedAt = tools.getResultSet().getTimestamp(1);
                        String ltmData = tools.getResultSet().getString(2);
                        int ltmPartnerId = tools.getResultSet().getInt(3);
                        String ltmPartnerUsername = tools.getResultSet().getString(4);
                        Timestamp lfmCreatedAt = tools.getResultSet().getTimestamp(5);
                        String lfmData = tools.getResultSet().getString(6);
                        int lfmPartnerId = tools.getResultSet().getInt(7);
                        String lfmPartnerUsername = tools.getResultSet().getString(8);

                        User partner;
                        Message lastMessage = new Message();
                        if (ltmCreatedAt == null || lfmCreatedAt != null && lfmCreatedAt.compareTo(ltmCreatedAt) > 0) {
                            partner = new User(lfmPartnerId, lfmPartnerUsername, null);
                            lastMessage.setData(lfmData);
                            lastMessage.setAuthor(user);
                            lastMessage.setCreatedAt(lfmCreatedAt.toString());
                        } else {
                            partner = new User(ltmPartnerId, ltmPartnerUsername, null);
                            lastMessage.setData(ltmData);
                            lastMessage.setAuthor(partner);
                            lastMessage.setCreatedAt(ltmCreatedAt.toString());
                        }
                        Dialog dialog = new Dialog(partner, lastMessage);
                        dialogs.add(dialog);
                    }
                }
                return dialogs;
            }
        });
    }

    @Override
    public List<Message> getMessages(final User user, final User partner) throws ChatException, TException {
        authenticate(user);

        return DB.INSTANCE.run(new Query<List<Message>, ChatException>() {
            @Override
            public List<Message> query(Tools tools) throws SQLException, ChatException {
                if (partner != null) {
                    String sql = "select created_at, data, author_id, users.username from messages, users where " +
                            "users.id = messages.author_id and (receiver_id = ? and author_id = ? " +
                            "or receiver_id = ? and author_id = ?) order by created_at";
                    tools.setStatement(tools.getConnection().prepareStatement(sql));
                    tools.getPreparedStatement().setInt(1, user.getId());
                    tools.getPreparedStatement().setInt(2, partner.getId());
                    tools.getPreparedStatement().setInt(3, partner.getId());
                    tools.getPreparedStatement().setInt(4, user.getId());
                } else {
                    String sql = " select created_at, data, author_id, users.username from messages, users where " +
                            "users.id = messages.author_id and isnull(receiver_id) order by created_at";
                    tools.setStatement(tools.getConnection().prepareStatement(sql));
                }
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                List<Message> messages = new ArrayList<Message>();
                if (tools.getResultSet().first()) {
                    for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                        String createdAt = tools.getResultSet().getTimestamp(1).toString();
                        String data = tools.getResultSet().getString(2);
                        int authorId = tools.getResultSet().getInt(3);
                        String authorUsername = tools.getResultSet().getString(4);
                        User author = new User(authorId, authorUsername, null);
                        messages.add(new Message(data, author, createdAt));
                    }
                }
                return messages;
            }
        });
    }

    @Override
    public List<Message> sendMessage(final User user, final Dialog dialog) throws ChatException, TException {
        authenticate(user);

        return DB.INSTANCE.run(new Query<List<Message>, ChatException>() {
            @Override
            public List<Message> query(Tools tools) throws SQLException, ChatException {
                // inserting message
                if (dialog.getPartner() != null) {
                    String sql = "insert into messages (author_id, receiver_id, data) values (?, ?, ?)";
                    tools.setStatement(tools.getConnection().prepareStatement(sql));
                    tools.getPreparedStatement().setInt(1, user.getId());
                    tools.getPreparedStatement().setInt(2, dialog.getPartner().getId());
                    tools.getPreparedStatement().setString(3, dialog.getLastMessage().getData());
                } else {
                    // public message
                    String sql = "insert into messages (author_id, data) values (?, ?)";
                    tools.setStatement(tools.getConnection().prepareStatement(sql));
                    tools.getPreparedStatement().setInt(1, user.getId());
                    tools.getPreparedStatement().setString(2, dialog.getLastMessage().getData());
                }
                tools.getPreparedStatement().executeUpdate();

                List<String> receivers = new ArrayList<String>();
                boolean publicMessage = false;
                if (dialog.getPartner() != null) {
                    // get partner's gcm id to notify him
                    tools.setStatement(tools.getConnection().prepareStatement("select register_id from users where id = ?"));
                    tools.getPreparedStatement().setInt(1, dialog.getPartner().getId());
                    tools.setResultSet(tools.getPreparedStatement().executeQuery());
                    String gcmId = "";
                    if (tools.getResultSet().first()) {
                        gcmId = tools.getResultSet().getString(1);
                    }

                    if (gcmId != null && !gcmId.isEmpty()) {
                        receivers.add(gcmId);
                    }
                } else {
                    // notify every body
                    publicMessage = true;
                    tools.setStatement(tools.getConnection().prepareStatement("select register_id from users"));
                    tools.setResultSet(tools.getPreparedStatement().executeQuery());

                    if (tools.getResultSet().first()) {
                        for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                            String gcmId = tools.getResultSet().getString(1);
                            if (gcmId != null && !gcmId.isEmpty()) {
                                receivers.add(gcmId);
                            }
                        }
                    }
                }

                // for partners it's not the same as for me
                User author = new User(user.getId(), user.getUsername(), null);
                Message message = new Message(dialog.getLastMessage().getData(), author, dialog.getLastMessage().getCreatedAt());
                // must split in chunks of 1000 devices (GCM limit)
                int total = receivers.size();
                List<String> partialReceivers = new ArrayList<String>(total);
                int counter = 0;
                int tasks = 0;
                for (String receiver : receivers) {
                    counter++;
                    partialReceivers.add(receiver);
                    int partialSize = partialReceivers.size();
                    if (partialSize == MULTICAST_SIZE || counter == total) {
                        asyncNotify(partialReceivers, message, tools, publicMessage);
                        partialReceivers.clear();
                        tasks++;
                    }
                }
                LOGGER.info("Asynchronously sending " + tasks + " multi-cast messages to " + total + " devices");

                try {
                    return getMessages(user, dialog.getPartner());
                } catch (TException e) {
                    if (e instanceof ChatException) {
                        throw (ChatException) e;
                    }
                    return null;
                }
            }
        });
    }

    private void asyncNotify(List<String> receivers, final Message msg, final Tools tools, final boolean publicMessage) {
        final List<String> partialReceivers = new ArrayList<String>(receivers);
        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    notifyReceivers(partialReceivers, msg, tools, publicMessage);
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyReceivers(List<String> receivers, Message msg, Tools tools, boolean publicMessage) throws SQLException {
        com.google.android.gcm.server.Message.Builder builder = new com.google.android.gcm.server.Message.Builder()
                .addData(KEY_DATA, msg.getData())
                .addData(KEY_AUTHOR_USERNAME, msg.getAuthor().getUsername())
                .addData(KEY_AUTHOR_ID, Integer.toString(msg.getAuthor().getId()));
        if (publicMessage) {
            builder.addData(KEY_PUBLIC_MESSAGE, "1"); // just need to be present
        }
        MulticastResult multicastResult;
        try {
            multicastResult = sender.send(builder.build(), receivers, 5);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }
        List<Result> results = multicastResult.getResults();
        int position = 0;

        for (Result result : results) {
            String regId = receivers.get(position);
            String messageId = result.getMessageId();
            if (messageId != null) {
                LOGGER.info("Successfully sent messageId = " + messageId);
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // same device has more than on registration id:
                    // update it
                    tools.setStatement(tools.getConnection().prepareStatement("update users set register_id = ? where register_id = ?"));
                    tools.getPreparedStatement().setString(1, regId);
                    tools.getPreparedStatement().setString(2, canonicalRegId);
                    tools.getPreparedStatement().executeUpdate();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    // application has been removed from device -
                    // unregister it
                    tools.setStatement(tools.getConnection().prepareStatement("update users set register_id = '' where register_id = ?"));
                    tools.getPreparedStatement().setString(1, regId);
                    tools.getPreparedStatement().executeUpdate();
                } else {
                    LOGGER.error("Error sending message to " + regId + ": " + error);
                }
            }
        }
    }


    /**
     * Validates user. If authentication fails exception is thrown.
     *
     * @param user to validate
     * @throws ChatException when there is no such user
     */
    public void authenticate(final User user) throws ChatException {
        DB.INSTANCE.run(new Query<Void, ChatException>() {
            @Override
            public Void query(Tools tools) throws SQLException, ChatException {
                tools.setStatement(tools.getConnection().prepareStatement("select id from users where (id = ?) and (username = ?) and (passhash = ?)"));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.getPreparedStatement().setString(2, user.getUsername());
                tools.getPreparedStatement().setString(3, user.getPasshash());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                if (!tools.getResultSet().first()) {
                    // there is no such user
                    String error = String.format("Authentication failed for user %s.", user.getUsername());
                    LOGGER.error(error);
                    throw new ChatException(ErrorType.NO_SUCH_USER, error);
                }
                return null;
            }
        });
    }
}