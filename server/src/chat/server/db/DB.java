package chat.server.db;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.SQLException;

public enum DB {

    INSTANCE;

    private final PoolProperties properties = new PoolProperties();
    private final DataSource datasource = new DataSource();

    {
        properties.setUrl("jdbc:mysql://localhost:3306/chat");
        properties.setDriverClassName("com.mysql.jdbc.Driver");
        properties.setUsername("sqluser");
        properties.setPassword("sqlpassword");
        properties.setTestWhileIdle(false);
        properties.setTestOnBorrow(true);
        properties.setValidationQuery("SELECT 1");
        properties.setTestOnReturn(false);
        properties.setValidationInterval(30000);
        properties.setTimeBetweenEvictionRunsMillis(30000);
        properties.setMaxActive(100);
        properties.setInitialSize(10);
        properties.setMaxWait(10000);
        properties.setRemoveAbandonedTimeout(60);
        properties.setMinEvictableIdleTimeMillis(30000);
        properties.setMinIdle(10);
        properties.setLogAbandoned(true);
        properties.setRemoveAbandoned(true);

        datasource.setPoolProperties(properties);
    }

    public <T, E extends Exception> T run(Query<T, E> query) throws E {


        Tools tools = new Tools();
        T result = null;
        try {
            tools.setConnection(datasource.getConnection());
            result = query.query(tools);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            tools.close();
        }

        return result;
    }
}
