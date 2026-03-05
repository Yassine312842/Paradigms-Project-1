package org.example;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ServerModel {

    private final String bindIp;
    private final int port;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private Consumer<String> onLog = System.out::println;
    private BiConsumer<String, String> onClientJoined = (u, c) -> {};
    private Consumer<String> onClientLeft = u -> {};

    private static final String[] COLORS = {
            "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
            "#3949AB", "#1E88E5", "#039BE5", "#00ACC1",
            "#00897B", "#43A047", "#7CB342", "#C0CA33",
            "#FDD835", "#FFB300", "#FB8C00", "#F4511E"
    };

    private final Random rng = new Random();

    public ServerModel(String bindIp, int port) {
        this.bindIp = bindIp;
        this.port = port;
    }

    public static ServerModel fromProperties() {

        Properties props = new Properties();

        try (InputStream in =
                     ServerModel.class.getClassLoader()
                             .getResourceAsStream("server.properties")) {

            if (in != null)
                props.load(in);

        } catch (Exception ignored) {}

        String ip = props.getProperty(
                "server.host",
                props.getProperty("server.ip", "127.0.0.1")
        ).trim();

        int port = Integer.parseInt(
                props.getProperty("server.port", "3000").trim()
        );

        return new ServerModel(ip, port);
    }

    public void setOnLog(Consumer<String> cb) {
        this.onLog = cb;
    }

    public void setOnClientJoined(BiConsumer<String, String> cb) {
        this.onClientJoined = cb;
    }

    public void setOnClientLeft(Consumer<String> cb) {
        this.onClientLeft = cb;
    }

    public void start() {

        try (ServerSocket serverSocket = new ServerSocket()) {

            serverSocket.bind(
                    new InetSocketAddress(
                            InetAddress.getByName(bindIp),
                            port
                    )
            );

            log("Server Started on " + bindIp + ":" + port);
            log("Waiting for Client...");

            while (true) {

                Socket socket = serverSocket.accept();

                log("New connection: "
                        + socket.getInetAddress()
                        + ":" + socket.getPort());

                ClientHandler handler =
                        new ClientHandler(socket, this);

                clients.add(handler);

                handler.start();
            }

        } catch (Exception e) {

            log("Server crashed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcast(String message, ClientHandler sender) {

        for (ClientHandler client : clients) {

            if (client != sender) {

                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {

        clients.remove(client);

        if (onClientLeft != null
                && client.getUsername() != null) {

            onClientLeft.accept(client.getUsername());
        }
    }

    public void notifyClientJoined(String username) {

        String color =
                COLORS[rng.nextInt(COLORS.length)];

        if (onClientJoined != null)
            onClientJoined.accept(username, color);
    }

    public String getActiveUsers() {

        return clients.stream()

                .map(ClientHandler::getUsername)

                .filter(u -> u != null && !u.isBlank())

                .collect(Collectors.joining(", "));
    }

    public List<String> getActiveUserList() {

        return clients.stream()

                .map(ClientHandler::getUsername)

                .filter(u -> u != null && !u.isBlank())

                .collect(Collectors.toList());
    }

    public void log(String msg) {

        if (onLog != null)
            onLog.accept(msg);
    }

    public String getBindIp() {
        return bindIp;
    }

    public int getPort() {
        return port;
    }

    // ⭐ Added method (fixes your error)

    public List<ClientHandler> getClients() {
        return clients;
    }
}