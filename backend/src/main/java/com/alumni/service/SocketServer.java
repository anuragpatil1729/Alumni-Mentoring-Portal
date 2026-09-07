package com.alumni.service;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * SocketServer
 *
 * Implements a multi-client TCP socket server for newline-delimited JSON
 * registration processing on port 5002 (matching previous Node net socket).
 */
public class SocketServer {

    private final int port;
    private final RegistrationService registrationService;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public SocketServer(int port) {
        this.port = port;
        this.registrationService = new RegistrationService();
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Registration socket server running on port " + port);

                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(() -> handleClient(clientSocket)).start();
                    } catch (Exception e) {
                        if (!running) break;
                    }
                }
            } catch (Exception e) {
                System.err.println("SocketServer error: " + e.getMessage());
            }
        }, "RegistrationSocketServerThread").start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                try {
                    JSONObject payload = new JSONObject(trimmed);
                    RegistrationService.ProcessResult result = registrationService.processUnifiedRegistration(payload);
                    writer.println(result.getBody().toString());
                } catch (Exception e) {
                    JSONObject err = new JSONObject();
                    err.put("success", false);
                    err.put("message", "Invalid socket registration payload. Send one JSON object per line.");
                    writer.println(err.toString());
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
