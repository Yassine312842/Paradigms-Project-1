package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler extends Thread {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String username;
    private boolean readOnly;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 1) First line from client = username
            String firstLine = in.readLine();
            if (firstLine == null) {
                return; // client disconnected immediately
            }

            username = firstLine.trim();

            // 2) Read-only mode if empty username
            if (username.isEmpty()) {
                readOnly = true;
                username = "READ_ONLY_" + socket.getPort();
                sendMessage(systemMsg("You are connected in READ-ONLY MODE (cannot send messages)."));
            } else {
                readOnly = false;
                sendMessage(systemMsg("Welcome " + username + "!"));
            }

            // ✅ SYSTEM join message (broadcast to others)
            TCPServer.log(username + " connected.");
            TCPServer.broadcast(systemMsg(username + " joined the chat."), this);

            // 3) Message loop
            String msg;
            while ((msg = in.readLine()) != null) {
                msg = msg.trim();
                if (msg.isEmpty()) continue;

                // Disconnect commands
                if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("end")) {
                    sendMessage(systemMsg("Goodbye!"));
                    break;
                }

                // allUsers command
                if (msg.equalsIgnoreCase("allUsers")) {
                    sendMessage(systemMsg("Active users: " + TCPServer.getActiveUsers()));
                    continue;
                }

                // Read-only enforcement
                if (readOnly) {
                    sendMessage(systemMsg("READ-ONLY MODE: you cannot send messages."));
                    continue;
                }

                // Normal message broadcast
                String formatted = "[" + now() + "] " + username + ": " + msg;
                TCPServer.broadcast(formatted, this);
            }

        } catch (Exception ignored) {
            // unexpected disconnects end up here
        } finally {
            // ✅ SYSTEM leave message + remove client
            TCPServer.removeClient(this);
            if (username != null) {
                TCPServer.broadcast(systemMsg(username + " left the chat."), this);
                TCPServer.log(username + " disconnected.");
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
        if (out != null) out.println(message);
    }

    public String getUsername() {
        return username;
    }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
    }
}