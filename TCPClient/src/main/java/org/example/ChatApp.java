package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatApp extends Application {

    private final ClientModel model = new ClientModel();

    // --- UI state ---
    private final ListView<Message> chatList = new ListView<>(FXCollections.observableArrayList());
    private final ListView<String> usersList = new ListView<>(FXCollections.observableArrayList());

    private final TextField messageField = new TextField();
    private final Button sendBtn = new Button("Send");
    private final Button refreshUsersBtn = new Button("Refresh users");
    private final Label statusLabel = new Label("Offline");

    private String myUsername = "";
    private boolean readOnlyMode = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void start(Stage stage) {
        stage.setTitle("AUI General Chat");

        Scene loginScene = buildLoginScene(stage);
        Scene chatScene = buildChatScene(stage);

        model.setOnMessage(line -> Platform.runLater(() -> handleIncomingLine(line)));
        model.setOnDisconnect(() -> Platform.runLater(() -> {
            statusLabel.setText("Offline (disconnected)");
            setChatControlsEnabled(false);
            addSystem("Disconnected from server.");
        }));

        stage.getProperties().put("chatScene", chatScene);
        stage.setScene(loginScene);
        stage.show();
    }

    private Scene buildLoginScene(Stage stage) {
        TextField ipField = new TextField("localhost");
        TextField portField = new TextField("3000");
        TextField userField = new TextField();
        userField.setPromptText("Leave empty for READ-ONLY");

        Label error = new Label();
        Button connectBtn = new Button("Connect");
        connectBtn.getStyleClass().add("primary-btn");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(18));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Server IP:"), 0, 0);
        grid.add(ipField, 1, 0);

        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);

        grid.add(new Label("Username:"), 0, 2);
        grid.add(userField, 1, 2);

        grid.add(connectBtn, 1, 3);
        grid.add(error, 1, 4);

        connectBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String portStr = portField.getText().trim();
            String username = userField.getText() == null ? "" : userField.getText().trim();

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception ex) {
                error.setText("Invalid port.");
                return;
            }

            try {
                myUsername = username;
                readOnlyMode = username.isEmpty();

                model.connect(ip, port, username);
                model.startListening();

                statusLabel.setText(readOnlyMode ? "Online (READ-ONLY)" : "Online");
                setChatControlsEnabled(true);

                if (readOnlyMode) {
                    messageField.setDisable(true);
                    sendBtn.setDisable(true);
                    addSystem("Connected in READ-ONLY mode.");
                } else {
                    addSystem("Connected as " + myUsername + ".");
                }

                Scene chatScene = (Scene) stage.getProperties().get("chatScene");
                stage.setScene(chatScene);

            } catch (Exception ex) {
                error.setText("Failed to connect. Is the server running?");
            }
        });

        Scene scene = new Scene(grid, 460, 250);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    private Scene buildChatScene(Stage stage) {
        // Top bar: logo + title + status
        HBox topbar = new HBox();
        topbar.getStyleClass().add("topbar");

        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/aui_logo.png"));
            logoView.setImage(logo);
            logoView.setFitHeight(28);
            logoView.setFitWidth(28);
            logoView.setPreserveRatio(true);
        } catch (Exception ignored) {
            // If logo missing, app still runs
        }

        Label title = new Label("AUI General Chat");
        title.getStyleClass().add("topbar-title");

        statusLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topbar.getChildren().addAll(logoView, title, spacer, statusLabel);

        // Chat list (WhatsApp style bubbles)
        chatList.getStyleClass().add("chat-list");
        chatList.setFocusTraversable(false);
        chatList.setCellFactory(lv -> new MessageCell());

        // Right panel users
        VBox right = new VBox(10, new Label("Users"), usersList, refreshUsersBtn);
        right.setPadding(new Insets(16));
        right.setPrefWidth(220);
        VBox.setVgrow(usersList, Priority.ALWAYS);

        refreshUsersBtn.setOnAction(e -> model.sendMessage("allUsers"));

        // Input bar
        messageField.setPromptText("Type a message...");
        sendBtn.getStyleClass().add("primary-btn");

        Runnable sendAction = () -> {
            if (readOnlyMode) return;
            String text = messageField.getText();
            if (text == null || text.isBlank()) return;

            // 1) show locally (server doesn't echo back to sender)
            addOwn(text);

            // 2) send to server
            model.sendMessage(text);

            messageField.clear();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        messageField.setOnAction(e -> sendAction.run());

        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setOnAction(e -> {
            if (model.isConnected()) model.sendMessage("bye");
            model.disconnect();
            stage.setScene(buildLoginScene(stage));
        });

        HBox inputBar = new HBox(10, messageField, sendBtn, disconnectBtn);
        inputBar.getStyleClass().add("input-bar");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(topbar);
        root.setCenter(chatList);
        root.setRight(right);
        root.setBottom(inputBar);

        setChatControlsEnabled(false);

        Scene scene = new Scene(root, 980, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
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

    // ---------- Incoming parsing ----------
    private void handleIncomingLine(String line) {
        // Update users list if this is the server's response for allUsers
        if (line.contains("(SYSTEM): Active users:")) {
            String usersPart = line.substring(line.indexOf("Active users:") + "Active users:".length()).trim();
            List<String> users = Arrays.stream(usersPart.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
            usersList.getItems().setAll(users);

            addSystem("Active users updated.");
            return;
        }

        // System messages centered
        if (line.contains("(SYSTEM):")) {
            addSystem(line);
            return;
        }

        // Normal message pattern: "[HH:mm] user: msg"
        // We treat everything received from server as "OTHER" because server doesn't echo to sender.
        addOther(line);
    }

    // ---------- Message creation ----------
    private void addOwn(String text) {
        chatList.getItems().add(Message.own(now(), myUsername, text));
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    private void addOther(String rawLine) {
        chatList.getItems().add(Message.other(rawLine));
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    private void addSystem(String text) {
        chatList.getItems().add(Message.system(text));
        chatList.scrollTo(chatList.getItems().size() - 1);
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    @Override
    public void stop() {
        model.disconnect();
    }

    // ---------- Message model ----------
    private static class Message {
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

        static Message other(String rawLine) {
            // keep the server line as text for display
            return new Message(Kind.OTHER, "", "", rawLine);
        }

        static Message system(String text) {
            return new Message(Kind.SYSTEM, "", "", text);
        }
    }

    // ---------- WhatsApp-like bubble cell ----------
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
                box.setPadding(new Insets(6, 10, 6, 10));

                setGraphic(box);
                return;
            }

            // Bubble content
            VBox bubble = new VBox(3);
            bubble.getStyleClass().add("bubble");

            Label text = new Label(msg.kind == Message.Kind.OTHER ? msg.text : msg.text);
            text.setWrapText(true);

            // For own messages, show time only; for other messages, raw line already includes time/sender.
            // If later you want perfect parsing, we can split "[HH:mm] user: msg" properly.
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
            row.setPadding(new Insets(4, 10, 4, 10));

            if (msg.kind == Message.Kind.OWN) {
                row.setAlignment(Pos.CENTER_RIGHT);
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
            }

            setGraphic(row);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}