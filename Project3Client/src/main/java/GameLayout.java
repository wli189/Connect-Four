import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameLayout {
    @FXML
    private GridPane boardGrid;

    @FXML
    private Label messageLabel;

    @FXML
    private Button backButton;

    @FXML
    private TextField chatInput;

    @FXML
    private TextArea chatOutput;

    @FXML
    private Button sendButton;

    @FXML
    private Label usernameLabel;

    private final int rows = 6;
    private final int cols = 7;

    @FXML
    public void initialize() {
        drawEmptyBoard(); // Draw an empty board before the first move
        sendButton.setOnAction(e -> sendMessage());
    }

    public void drawEmptyBoard() {
        boardGrid.getChildren().clear();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Circle circle = new Circle(30);
                circle.setFill(Color.LIGHTGRAY);
                int clickedCol = col;
                circle.setOnMouseClicked(event -> {
                    handleColumnClick(clickedCol);
                    messageLabel.setText("");  // Clear the message label
                });
                boardGrid.add(circle, col, row);
            }
        }
    }

    public void updateBoard(int[][] board) {
        boardGrid.getChildren().clear(); // Clear the previous board

        // Draw the new board
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Circle circle = new Circle(30);
                int value = board[row][col];
                if (value == 1) {
                    circle.setFill(Color.RED);
                } else if (value == 2) {
                    circle.setFill(Color.YELLOW);
                } else {
                    circle.setFill(Color.LIGHTGRAY);
                }

                int clickedCol = col;
                circle.setOnMouseClicked(event -> {
                    handleColumnClick(clickedCol);
                    messageLabel.setText("");  // Clear the message label
                });

                boardGrid.add(circle, col, row);
            }
        }
    }
    private void handleColumnClick(int col) {
//        System.out.println("Clicked column: " + col);
        GuiClient.getClient().sendMove(col);
    }

    @FXML
    private void handleBackButtonClick() throws Exception {
        Client client = GuiClient.getClient();
        if (client != null) {
            client.disconnect(); // or whatever method you're using
            GuiClient.setClient(null); // clear the reference if needed
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
        Parent root = loader.load();

        // Create a new scene with the previous scene layout
        Scene previousScene = new Scene(root);

        root.setOpacity(0); // Start fully transparent
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
        fadeTransition.setFromValue(0.0); // Start opacity
        fadeTransition.setToValue(1.0);   // End opacity

        // Get current window from the button
        Stage currentStage = (Stage) backButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(previousScene);
        fadeTransition.play();
        currentStage.show();
    }
    // outputs the message on the message board
    private void sendMessage() {
        Client client = GuiClient.getClient();
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            chatOutput.appendText(client.getUsername() + ": " + message + "\n"); // Display your message
            chatInput.clear();
//            System.out.println(client.getUsername() + ": " + message);
            GuiClient.getClient().sendUserMessage(client.getUsername() + ": " + message + "\n");
        }
    }
    public void receiveMessage(String message) {
        Platform.runLater(() -> chatOutput.appendText(message));
    }

    // Show a message when the game ends
    public void showEndMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            // Navigate back to ClientLayout after the alert
            try {
                Client clientThread = GuiClient.getClient();
                if (clientThread != null) {
                    clientThread.disconnect(); // Disconnect the client
                    GuiClient.setClient(null); // Clear the client reference
                }

                // Load the ClientLayout
                FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
                Parent root = loader.load();

                // Create the new scene
                Scene clientScene = new Scene(root);

                root.setOpacity(0); // Start fully transparent
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
                fadeTransition.setFromValue(0.0); // Start opacity
                fadeTransition.setToValue(1.0);   // End opacity

                // Get the current stage
                Stage currentStage = (Stage) messageLabel.getScene().getWindow();

                // Set the new scene
                currentStage.setScene(clientScene);
                fadeTransition.play();
                currentStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Show a notice message
    public void showMessage(String message) {
        messageLabel.setText(message);
    }

    public void showUsername(String username) {
    	usernameLabel.setText(username);
    }

    // Show a duplicate username message
    public void showDuplicateUsernameMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Duplicate Username");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            // Navigate back to ClientLayout after the alert
            try {
                Client clientThread = GuiClient.getClient();
                if (clientThread != null) {
                    clientThread.disconnect(); // Disconnect the client
                    GuiClient.setClient(null); // Clear the client reference
                }

                // Load the ClientLayout
                FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
                Parent root = loader.load();

                // Create the new scene
                Scene clientScene = new Scene(root);

                root.setOpacity(0); // Start fully transparent
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), root); // 500ms duration
                fadeTransition.setFromValue(0.0); // Start opacity
                fadeTransition.setToValue(1.0);   // End opacity

                // Get the current stage
                Stage currentStage = (Stage) messageLabel.getScene().getWindow();

                // Set the new scene
                currentStage.setScene(clientScene);
                fadeTransition.play();
                currentStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
