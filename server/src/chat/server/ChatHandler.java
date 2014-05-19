package chat.server;

import chat.server.db.DB;
import chat.server.db.Query;
import chat.server.db.Tools;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thrift.entity.*;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChatHandler implements Chat.Iface {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);

    @Override
    public int registerUser(final User user) throws TException {
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
                    tools.setStatement(tools.getConnection().prepareStatement("insert into users (username, passhash) values (?,?)", Statement.RETURN_GENERATED_KEYS));
                    tools.getPreparedStatement().setString(1, user.getUsername());
                    tools.getPreparedStatement().setString(2, user.getPasshash());
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
    public int loginUser(final User user) throws ChatException, TException {
        return DB.INSTANCE.run(new Query<Integer, ChatException>() {
            @Override
            public Integer query(Tools tools) throws SQLException, ChatException {
                tools.setStatement(tools.getConnection().prepareStatement("select id from users where (username = ?) and (passhash = ?)"));
                tools.getPreparedStatement().setString(1, user.getUsername());
                tools.getPreparedStatement().setString(2, user.getPasshash());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                if (tools.getResultSet().first()) {
                    return tools.getResultSet().getInt(1);
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
                String sql = "select ltm.created_at, ltm.data, ltm.author_id as partner_id, lfm.username, " +
                        "lfm.created_at, lfm.data from ((select m.created_at, m.data, m.author_id, m.receiver_id " +
                        "from messages m inner join (select author_id, max(created_at) as max_created_at from " +
                        "messages where receiver_id = ? group by author_id) last on (m.author_id = last.author_id) " +
                        "and m.created_at = last.max_created_at) ltm left join (select m.created_at, m.data, " +
                        "m.author_id, m.receiver_id, last.username as username from messages m inner join (select " +
                        "receiver_id, max(created_at) as max_created_at, users.username as username from messages, " +
                        "users where users.id = receiver_id and author_id = ? group by receiver_id) last on " +
                        "(m.receiver_id = last.receiver_id) and m.created_at = last.max_created_at) lfm on " +
                        "ltm.author_id = lfm.receiver_id) union (select ltm.created_at, ltm.data, ltm.author_id as " +
                        "partner_id, lfm.username, lfm.created_at, lfm.data from ((select m.created_at, m.data, " +
                        "m.author_id, m.receiver_id from messages m inner join (select author_id, max(created_at) as " +
                        "max_created_at from messages where receiver_id = ? group by author_id) last on (m.author_id = " +
                        "last.author_id) and m.created_at = last.max_created_at) ltm right join (select m.created_at, " +
                        "m.data, m.author_id, m.receiver_id, last.username as username from messages m inner join " +
                        "(select receiver_id, max(created_at) as max_created_at, users.username as username from " +
                        "messages, users where users.id = receiver_id and author_id = ? group by receiver_id) last on " +
                        "(m.receiver_id = last.receiver_id) and m.created_at = last.max_created_at) lfm on " +
                        "ltm.author_id = lfm.receiver_id));";
                tools.setStatement(tools.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.getPreparedStatement().setInt(2, user.getId());
                tools.getPreparedStatement().setInt(3, user.getId());
                tools.getPreparedStatement().setInt(4, user.getId());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                List<Dialog> dialogs = new ArrayList<Dialog>();
                if (tools.getResultSet().first()) {
                    for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                        Date ltmCreatedAt = tools.getResultSet().getDate(1);
                        String ltmData = tools.getResultSet().getString(2);
                        int partnerId = tools.getResultSet().getInt(3);
                        String partnerUsername = tools.getResultSet().getString(4);
                        Date lfmCreatedAt = tools.getResultSet().getDate(5);
                        String lfmData = tools.getResultSet().getString(6);

                        User partner = new User(partnerId, partnerUsername, null);
                        Message lastMessage = new Message();
                        if (ltmCreatedAt == null || lfmCreatedAt.compareTo(ltmCreatedAt) > 0) {
                            lastMessage.setData(lfmData);
                            lastMessage.setAuthor(user);
                            lastMessage.setCreatedAt(lfmCreatedAt.toString());
                        } else {
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
                String sql = " select created_at, data, author_id from messages where receiver_id = ? and author_id = ? " +
                        "or receiver_id = ? and author_id = ? order by created_at";
                tools.setStatement(tools.getConnection().prepareStatement(sql));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.getPreparedStatement().setInt(2, partner.getId());
                tools.getPreparedStatement().setInt(3, partner.getId());
                tools.getPreparedStatement().setInt(4, user.getId());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                List<Message> messages = new ArrayList<Message>();
                if (tools.getResultSet().first()) {
                    for (; !tools.getResultSet().isAfterLast(); tools.getResultSet().next()) {
                        String createdAt = tools.getResultSet().getDate(1).toString();
                        String data = tools.getResultSet().getString(2);
                        int authorId = tools.getResultSet().getInt(3);

                        User author = authorId == user.getId() ? user : partner;
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
                String sql = "insert into messages (author_id, receiver_id, data) values (?, ?, ?)";
                tools.setStatement(tools.getConnection().prepareStatement(sql));
                tools.getPreparedStatement().setInt(1, user.getId());
                tools.getPreparedStatement().setInt(2, dialog.getPartner().getId());
                tools.getPreparedStatement().setString(3, dialog.getLastMessage().getData());
                tools.getPreparedStatement().executeUpdate();

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

    /**
     * Validates user. If authentication fails exception is thrown.
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