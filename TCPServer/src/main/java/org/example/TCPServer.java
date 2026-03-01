package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TCPServer {

    private static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        int port = 3000;

        try {

            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Server started on port " + port);
            System.out.println("Waiting for clients...");

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("New client connected: " + socket.getInetAddress());

                ClientHandler client = new ClientHandler(socket);

                clients.add(client);

                client.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message) {

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }

    }
}