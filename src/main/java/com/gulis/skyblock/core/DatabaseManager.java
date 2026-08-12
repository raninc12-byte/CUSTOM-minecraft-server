package com.gulis.skyblock.core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database abstraction layer.
 *
 * <p>For local laptop hosting, SQLite is ideal: a single {@code skyblock.db}
 * file with zero external setup. The schema is created lazily on connect via
 * {@link #createTables()}. All queries use {@link PreparedStatement} to avoid
 * SQL injection. If MySQL support is needed later, swap the connection logic
 * for HikariCP pooling (already on the classpath) and adjust the dialect.</p>
 */
public class DatabaseManager {

    private final Skyblock plugin;
    private Connection connection;

    public DatabaseManager(Skyblock plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the SQLite connection. The driver is bundled in the plugin jar so
     * no server-side installation is required.
     */
    public void connect() {
        try {
            // Load the SQLite driver explicitly (works around some classloader issues)
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "skyblock.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.connection = DriverManager.getConnection(url);
            plugin.getLogger().info("SQLite database connected: " + dbFile.getName());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found! " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database: " + e.getMessage());
        }
    }

    /**
     * Creates all required tables if they do not already exist.
     */
    public void createTables() {
        try (Statement stmt = connection.createStatement()) {

            // Player economy balances
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS players (" +
                "  uuid VARCHAR(36) PRIMARY KEY," +
                "  name VARCHAR(16)," +
                "  balance DOUBLE DEFAULT 0.0" +
                ");"
            );

            // Islands
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS islands (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  owner_uuid VARCHAR(36) NOT NULL," +
                "  center_x INT NOT NULL," +
                "  center_y INT NOT NULL," +
                "  center_z INT NOT NULL," +
                "  world VARCHAR(32) NOT NULL," +
                "  level DOUBLE DEFAULT 0.0," +
                "  members TEXT DEFAULT '[]'," +
                "  UNIQUE(owner_uuid)" +
                ");"
            );

            // Island challenges progress
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS challenges (" +
                "  player_uuid VARCHAR(36) NOT NULL," +
                "  challenge_id VARCHAR(64) NOT NULL," +
                "  completed INT DEFAULT 0," +
                "  PRIMARY KEY (player_uuid, challenge_id)" +
                ");"
            );

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    /**
     * Convenience wrapper for prepared queries.
     *
     * @param sql  the SQL statement with {@code ?} placeholders
     * @param args the bind arguments
     * @return the resulting {@link ResultSet}; callers should close it
     */
    public ResultSet query(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps.executeQuery();
    }

    /**
     * Convenience wrapper for prepared updates (INSERT/UPDATE/DELETE).
     *
     * @param sql  the SQL statement with {@code ?} placeholders
     * @param args the bind arguments
     * @return the number of rows affected
     */
    public int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * Runs an update and returns the generated row key (for AUTOINCREMENT).
     *
     * @param sql  the SQL INSERT statement
     * @param args the bind arguments
     * @return the generated key, or -1 if none was produced
     */
    public int insertAndGetKey(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return -1;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection cleanly.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("SQLite database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }
}
