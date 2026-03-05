package org.example;

import java.io.InputStream;
import java.util.Properties;

/**
 * Lightweight launcher — starts the JavaFX ServerApp.
 * Can also be run headless (without JavaFX) by passing --headless.
 */
public class TCPServer {

    public static void main(String[] args) {

        // Headless / console mode (optional)
        for (String a : args) {
            if ("--headless".equalsIgnoreCase(a)) {
                runHeadless();
                return;
            }
        }

        // Default: launch JavaFX server dashboard
        ServerApp.main(args);
    }

    /** Fallback console-only mode (kept from original code). */
    private static void runHeadless() {
        Properties props = new Properties();

        try (InputStream in = TCPServer.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.out.println("server.properties not found, using defaults.");
            }
        } catch (Exception e) {
            System.out.println("Could not load server.properties, using defaults.");
            e.printStackTrace();
        }

        String ip = props.getProperty("server.host",
                props.getProperty("server.ip", "127.0.0.1")).trim();
        int port = Integer.parseInt(props.getProperty("server.port", "3000").trim());

        ServerModel server = new ServerModel(ip, port);
        server.start();
    }
}