import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.stage.Stage;

public class ClientLayout {

    @FXML
    private Button startButton;

    @FXML
    private Button quitButton;

    @FXML
    private void handleStartButtonClick() throws Exception {
        System.out.println("Start Game clicked!");
        // Load the game layout
        Parent root = FXMLLoader.load(getClass().getResource("gameLayout.fxml"));

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
}
