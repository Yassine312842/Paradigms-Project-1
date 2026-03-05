package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientModel {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Consumer<String> onMessage = msg -> {};
    private Runnable onDisconnect = () -> {};

    public void connect(String ip, int port, String username) throws Exception {
        socket = new Socket(ip, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        out.println(username == null ? "" : username);
    }

    public void setOnMessage(Consumer<String> onMessage) {
        this.onMessage = (onMessage == null) ? (msg -> {}) : onMessage;
    }

    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = (onDisconnect == null) ? (() -> {}) : onDisconnect;
    }

    public void startListening() {
        Thread t = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    onMessage.accept(msg);
                }
            } catch (Exception ignored) {
                // ignore
            } finally {
                onDisconnect.run();
            }
        }, "client-listener");

        t.setDaemon(true);
        t.start();
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}