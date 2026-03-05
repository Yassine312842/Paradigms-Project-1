package server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ServerModel {

    private final String bindIp;
    private final int port;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public ServerModel(String bindIp, int port) {
        this.bindIp = bindIp;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(bindIp), port));

            log("Server started on " + bindIp + ":" + port);
            log("Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                log("New connection: " + socket.getInetAddress() + ":" + socket.getPort());

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                handler.start();
            }
        } catch (Exception e) {
            log("Server crashed:");
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
    }

    public String getActiveUsers() {
        return clients.stream()
                .map(ClientHandler::getUsername)
                .filter(u -> u != null && !u.isBlank())
                .collect(Collectors.joining(", "));
    }

    public void log(String msg) {
        System.out.println(msg);
    }
}