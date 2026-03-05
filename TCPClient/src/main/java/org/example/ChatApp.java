package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for the client.
 * Loads login-view.fxml and delegates to LoginController.
 */
public class ChatApp extends Application {

    // Default server configuration
    public static String serverIP = "localhost";
    public static int serverPort = 3000;

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/login-view.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 650);

        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("AUI General Chat — Login");

        stage.setScene(scene);

        stage.show();
    }

    @Override
    public void stop() {

        // Called when application closes
        System.out.println("Client shutting down...");

        System.exit(0);
    }

    public static void main(String[] args) {

        /*
         * Allow command-line startup
         *
         * Example:
         * java TCPClient localhost 3000
         */

        if (args.length >= 2) {

            serverIP = args[0];

            try {
                serverPort = Integer.parseInt(args[1]);
            }
            catch (Exception ignored) {
                serverPort = 3000;
            }
        }

        launch(args);
    }
}