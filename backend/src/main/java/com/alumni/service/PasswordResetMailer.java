package com.alumni.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/** SMTP delivery for password-reset links. Configure SMTP_* environment variables. */
public final class PasswordResetMailer {
    private PasswordResetMailer() {}

    public static void send(String recipient, String token) throws Exception {
        String host = required("SMTP_HOST");
        String username = required("SMTP_USERNAME");
        String password = required("SMTP_PASSWORD");
        String from = System.getenv().getOrDefault("SMTP_FROM", username);
        String port = System.getenv().getOrDefault("SMTP_PORT", "587");
        String appUrl = required("APP_PUBLIC_URL"); // e.g. https://your-site.example/reset-password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        String separator = appUrl.contains("?") ? "&" : "?";
        String link = appUrl + separator + "token=" + token;
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
        message.setSubject("Reset your Alumni Mentoring Portal password", "UTF-8");
        message.setText("We received a request to reset your password. This link expires in 30 minutes and can be used once:\n\n"
                + link + "\n\nIf you did not request this, you can ignore this email.", "UTF-8");
        Transport.send(message);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing environment variable: " + name);
        return value.trim();
    }
}
