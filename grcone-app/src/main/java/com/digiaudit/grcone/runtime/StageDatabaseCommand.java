package com.digiaudit.grcone.runtime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;

/** One-shot Oracle maintenance commands used by the deployment runners. */
public final class StageDatabaseCommand {

    private static final String CHECK_COMMAND = "--stage-db-check";
    private static final String INIT_COMMAND = "--stage-db-init";
    private static final String SCHEMA_COPY_CHECK_COMMAND = "--schema-copy-check";
    private static final Pattern ORACLE_SCHEMA = Pattern.compile("[A-Z][A-Z0-9_$#]{0,127}");

    private StageDatabaseCommand() {
    }

    public static boolean handles(String[] args) {
        return args.length == 1 && (CHECK_COMMAND.equals(args[0]) || INIT_COMMAND.equals(args[0])
                || SCHEMA_COPY_CHECK_COMMAND.equals(args[0]));
    }

    public static void run(String command) {
        boolean checking = CHECK_COMMAND.equals(command);
        boolean checkingSchemaCopy = SCHEMA_COPY_CHECK_COMMAND.equals(command);
        String url = requiredEnv("DB_URL");
        String username = requiredEnv("DB_USERNAME");
        String password = requiredEnv("DB_PASSWORD");
        String schema = requiredEnv("DB_SCHEMA").toUpperCase(Locale.ROOT);
        if (!ORACLE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("DB_SCHEMA is not an ordinary Oracle schema name");
        }
        if (!url.startsWith("jdbc:oracle:")) {
            throw new IllegalArgumentException("Stage database commands require an Oracle JDBC URL");
        }

        if (checking) {
            checkLog("Connecting to Oracle with the configured stage credentials (credentials are not logged)");
        }
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (checking) {
                checkLog("Connection established");
            }
            verifySchemaOwner(connection, schema, checking);
            if (checkingSchemaCopy) {
                checkSchemaCopyPrivileges(connection, schema);
            } else if (checking) {
                checkCrud(connection);
                checkLog("SUCCESS: connection, schema ownership, CRUD and table cleanup verified");
            } else if (INIT_COMMAND.equals(command)) {
                if (!"RESET".equals(System.getenv("GRCONE_STAGE_DB_INIT_CONFIRMED"))) {
                    throw new IllegalStateException("Stage database initialization needs explicit RESET confirmation");
                }
                if (!"true".equalsIgnoreCase(requiredEnv("FLYWAY_ENABLED"))) {
                    throw new IllegalStateException("FLYWAY_ENABLED must be true for stage database initialization");
                }
                Flyway flyway = Flyway.configure()
                        .dataSource(url, username, password)
                        .defaultSchema(schema)
                        .schemas(schema)
                        .locations("classpath:db/migration/common", "classpath:db/migration/oracle")
                        .cleanDisabled(false)
                        .load();
                System.out.println("Cleaning Oracle schema " + schema + "...");
                flyway.clean();
                System.out.println("Applying Flyway migrations from the beginning...");
                flyway.migrate();
                System.out.println("Oracle schema " + schema + " initialized successfully.");
            } else {
                throw new IllegalArgumentException("Unsupported stage database command");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Stage Oracle operation failed: " + e.getMessage(), e);
        }
    }

