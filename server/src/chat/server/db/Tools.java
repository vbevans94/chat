package chat.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Tools {

    private Connection connection;
    private ResultSet resultSet;
    private Statement statement;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public Statement getStatement() {
        return statement;
    }

    public PreparedStatement getPreparedStatement() {
        return (PreparedStatement) statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public void close() {
        if (resultSet != null) try {
            resultSet.close();
        } catch (Exception ignore) {
        }

        if (connection != null) try {
            connection.close();
        } catch (Exception ignore) {
        }

        if (statement != null) try {
            statement.close();
        } catch (Exception ignore) {
        }
    }
}