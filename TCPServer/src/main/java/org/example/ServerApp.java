package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for the server.
 * Loads server-view.fxml and delegates to ServerController.
 */
public class ServerApp extends Application {

    // Default server port
    public static int serverPort = 3000;

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/server-view.fxml"));

        Parent root = loader.load();

        // Get controller
        ServerController controller = loader.getController();

        // Start server
        controller.startServer();

        Scene scene = new Scene(root, 960, 600);

        scene.getStylesheets().add(
                getClass().getResource("/server-styles.css").toExternalForm()
        );

        stage.setTitle("TCP Chat Server — Dashboard");

        stage.setScene(scene);

        stage.show();
    }

    @Override
    public void stop() {

        System.out.println("Server shutting down...");

        System.exit(0);
    }

    public static void main(String[] args) {

        /*
         * Allow command line server startup
         *
         * Example:
         * java TCPServer 3000
         */

        if (args.length >= 1) {

            try {

                serverPort = Integer.parseInt(args[0]);

            } catch (Exception ignored) {

                serverPort = 3000;
            }
        }

        launch(args);
    }
}