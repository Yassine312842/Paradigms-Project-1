package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatApp extends Application {


    private static final String LOGIN_BG_PATH = "/images/login_bg.jpg.jpg";
    private static final String LOGIN_LOGO_PATH = "/images/aui_logo.png.png";

    private static final String CHAT_BG_PATH = "/images/login_bg.jpg.jpg";

    private static final String AUI_GREEN = "#0B5431";

    private final ClientModel model = new ClientModel();


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
        error.setVisible(false);
        error.setStyle("-fx-text-fill: #ff3b30; -fx-font-size: 12px; -fx-font-weight: 800;");

        Button connectBtn = new Button("Connect");
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: 900;
                -fx-padding: 12 16 12 16;
                -fx-background-radius: 12;
                -fx-cursor: hand;
                """.formatted(AUI_GREEN));

        StackPane root = new StackPane();

        Region fallback = new Region();
        fallback.setStyle("-fx-background-color: #0b1f16;");

        ImageView bg = new ImageView();
        Image bgImg = tryLoadImage(LOGIN_BG_PATH);
        if (bgImg != null) bg.setImage(bgImg);

        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(stage.widthProperty());
        bg.fitHeightProperty().bind(stage.heightProperty());

        Region overlay = new Region();
        overlay.setStyle("""
                -fx-background-color: linear-gradient(to bottom right,
                    rgba(11,84,49,0.72),
                    rgba(0,0,0,0.55));
                """);
        overlay.prefWidthProperty().bind(stage.widthProperty());
        overlay.prefHeightProperty().bind(stage.heightProperty());

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(26, 30, 22, 30));
        card.setMaxWidth(540);
        card.setStyle("""
                -fx-background-color: rgba(255,255,255,0.72);
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(255,255,255,0.35);
                -fx-border-width: 1;
                """);
        card.setEffect(new DropShadow(28, Color.rgb(0, 0, 0, 0.28)));

        ImageView logo = new ImageView();
        Image logoImg = tryLoadImage(LOGIN_LOGO_PATH);
        if (logoImg != null) {
            logo.setImage(logoImg);
            logo.setFitHeight(150);
            logo.setPreserveRatio(true);
        }

        Label title = new Label("AUI General Chat");
        title.setStyle("""
                -fx-font-size: 26px;
                -fx-font-weight: 900;
                -fx-text-fill: %s;
                """.formatted(AUI_GREEN));

        Label subtitle = new Label("Sign in to join the conversation.");
        subtitle.setStyle("""
                -fx-font-size: 13px;
                -fx-font-weight: 700;
                -fx-text-fill: rgba(0,0,0,0.55);
                """);

        styleField(ipField);
        styleField(portField);
        styleField(userField);

        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setFillWidth(true);
        form.getChildren().addAll(
                labelSmall("Server IP"),
                ipField,
                labelSmall("Port"),
                portField,
                labelSmall("Username"),
                userField
        );

        Label tip = new Label("Leave username empty to join as read-only.");
        tip.setStyle("-fx-text-fill: rgba(0,0,0,0.55); -fx-font-size: 12px; -fx-font-weight: 700;");

        VBox buttons = new VBox(10, connectBtn, tip);
        buttons.setAlignment(Pos.CENTER);
        buttons.setFillWidth(true);

        card.getChildren().addAll(
                logo,
                title,
                subtitle,
                new Separator(),
                form,
                error,
                buttons
        );

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().addAll(fallback, bg, overlay, card);

        Runnable doConnect = () -> {
            String ip = ipField.getText().trim();
            String portStr = portField.getText().trim();
            String username = (userField.getText() == null) ? "" : userField.getText().trim();

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception ex) {
                showError(error, "Invalid port.");
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
                showError(error, "Failed to connect. Is the server running?");
            }
        };

        connectBtn.setOnAction(e -> doConnect.run());
        userField.setOnAction(e -> doConnect.run());
        portField.setOnAction(e -> doConnect.run());

        Scene scene = new Scene(root, 1100, 650);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    private Scene buildChatScene(Stage stage) {
        HBox topbar = new HBox(12);
        topbar.getStyleClass().add("topbar");

        Image topLogoImg = tryLoadImage(
                "/images/aui_logo_white.png",
                "/images/aui_logo_white.png.png",
                "/images/aui_logo.png",
                "/images/aui_logo.png.png",
                "/aui_logo.png"
        );

        ImageView logoView = new ImageView();
        if (topLogoImg != null) {
            logoView.setImage(topLogoImg);
            logoView.setFitHeight(56);
            logoView.setPreserveRatio(true);
        }

        statusLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (topLogoImg != null) {
            topbar.getChildren().addAll(logoView, spacer, statusLabel);
        } else {
            Label fallbackTitle = new Label("AUI General Chat");
            fallbackTitle.getStyleClass().add("topbar-title");
            topbar.getChildren().addAll(fallbackTitle, spacer, statusLabel);
        }

        chatList.getStyleClass().add("chat-list");
        chatList.setFocusTraversable(false);
        chatList.setCellFactory(lv -> new MessageCell());

        StackPane chatCenter = new StackPane();
        chatCenter.getStyleClass().add("chat-center");

        ImageView chatBg = new ImageView();
        Image chatBgImg = tryLoadImage(CHAT_BG_PATH);
        if (chatBgImg != null) chatBg.setImage(chatBgImg);

        chatBg.setOpacity(0.08);
        chatBg.setPreserveRatio(false);
        chatBg.fitWidthProperty().bind(chatCenter.widthProperty());
        chatBg.fitHeightProperty().bind(chatCenter.heightProperty());

        chatCenter.getChildren().addAll(chatBg, chatList);

        Label usersTitle = new Label("Users");
        usersTitle.getStyleClass().add("users-title");

        VBox right = new VBox(10, usersTitle, usersList, refreshUsersBtn);
        right.getStyleClass().add("users-panel");
        VBox.setVgrow(usersList, Priority.ALWAYS);

        refreshUsersBtn.getStyleClass().add("secondary-btn");
        refreshUsersBtn.setOnAction(e -> model.sendMessage("allUsers"));

        messageField.setPromptText("Type a message...");
        sendBtn.getStyleClass().add("primary-btn");

        Runnable sendAction = () -> {
            if (readOnlyMode) return;
            String text = messageField.getText();
            if (text == null || text.isBlank()) return;

            addOwn(text);
            model.sendMessage(text);
            messageField.clear();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        messageField.setOnAction(e -> sendAction.run());

        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.getStyleClass().add("danger-btn");
        disconnectBtn.setOnAction(e -> {
            if (model.isConnected()) model.sendMessage("bye");
            model.disconnect();
            stage.setScene(buildLoginScene(stage));
        });

        HBox inputBar = new HBox(10, messageField, sendBtn, disconnectBtn);
        inputBar.getStyleClass().add("input-bar");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("chat-root");
        root.setTop(topbar);
        root.setCenter(chatCenter);
        root.setRight(right);
        root.setBottom(inputBar);

        setChatControlsEnabled(false);

        Scene scene = new Scene(root, 1100, 700);
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

    private void handleIncomingLine(String line) {
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

        if (line.contains("(SYSTEM):")) {
            addSystem(line);
            return;
        }

        addOther(line);
    }

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

    private static void showError(Label error, String msg) {
        error.setText(msg);
        error.setVisible(true);
    }

    private static Label labelSmall(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(0,0,0,0.78); -fx-font-weight: 900;");
        return l;
    }

    private static void styleField(TextField tf) {
        tf.setStyle("""
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-border-color: rgba(11,84,49,0.22);
                -fx-border-width: 1.2;
                -fx-padding: 10 12 10 12;
                -fx-font-size: 13px;
                -fx-background-color: rgba(255,255,255,0.98);
                """);
    }

    private Image tryLoadImage(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null) return null;
            return new Image(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }

    private Image tryLoadImage(String... paths) {
        for (String p : paths) {
            Image img = tryLoadImage(p);
            if (img != null) return img;
        }
        return null;
    }

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
            return new Message(Kind.OTHER, "", "", rawLine);
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
                box.setPadding(new Insets(8, 12, 8, 12));

                setGraphic(box);
                return;
            }

            VBox bubble = new VBox(4);
            bubble.getStyleClass().add("bubble");

            Label text = new Label(msg.text);
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
            row.setPadding(new Insets(6, 14, 6, 14));
            row.setAlignment(msg.kind == Message.Kind.OWN ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            setGraphic(row);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}