    private static void checkSchemaCopyPrivileges(Connection connection, String schema) throws SQLException {
        System.out.println("Connected Oracle schema: " + schema);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT
                         (SELECT COUNT(*) FROM SESSION_PRIVS WHERE PRIVILEGE = 'CREATE USER'),
                         (SELECT COUNT(*) FROM SESSION_ROLES WHERE ROLE = 'DATAPUMP_IMP_FULL_DATABASE'),
                         (SELECT COUNT(*) FROM ALL_USERS WHERE USERNAME = 'GRCONE'),
                         (SELECT COUNT(*) FROM ALL_DIRECTORIES WHERE DIRECTORY_NAME = 'DATA_PUMP_DIR')
                     FROM DUAL
                     """)) {
            result.next();
            boolean createUser = result.getInt(1) > 0;
            boolean importAcrossSchemas = result.getInt(2) > 0;
            boolean targetExists = result.getInt(3) > 0;
            boolean directoryVisible = result.getInt(4) > 0;
            System.out.println("CREATE USER: " + createUser);
            System.out.println("DATAPUMP_IMP_FULL_DATABASE: " + importAcrossSchemas);
            System.out.println("Target user GRCONE exists: " + targetExists);
            System.out.println("DATA_PUMP_DIR visible to this connection: " + directoryVisible);
            if ("GRCONE".equals(schema)) {
                System.out.println("The application already connects to GRCONE.");
            } else if (!targetExists && !createUser) {
                System.out.println("This connection cannot create the target Oracle user GRCONE.");
            } else {
                System.out.println("Before copying, verify the target is empty and confirm Data Pump directory access.");
            }
        }
        System.out.println("Read-only check complete.");
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required for stage database commands");
        }
        return value;
    }

    private static void verifySchemaOwner(Connection connection, String schema, boolean checking) throws SQLException {
        String sessionSql = "SELECT SYS_CONTEXT('USERENV','SESSION_USER'), "
                + "SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL";
        if (checking) {
            checkSql(sessionSql);
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sessionSql)) {
            result.next();
            String sessionUser = result.getString(1);
            String currentSchema = result.getString(2);
            if (checking) {
                checkLog("ROW: SESSION_USER='" + sessionUser + "', CURRENT_SCHEMA='" + currentSchema + "'");
            }
            if (!schema.equalsIgnoreCase(sessionUser) || !schema.equalsIgnoreCase(currentSchema)) {
                throw new IllegalStateException(
                        "DB_SCHEMA must be the connected Oracle user's own current schema");
            }
            if ("SYS".equals(schema) || "SYSTEM".equals(schema)) {
                throw new IllegalStateException("Refusing a database maintenance command on a system schema");
            }
        }
        if (checking) {
            checkSql("SELECT ORACLE_MAINTAINED FROM ALL_USERS WHERE USERNAME = ?");
            checkLog("BIND: USERNAME='" + schema + "'");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ORACLE_MAINTAINED FROM ALL_USERS WHERE USERNAME = ?")) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                String maintained = result.next() ? result.getString(1) : null;
                if (checking) {
                    checkLog("ROW: ORACLE_MAINTAINED=" + (maintained == null ? "<none>" : "'" + maintained + "'"));
                }
                if (!"N".equals(maintained)) {
                    throw new IllegalStateException("Refusing a database maintenance command on an Oracle-maintained schema");
                }
            }
        }
    }

    private static void checkCrud(Connection connection) throws SQLException {
        String table = "GRCONE_CHECK_" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        boolean created = false;
        SQLException failure = null;
        try (Statement statement = connection.createStatement()) {
            String createSql = "CREATE TABLE " + table
                    + " (ID NUMBER(10) PRIMARY KEY, CHECK_VALUE VARCHAR2(100 CHAR))";
            checkSql(createSql);
            statement.execute(createSql);
            created = true;
            checkLog("RESULT: table created");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + table + " (ID, CHECK_VALUE) VALUES (?, ?)")) {
                insert.setInt(1, 1);
                insert.setString(2, "created");
                checkSql("INSERT INTO " + table + " (ID, CHECK_VALUE) VALUES (?, ?)");
                checkLog("BIND: ID=1, CHECK_VALUE='created'");
                int inserted = insert.executeUpdate();
                checkLog("RESULT: affected rows=" + inserted);
                requireOneRow(inserted, "insert");
            }
            requireValue(connection, table, "created");
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE " + table + " SET CHECK_VALUE = ? WHERE ID = ?")) {
                update.setString(1, "updated");
                update.setInt(2, 1);
                checkSql("UPDATE " + table + " SET CHECK_VALUE = ? WHERE ID = ?");
                checkLog("BIND: CHECK_VALUE='updated', ID=1");
                int updated = update.executeUpdate();
                checkLog("RESULT: affected rows=" + updated);
                requireOneRow(updated, "update");
            }
            requireValue(connection, table, "updated");
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE ID = ?")) {
                delete.setInt(1, 1);
                checkSql("DELETE FROM " + table + " WHERE ID = ?");
                checkLog("BIND: ID=1");
                int deleted = delete.executeUpdate();
                checkLog("RESULT: affected rows=" + deleted);
                requireOneRow(deleted, "delete");
            }
            requireValue(connection, table, null);
        } catch (SQLException e) {
            failure = e;
            throw e;
        } finally {
            if (created) {
                try (Statement drop = connection.createStatement()) {
                    checkSql("DROP TABLE " + table + " PURGE");
                    drop.execute("DROP TABLE " + table + " PURGE");
                    checkLog("RESULT: table dropped and purged");
                } catch (SQLException dropFailure) {
                    if (failure != null) {
                        failure.addSuppressed(dropFailure);
                    } else {
                        throw dropFailure;
                    }
                }
            }
        }
    }

    private static void requireOneRow(int count, String operation) throws SQLException {
        if (count != 1) {
            throw new SQLException("Stage CRUD " + operation + " affected " + count + " rows instead of one");
        }
    }

    private static void requireValue(Connection connection, String table, String expected) throws SQLException {
        checkSql("SELECT CHECK_VALUE FROM " + table + " WHERE ID = ?");
        checkLog("BIND: ID=1");
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT CHECK_VALUE FROM " + table + " WHERE ID = ?")) {
            select.setInt(1, 1);
            try (ResultSet result = select.executeQuery()) {
                boolean found = result.next();
                String actual = found ? result.getString(1) : null;
                checkLog("ROW: " + (found ? "CHECK_VALUE='" + actual + "'" : "<none>"));
                if (expected == null) {
                    if (found) {
                        throw new SQLException("Stage CRUD delete left a row behind");
                    }
                } else if (!found || !expected.equals(actual) || result.next()) {
                    throw new SQLException("Stage CRUD read did not return the expected value");
                }
            }
        }
    }

    private static void checkSql(String sql) {
        checkLog("SQL: " + sql);
    }

    private static void checkLog(String message) {
        System.out.println("[stage-db-check] " + message);
    }
}
