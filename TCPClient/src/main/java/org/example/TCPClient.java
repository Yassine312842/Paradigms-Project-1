package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Console-mode TCP client.
 * Usage: java TCPClient <ServerIP> <Port>
 *
 * The JavaFX GUI client is ChatApp; this class is for command-line usage.
 */
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

            // Ask for username
            System.out.print("Enter username (leave empty for READ-ONLY mode): ");
            String username = console.readLine();

            if (username == null) {
                username = "";
            }

            // Send username to server (handshake)
            out.println(username);

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