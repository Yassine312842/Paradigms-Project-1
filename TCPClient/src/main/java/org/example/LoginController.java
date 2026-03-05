package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Controller for login-view.fxml — handles login form and connection.
 */
public class LoginController {

    // FIXED image paths
    private static final String LOGIN_BG_PATH = "/images/login_bg.jpg";
    private static final String LOGIN_LOGO_PATH = "/images/aui_logo.png";

    @FXML
    private StackPane rootPane;

    @FXML
    private ImageView bgImage;

    @FXML
    private Region overlay;

    @FXML
    private ImageView logoImage;

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private TextField userField;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {

        // Load background image
        Image bg = tryLoadImage(LOGIN_BG_PATH);
        if (bg != null) {
            bgImage.setImage(bg);
        }

        // Make background resize with window
        rootPane.widthProperty().addListener((obs, o, n) -> bgImage.setFitWidth(n.doubleValue()));
        rootPane.heightProperty().addListener((obs, o, n) -> bgImage.setFitHeight(n.doubleValue()));

        overlay.prefWidthProperty().bind(rootPane.widthProperty());
        overlay.prefHeightProperty().bind(rootPane.heightProperty());

        // Load logo
        Image logo = tryLoadImage(LOGIN_LOGO_PATH);
        if (logo != null) {
            logoImage.setImage(logo);
        }
    }

    /**
     * Connect button handler
     */
    @FXML
    private void handleConnect() {

        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        String username = (userField.getText() == null) ? "" : userField.getText().trim();

        int port;

        try {
            port = Integer.parseInt(portStr);
        } catch (Exception e) {
            showError("Invalid port number.");
            return;
        }

        try {

            // Create client model and connect
            ClientModel model = new ClientModel();
            boolean readOnly = username.isEmpty();

            model.connect(ip, port, username);
            model.startListening();

            // Load chat screen
            FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/chat-view.fxml"));
            Parent chatRoot = chatLoader.load();

            ChatController chatController = chatLoader.getController();
            chatController.initChat(model, username, readOnly);

            Scene chatScene = new Scene(chatRoot, 1100, 700);
            chatScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(chatScene);

        }
        catch (Exception e) {

            // Show real error in console for debugging
            e.printStackTrace();

            showError("Connection error: " + e.getMessage());
        }
    }

    // Show error message
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // Load image safely
    private Image tryLoadImage(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null)
                return null;
            return new Image(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }
}