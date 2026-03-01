package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: java TCPClient <ServerIP> <Port>");
            return;
        }

        String serverIP = args[0];
        int port = Integer.parseInt(args[1]);

        try {

            Socket socket = new Socket(serverIP, port);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server.");

            // Thread to receive messages
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            // Send messages
            String userInput;

            while ((userInput = console.readLine()) != null) {
                out.println(userInput);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}