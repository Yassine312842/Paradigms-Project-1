package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatController {

    private static final String CHAT_BG_PATH = "/images/login_bg.jpg";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private ImageView topLogo;
    @FXML private Circle statusCircle;
    @FXML private Label statusLabel;
    @FXML private ImageView chatBgImage;
    @FXML private ListView<Message> chatList;
    @FXML private ListView<String> usersList;
    @FXML private Button refreshUsersBtn;
    @FXML private TextField messageField;
    @FXML private Button sendBtn;
    @FXML private Button disconnectBtn;

    private ClientModel model;
    private String myUsername = "";
    private boolean readOnlyMode = false;

    @FXML
    private void initialize() {

        chatList.setItems(FXCollections.observableArrayList());
        usersList.setItems(FXCollections.observableArrayList());

        chatList.setCellFactory(lv -> new MessageCell());

        messageField.setOnAction(e -> handleSend());

        Platform.runLater(() -> messageField.requestFocus());

        Image logo = tryLoadImage("/images/aui_logo_white.png", "/images/aui_logo.png");
        if (logo != null) {
            topLogo.setImage(logo);
        }

        Image bg = tryLoadImage(CHAT_BG_PATH);
        if (bg != null) {
            chatBgImage.setImage(bg);
            chatBgImage.setOpacity(0.08);
        }

        chatList.setStyle("-fx-background-color: transparent;");

        setChatControlsEnabled(false);
    }

    public void initChat(ClientModel model, String username, boolean readOnly) {

        this.model = model;
        this.myUsername = username;
        this.readOnlyMode = readOnly;

        model.setOnMessage(line -> Platform.runLater(() -> handleIncomingLine(line)));

        model.setOnDisconnect(() -> Platform.runLater(() -> {
            statusLabel.setText("Offline (disconnected)");
            statusCircle.setFill(Color.GRAY);
            setChatControlsEnabled(false);
            addSystem("Disconnected from server.");
        }));

        statusLabel.setText(readOnly ? "Online (READ-ONLY)" : "Online");
        statusCircle.setFill(Color.web("#43A047"));

        setChatControlsEnabled(true);

        if (readOnly) {
            messageField.setDisable(true);
            sendBtn.setDisable(true);
            addSystem("Connected in READ-ONLY mode.");
        } else {
            addSystem("Connected as " + myUsername + ".");
        }
    }

    @FXML
    private void handleSend() {

        if (readOnlyMode || model == null) return;

        String text = messageField.getText();

        if (text == null || text.isBlank()) return;

        addOwn(text);

        model.sendMessage(text);

        messageField.clear();
    }

    @FXML
    private void handleRefreshUsers() {

        if (model != null)
            model.sendMessage("allUsers");
    }

    @FXML
    private void handleDisconnect() {

        try {

            if (model != null && model.isConnected()) {
                model.sendMessage("bye");
                model.disconnect();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 650);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            Stage stage = (Stage) messageField.getScene().getWindow();
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingLine(String line) {

        if (line == null) return;

        if (line.contains("(SYSTEM): Active users:")) {

            String usersPart = line.substring(line.indexOf("Active users:") + "Active users:".length()).trim();

            List<String> users =
                    Arrays.stream(usersPart.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList());

            usersList.getItems().setAll(users);

            addSystem("Active users updated.");
            return;
        }

        if (line.contains("(SYSTEM):")) {
            addSystem(line);
            return;
        }

        addOther(line);
    }

    private void addOwn(String text) {

        chatList.getItems().add(Message.own(now(), myUsername, text));

        scrollToBottom();
    }

    private void addOther(String rawLine) {

        String sender = "";
        String text = rawLine;

        if (rawLine.contains(":")) {

            int idx = rawLine.indexOf(":");

            sender = rawLine.substring(0, idx).trim();
            text = rawLine.substring(idx + 1).trim();
        }

        chatList.getItems().add(new Message(Message.Kind.OTHER, now(), sender, text));

        scrollToBottom();
    }

    private void addSystem(String text) {

        chatList.getItems().add(Message.system(text));

        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatList.scrollTo(chatList.getItems().size() - 1));
    }

    private void setChatControlsEnabled(boolean enabled) {

        refreshUsersBtn.setDisable(!enabled);
        usersList.setDisable(!enabled);

        if (!enabled) {
            messageField.setDisable(true);
            sendBtn.setDisable(true);
        } else if (!readOnlyMode) {
            messageField.setDisable(false);
            sendBtn.setDisable(false);
        }
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private Image tryLoadImage(String... paths) {

        for (String p : paths) {

            try {

                var url = getClass().getResource(p);

                if (url != null)
                    return new Image(url.toExternalForm());

            } catch (Exception ignored) {}
        }

        return null;
    }

    static class Message {

        enum Kind { OWN, OTHER, SYSTEM }

        final Kind kind;
        final String time;
        final String sender;
        final String text;

        private Message(Kind kind, String time, String sender, String text) {
            this.kind = kind;
            this.time = time;
            this.sender = sender;
            this.text = text;
        }

        static Message own(String time, String sender, String text) {
            return new Message(Kind.OWN, time, sender, text);
        }

        static Message system(String text) {
            return new Message(Kind.SYSTEM, "", "", text);
        }
    }

    private static class MessageCell extends ListCell<Message> {

        @Override
        protected void updateItem(Message msg, boolean empty) {

            super.updateItem(msg, empty);

            if (empty || msg == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            if (msg.kind == Message.Kind.SYSTEM) {

                Label sys = new Label(msg.text);
                sys.getStyleClass().add("bubble-system");
                sys.setWrapText(true);
                sys.setTextAlignment(TextAlignment.CENTER);

                HBox box = new HBox(sys);
                box.setAlignment(Pos.CENTER);
                box.setPadding(new Insets(8,12,8,12));

                setGraphic(box);
                return;
            }

            VBox bubble = new VBox(4);
            bubble.getStyleClass().add("bubble");

            Label text = new Label(
                    msg.sender == null || msg.sender.isEmpty()
                            ? msg.text
                            : msg.sender + ": " + msg.text
            );

            text.getStyleClass().add("bubble-text");
            text.setWrapText(true);

            Label meta = new Label(msg.kind == Message.Kind.OWN ? msg.time : "");
            meta.getStyleClass().add("meta");

            if (msg.kind == Message.Kind.OWN) {

                bubble.getStyleClass().add("bubble-own");
                bubble.getChildren().addAll(text, meta);

            } else {

                bubble.getStyleClass().add("bubble-other");
                bubble.getChildren().add(text);
            }

            HBox row = new HBox(bubble);
            row.getStyleClass().add("bubble-row");
            row.setPadding(new Insets(6,14,6,14));
            row.setAlignment(msg.kind == Message.Kind.OWN ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            setGraphic(row);
        }
    }
}