import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class ClientLayout {

    @FXML
    private Button startButton;

    @FXML
    private Button quitButton;

    @FXML
    private TextField usernameInput;

    @FXML
    private Button leaderboardButton;

    @FXML
    private void handleStartButtonClick() throws Exception {
        String username = usernameInput.getText().trim();
        if (username.isEmpty()) {
            // Show error message to the user
            showError("Username cannot be empty!");
            return; // Stop execution
        }

        Client clientThread = new Client();
        clientThread.setUsername(username);
        clientThread.start();
        GuiClient.setClient(clientThread);

        // Load the game layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameLayout.fxml"));
        Parent root = loader.load();

        GameLayout controller = loader.getController();
        GuiClient.setGameController(controller);

        // Create the new scene
        Scene gameScene = new Scene(root);

        root.setOpacity(0); // Start fully transparent
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
        fadeTransition.setFromValue(0.0); // Start opacity
        fadeTransition.setToValue(1.0);   // End opacity

        // Get current window from the button
        Stage currentStage = (Stage) startButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(gameScene);
        fadeTransition.play();
        currentStage.show();
    }

    @FXML
    private void handleQuitButtonClick() {
        System.out.println("Exiting.");
        Platform.exit();
        System.exit(0);  // Close the program
    }

    @FXML
    private void handleLeaderboardButton() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("leaderboard.fxml"));
        Parent root = loader.load();

        // Create a new scene
        Scene leaderboardScene = new Scene(root);

        root.setOpacity(0); // Start fully transparent
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
        fadeTransition.setFromValue(0.0); // Start opacity
        fadeTransition.setToValue(1.0);   // End opacity

        // Get current window from the button
        Stage currentStage = (Stage) leaderboardButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(leaderboardScene);
        fadeTransition.play();
        currentStage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
