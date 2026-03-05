package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final ServerModel server;

    private BufferedReader in;
    private PrintWriter out;

    private String username;
    private boolean readOnly;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    public ClientHandler(Socket socket, ServerModel server) {
        this.socket = socket;
        this.server = server;
        setName("client-handler-" + socket.getPort());
        setDaemon(true);
    }

    @Override
    public void run() {

        try {

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

            // ---- Username handshake ----
            String firstLine = in.readLine();

            if (firstLine == null)
                return;

            username = firstLine.trim();

            if (username.isEmpty()) {

                readOnly = true;
                username = "READ_ONLY_" + socket.getPort();

                sendMessage(systemMsg(
                        "You are connected in READ-ONLY MODE (cannot send messages)."));

            } else {

                readOnly = false;

                sendMessage(systemMsg(
                        "Welcome " + username + "!"));
            }

            server.log("Welcome " + username);

            server.notifyClientJoined(username);

            server.broadcast(
                    systemMsg(username + " joined the chat."), this);

            // ---- Message loop ----
            String msg;

            while ((msg = in.readLine()) != null) {

                msg = msg.trim();

                if (msg.isEmpty())
                    continue;

                // ---- Disconnect commands ----
                if (msg.equalsIgnoreCase("bye")
                        || msg.equalsIgnoreCase("end")) {

                    sendMessage(systemMsg("Goodbye!"));
                    break;
                }

                // ---- Active users command ----
                if (msg.equalsIgnoreCase("allUsers")) {

                    sendMessage(systemMsg(
                            "Active users: " + server.getActiveUsers()));

                    continue;
                }

                // ---- Private message command ----
                if (msg.startsWith("/msg ")) {

                    String[] parts = msg.split(" ", 3);

                    if (parts.length >= 3) {

                        String targetUser = parts[1];
                        String privateMsg = parts[2];

                        boolean found = false;

                        for (ClientHandler c : server.getClients()) {

                            if (targetUser.equalsIgnoreCase(c.getUsername())) {

                                c.sendMessage(
                                        "(Private) " + username + ": " + privateMsg);

                                sendMessage(
                                        "(Private to " + targetUser + "): " + privateMsg);

                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            sendMessage(systemMsg(
                                    "User not found: " + targetUser));
                        }
                    }

                    continue;
                }

                // ---- Read-only enforcement ----
                if (readOnly) {

                    sendMessage(systemMsg(
                            "READ-ONLY MODE: you cannot send messages."));

                    continue;
                }

                // ---- Normal message broadcast ----
                String formatted =
                        "[" + now() + "] " + username + ": " + msg;

                server.broadcast(formatted, this);
            }

        } catch (Exception ignored) {

        } finally {

            server.removeClient(this);

            if (username != null) {

                server.broadcast(
                        systemMsg(username + " left the chat."), this);

                server.log(username + " disconnected.");
            }

            closeQuietly();
        }
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private String systemMsg(String text) {
        return "[" + now() + "] (SYSTEM): " + text;
    }

    public void sendMessage(String message) {

        if (out != null)
            out.println(message);
    }

    public String getUsername() {
        return username;
    }

    private void closeQuietly() {

        try {
            if (in != null)
                in.close();
        } catch (Exception ignored) {}

        try {
            if (out != null)
                out.close();
        } catch (Exception ignored) {}

        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (Exception ignored) {}
    }
}