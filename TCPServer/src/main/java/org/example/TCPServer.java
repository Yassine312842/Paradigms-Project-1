package server;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TCPServer {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        Properties props = new Properties();

        try (InputStream in = TCPServer.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (in == null) {
                throw new IllegalStateException("server.properties not found in src/main/resources");
            }
            props.load(in);
        } catch (Exception e) {
            System.out.println("Failed to load server.properties");
            e.printStackTrace();
            return;
        }

        String ip = props.getProperty("server.ip", "0.0.0.0").trim();
        int port = Integer.parseInt(props.getProperty("server.port", "3000").trim());

        try (ServerSocket serverSocket = new ServerSocket()) {

            // Bind to IP + Port
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(ip), port));

            log("Server started on " + ip + ":" + port);
            log("Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                log("New connection: " + socket.getInetAddress() + ":" + socket.getPort());

                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) client.sendMessage(message);
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static String getActiveUsers() {
        return clients.stream()
                .map(ClientHandler::getUsername)
                .filter(u -> u != null && !u.isBlank())
                .collect(Collectors.joining(", "));
    }

    public static void log(String msg) {
        System.out.println(msg);
    }
}