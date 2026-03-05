package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML
    private Circle statusCircle;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea logArea;

    @FXML
    private Label clientCountLabel;

    @FXML
    private ListView<ClientEntry> clientListView;

    private final ObservableList<ClientEntry> clientEntries =
            FXCollections.observableArrayList();

    private ServerModel model;

    public static class ClientEntry {

        public final String username;
        public final String colorHex;

        public ClientEntry(String username, String colorHex) {
            this.username = username;
            this.colorHex = colorHex;
        }

        @Override
        public String toString() {
            return username;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        clientListView.setItems(clientEntries);

        clientListView.setCellFactory(lv -> new ListCell<>() {

            @Override
            protected void updateItem(ClientEntry entry, boolean empty) {

                super.updateItem(entry, empty);

                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Circle dot = new Circle(6, Color.web(entry.colorHex));

                Label name = new Label(entry.username);
                name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                name.setTextFill(Color.web("#222"));

                HBox row = new HBox(10, dot, name);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 8, 4, 8));

                setGraphic(row);
            }
        });

        clientEntries.addListener((ListChangeListener<ClientEntry>) c ->
                clientCountLabel.setText(clientEntries.size() + " connected"));
    }

    public void startServer() {

        model = ServerModel.fromProperties();

        model.setOnLog(msg -> Platform.runLater(() -> {

            logArea.appendText(msg + "\n");

            logArea.setScrollTop(Double.MAX_VALUE);
        }));

        model.setOnClientJoined((username, colorHex) ->
                Platform.runLater(() ->
                        clientEntries.add(new ClientEntry(username, colorHex))
                ));

        model.setOnClientLeft(username ->
                Platform.runLater(() ->
                        clientEntries.removeIf(e ->
                                e.username != null && e.username.equals(username))
                ));

        Thread serverThread = new Thread(() -> {

            Platform.runLater(() -> {

                statusCircle.setFill(Color.web("#43A047"));

                statusLabel.setText(
                        "Online  •  " + model.getBindIp() + ":" + model.getPort()
                );
            });

            model.start();

        }, "server-main");

        serverThread.setDaemon(true);

        serverThread.start();
    }
}