import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.control.Label;
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
    private Label loginMessage;

    @FXML
    private TextField passwordInput;

    @FXML
    private void handleStartButtonClick() throws Exception {
        String username = usernameInput.getText().trim();
        String password = passwordInput.getText().trim();
        if (username.isEmpty()) {
            // Show error message to the user
            showLoginMessage("Username cannot be empty!", false);
            return;
        }
        if (password.isEmpty()) {
            showLoginMessage("Password cannot be empty!", false);
            return;
        }

        Client clientThread = new Client(this);
        clientThread.setUsername(username);
        clientThread.setPassword(password);
        clientThread.start();
        GuiClient.setClient(clientThread);

//        // Show connecting message
//        showLoginMessage("Connecting to server...", true);
    }

    private void switchToGameLayout() throws Exception {
        // Load the game layout
        FXMLLoader loader = new FXMLLoader(ClientLayout.class.getResource("gameLayout.fxml"));
        Parent root = loader.load();

        GameLayout gameController = loader.getController();
        Client client = GuiClient.getClient();
        if (client != null) {
            client.setGameController(gameController);
        }

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

    public void showLoginMessage(String message, boolean isSuccess) {
        loginMessage.setText(message);
        loginMessage.setStyle("-fx-background-color: " + (isSuccess ? "rgba(0, 128, 0, 0.7)" : "rgba(255, 0, 0, 0.7)") + "; -fx-text-fill: white;");

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginMessage);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        loginMessage.setVisible(true);
        fadeIn.play();

        // Fade out after 2 seconds
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), loginMessage);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(2));
        fadeOut.setOnFinished(event -> {
            loginMessage.setVisible(false);
            if (isSuccess) {
                try {
                    switchToGameLayout();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        fadeOut.play();
    }
}
