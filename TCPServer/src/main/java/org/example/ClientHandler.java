package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Message received: " + message);

                TCPServer.broadcast(message);
            }

        } catch (Exception e) {
            System.out.println("Client disconnected.");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}