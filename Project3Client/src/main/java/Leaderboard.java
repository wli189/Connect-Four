import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;


public class Leaderboard {
    @FXML
    private TableView<UserRecord> leaderboardTable;

    @FXML
    private TableColumn<UserRecord, Number> rankColumn;

    @FXML
    private TableColumn<UserRecord, String> usernameColumn;

    @FXML
    private TableColumn<UserRecord, Number> winsColumn;

    @FXML
    private TableColumn<UserRecord, Number> lossesColumn;

    @FXML
    private Button closeButton;

    @FXML
    private Label fullscreenMessage;

    @FXML
    private void initialize() {
        // Configure TableColumn bindings
        rankColumn.setCellValueFactory(cellData -> cellData.getValue().rank());
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().username());
        winsColumn.setCellValueFactory(cellData -> cellData.getValue().wins());
        lossesColumn.setCellValueFactory(cellData -> cellData.getValue().losses());
    }


    @FXML
    private void handleCloseButtonClick() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
        Parent root = loader.load();

        // Create a new scene with the previous scene layout
        Scene previousScene = new Scene(root);

        root.setOpacity(0); // Start fully transparent
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
        fadeTransition.setFromValue(0.0); // Start opacity
        fadeTransition.setToValue(1.0);   // End opacity

        // Get current window from the button
        Stage currentStage = (Stage) closeButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(previousScene);
        fadeTransition.play();
        currentStage.show();
    }

    public void updateLeaderboard(String leaderboardData) {
        System.out.println(leaderboardData);
        ObservableList<UserRecord> records = FXCollections.observableArrayList();
        try {
            // Example format: "rank1,username1,wins1,losses1;rank2,username2,wins2,losses2"
            String[] entries = leaderboardData.split(";");
            for (String entry : entries) {
                String[] fields = entry.split(",");
                if (fields.length == 4) {
                    int rank = Integer.parseInt(fields[0].trim());
                    String username = fields[1].trim();
                    int wins = Integer.parseInt(fields[2].trim());
                    int losses = Integer.parseInt(fields[3].trim());
                    records.add(new UserRecord(rank, username, wins, losses));
                }
            }
            // Update the TableView on the JavaFX Application Thread
            Platform.runLater(() -> leaderboardTable.setItems(records));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Show a message
    public void showMessage(String message, boolean isError) {
        Platform.runLater(() -> {
            fullscreenMessage.setText(message);
            fullscreenMessage.setStyle("-fx-background-color: " + (!isError ? "rgba(0, 128, 0, 0.7)" : "rgba(255, 0, 0, 0.7)") + "; -fx-text-fill: white;");

            // Fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), fullscreenMessage);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fullscreenMessage.setVisible(true);
            fadeIn.play();

            // Fade out after 2 seconds
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), fullscreenMessage);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(1));
            fadeOut.setOnFinished(event -> {
                fullscreenMessage.setVisible(false);
            });
            fadeOut.play();
        });
    }
}
