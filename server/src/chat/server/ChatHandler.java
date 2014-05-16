package chat.server;

import chat.server.db.DB;
import chat.server.db.Query;
import chat.server.db.Tools;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thrift.entity.*;

import java.sql.SQLException;
import java.util.List;

public class ChatHandler implements Chat.Iface {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);

    @Override
    public void registerUser(final User user) throws TException {
        DB.INSTANCE.run(new Query<Boolean, ChatException>() {
            @Override
            public Boolean query(Tools tools) throws SQLException, ChatException {
                tools.setStatement(tools.getConnection().prepareStatement("select username from users where username = ?"));
                tools.getPreparedStatement().setString(1, user.getUsername());
                tools.setResultSet(tools.getPreparedStatement().executeQuery());
                if (tools.getResultSet().first()) {
                    // there is user
                    String error = String.format("Can't register %s: User already exists.", user.toString());
                    LOGGER.error(error);

                    throw new ChatException(ErrorType.USER_ALREADY_EXISTS, error);
                } else {
                    tools.setStatement(tools.getConnection().prepareStatement("insert into users (username, passhash) values (?,?)"));
                    tools.getPreparedStatement().setString(1, user.getUsername());
                    tools.getPreparedStatement().setString(2, user.getPasshash());
                    tools.getPreparedStatement().executeUpdate();

                    String info = String.format("User %s registered!", user.getUsername());
                    LOGGER.info(info);
                }
                return null;
            }
        });
    }

    @Override
    public List<User> getAllUsers(User user) throws ChatException, TException {
        return null;
    }

    @Override
    public List<Dialog> getDialogs(User user) throws ChatException, TException {
        return null;
    }

    @Override
    public List<Message> getMessages(User user, Dialog dialog) throws ChatException, TException {
        return null;
    }

    @Override
    public List<Message> sendMessage(User user, Dialog dialog) throws ChatException, TException {
        return null;
    }
}