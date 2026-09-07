package com.alumni;

import com.alumni.config.DBConnection;
import com.alumni.dao.AlumniDAO;
import com.alumni.server.HttpServerApp;
import com.alumni.service.SocketServer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Main Application Bootstrap
 *
 * Starts the Java HTTP REST Server on port 5001 and TCP Socket Server on port 5002.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  Alumni Mentoring Portal - 100% Java Backend Engine");
        System.out.println("==========================================================");

        // 1. Validate JDBC Driver Connection
        System.out.println("Connecting to MySQL via JDBC: " + DBConnection.getJdbcUrl());
        boolean connected = DBConnection.testConnection();

        if (!connected) {
            System.err.println("⚠️ Warning: Could not connect to local MySQL database.");
            System.err.println("Please check DB_HOST, DB_PORT, DB_USER, DB_PASSWORD settings.");
        } else {
            System.out.println("✅ JDBC Driver loaded & MySQL connected successfully!");
            try (Connection conn = DBConnection.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("   Driver Name   : " + metaData.getDriverName());
                System.out.println("   Driver Version: " + metaData.getDriverVersion());
                System.out.println("   Database Product: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());

                AlumniDAO alumniDAO = new AlumniDAO();
                System.out.println("   Total Mentors in Database: " + alumniDAO.count());
            } catch (Exception e) {
                System.err.println("Database inspection note: " + e.getMessage());
            }
        }

        // 2. Start HTTP Server on port 5001
        int httpPort = DBConnection.getServerPort();
        try {
            HttpServerApp httpServer = new HttpServerApp(httpPort);
            httpServer.start();
        } catch (Exception e) {
            System.err.println("❌ Failed to start HTTP server on port " + httpPort + ": " + e.getMessage());
        }

        // 3. Start Socket Server on port 5002
        int socketPort = DBConnection.getSocketPort();
        try {
            SocketServer socketServer = new SocketServer(socketPort);
            socketServer.start();
        } catch (Exception e) {
            System.err.println("❌ Failed to start socket server on port " + socketPort + ": " + e.getMessage());
        }

        System.out.println("\nBackend is fully active and ready for requests!");
        System.out.println("  • Health Check   : http://localhost:" + httpPort + "/api/health");
        System.out.println("  • Mentor Search  : http://localhost:" + httpPort + "/api/mentors/search");
        System.out.println("  • Student Reg    : POST http://localhost:" + httpPort + "/api/auth/register/student");
        System.out.println("  • Alumni Reg     : POST http://localhost:" + httpPort + "/api/auth/register/alumni");
        System.out.println("  • Servlet Form   : POST http://localhost:" + httpPort + "/api/servlet/register");
        System.out.println("  • CGI Form       : POST http://localhost:" + httpPort + "/cgi-bin/register");
        System.out.println("  • TCP Socket     : localhost:" + socketPort);
    }
}
