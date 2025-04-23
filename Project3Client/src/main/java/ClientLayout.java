import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
        Client clientThread = new Client();
        clientThread.setUsername(username);
        clientThread.start();
        GuiClient.setClient(clientThread);

        // Load the game layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameLayout.fxml"));
        Parent root = loader.load();

        GameLayout controller = loader.getController();
        GuiClient.setGameController(controller);

        // Get current window from the button
        Stage currentStage = (Stage) startButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(new Scene(root));
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

        // Get current window from the button
        Stage currentStage = (Stage) leaderboardButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(new Scene(root));
        currentStage.show();
    }
}
