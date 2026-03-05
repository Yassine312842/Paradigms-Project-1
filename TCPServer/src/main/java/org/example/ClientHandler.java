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

            // 1) Username handshake (first line)
            String firstLine = in.readLine(); // may be null if client disconnects instantly
            if (firstLine == null) {
                close();
                return;
            }

            username = firstLine.trim();
            if (username.isEmpty()) {
                readOnly = true;
                username = "READ_ONLY_" + socket.getPort(); // simple unique fallback
                sendMessage("You are connected in READ-ONLY MODE (cannot send messages).");
            } else {
                readOnly = false;
                sendMessage("Welcome " + username + "!");
            }

            TCPServer.log(username + " connected.");
            TCPServer.broadcast(systemMsg(username + " joined the chat."), this);

            // 2) Main message loop
            String msg;
            while ((msg = in.readLine()) != null) {
                msg = msg.trim();
                if (msg.isEmpty()) continue;

                // Disconnect commands
                if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("end")) {
                    sendMessage("Goodbye!");
                    break;
                }

                // allUsers command
                if (msg.equalsIgnoreCase("allUsers")) {
                    sendMessage("Active users: " + TCPServer.getActiveUsers());
                    continue;
                }

                // Read-only enforcement
                if (readOnly) {
                    sendMessage("READ-ONLY MODE: you cannot send messages.");
                    continue;
                }

                // Normal message → formatted broadcast
                String formatted = "[" + LocalTime.now().format(TIME_FMT) + "] " + username + ": " + msg;
                TCPServer.broadcast(formatted, this);
            }

        } catch (Exception e) {
            // client likely disconnected unexpectedly
        } finally {
            TCPServer.removeClient(this);
            TCPServer.broadcast(systemMsg(username + " left the chat."), this);
            TCPServer.log(username + " disconnected.");
            close();
        }
    }

    private String systemMsg(String text) {
        return "[" + LocalTime.now().format(TIME_FMT) + "] " + "(SYSTEM): " + text;
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public String getUsername() {
        return username;
    }

    private void close() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
    }
}