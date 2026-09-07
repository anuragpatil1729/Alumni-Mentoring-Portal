package com.alumni.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * DBConnection
 *
 * Configures com.mysql.cj.jdbc.Driver and manages JDBC connections to local MySQL.
 * Reads configurations in order:
 * 1. System environment variables
 * 2. .env file in project directory
 * 3. application.properties
 */
public class DBConnection {

    private static final Properties properties = new Properties();
    private static final Map<String, String> dotEnvMap = new HashMap<>();
    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;
    private static int serverPort = 5001;
    private static int socketPort = 5002;

    static {
        try {
            // 1. Load application.properties
            try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (input != null) {
                    properties.load(input);
                }
            }

            // 2. Load .env if present
            loadDotEnv();

            // 3. Register MySQL JDBC Driver
            String driverClass = properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            Class.forName(driverClass);

            // 4. Resolve settings with precedence: System Env > .env > application.properties
            String host = resolveConfig("DB_HOST", "db.host", "localhost");
            String port = resolveConfig("DB_PORT", "db.port", "3306");
            String dbName = resolveConfig("DB_NAME", "db.name", "alumni_mentoring_portal");
            String params = properties.getProperty("db.params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

            dbUser = resolveConfig("DB_USER", "db.user", "alumni_user");
            dbPassword = resolveConfig("DB_PASSWORD", "db.password", "alumni_password");

            String sPortStr = resolveConfig("PORT", "server.port", "5001");
            serverPort = Integer.parseInt(sPortStr);

            String sockPortStr = resolveConfig("SOCKET_PORT", "socket.port", "5002");
            socketPort = Integer.parseInt(sockPortStr);

            jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, dbName, params);

        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found in classpath: " + e.getMessage());
            throw new RuntimeException("MySQL JDBC Driver missing", e);
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize DBConnection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void loadDotEnv() {
        String[] possiblePaths = { ".env", "backend/.env", "../.env" };
        for (String path : possiblePaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String k = line.substring(0, eqIdx).trim();
                            String v = line.substring(eqIdx + 1).trim();
                            dotEnvMap.put(k, v);
                        }
                    }
                    break;
                } catch (Exception ignored) {}
            }
        }
    }

    private static String resolveConfig(String envKey, String propKey, String defaultValue) {
        String sysVal = System.getenv(envKey);
        if (sysVal != null && !sysVal.trim().isEmpty()) {
            return sysVal.trim();
        }
        String dotVal = dotEnvMap.get(envKey);
        if (dotVal != null && !dotVal.trim().isEmpty()) {
            return dotVal.trim();
        }
        return properties.getProperty(propKey, defaultValue);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getJdbcUrl() { return jdbcUrl; }
    public static String getDbUser() { return dbUser; }
    public static int getServerPort() { return serverPort; }
    public static int getSocketPort() { return socketPort; }
}
