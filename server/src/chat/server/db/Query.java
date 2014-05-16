package chat.server.db;

import java.sql.SQLException;

public interface Query<T, E extends Exception> {

    T query(Tools tools) throws SQLException, E;
}