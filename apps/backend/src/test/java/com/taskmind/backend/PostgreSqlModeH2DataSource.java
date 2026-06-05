package com.taskmind.backend;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Adapts the two PostgreSQL partial indexes in the already-applied V5 migration for H2. H2's unique
 * indexes already permit multiple null values, so removing these predicates preserves their
 * behavior without changing the immutable production migration.
 */
public class PostgreSqlModeH2DataSource extends HikariDataSource {

    private static final Map<String, String> H2_EQUIVALENT_STATEMENTS =
            Map.of(
                    "CREATE UNIQUE INDEX ux_users_primary_email ON users (primary_email) WHERE primary_email IS NOT NULL",
                    "CREATE UNIQUE INDEX ux_users_primary_email ON users (primary_email)",
                    "CREATE UNIQUE INDEX ux_users_primary_phone ON users (primary_phone) WHERE primary_phone IS NOT NULL",
                    "CREATE UNIQUE INDEX ux_users_primary_phone ON users (primary_phone)");

    @Override
    public Connection getConnection() throws SQLException {
        return adapt(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return adapt(super.getConnection(username, password));
    }

    private Connection adapt(Connection connection) {
        return (Connection)
                Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class},
                        (proxy, method, args) -> {
                            try {
                                if (method.getName().equals("createStatement")
                                        && (args == null || args.length == 0)) {
                                    return adapt((Statement) method.invoke(connection));
                                }
                                return method.invoke(connection, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
    }

    private Statement adapt(Statement statement) {
        return (Statement)
                Proxy.newProxyInstance(
                        Statement.class.getClassLoader(),
                        new Class<?>[] {Statement.class},
                        (proxy, method, args) -> {
                            try {
                                if (args != null
                                        && args.length > 0
                                        && args[0] instanceof String sql) {
                                    args[0] =
                                            H2_EQUIVALENT_STATEMENTS.getOrDefault(sql.trim(), sql);
                                }
                                return method.invoke(statement, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
    }
}
