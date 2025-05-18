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
    private Button rematchButton;

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

    @FXML
    private Label fullscreenMessage;

    private final int rows = 6;
    private final int cols = 7;

    @FXML
    public void initialize() {
        drawEmptyBoard(); // Draw an empty board before the first move
        sendButton.setOnAction(e -> sendMessage());
        rematchButton.setVisible(false);
    }

    public void drawEmptyBoard() {
        boardGrid.getChildren().clear();
        boardGrid.setHgap(10); // Add 10px between cells
        boardGrid.setVgap(10);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Circle circle = new Circle(30);
                circle.getStyleClass().add("game-circle");
                int clickedCol = col;
                circle.setOnMouseClicked(event -> {
                    handleColumnClick(clickedCol);
                });
                boardGrid.add(circle, col, row);
            }
        }
    }

    public void updateBoard(int[][] board) {
        boardGrid.getChildren().clear(); // Clear the previous board
        boardGrid.setHgap(10); // Add 10px between cells
        boardGrid.setVgap(10);

        // Draw the new board
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Circle circle = new Circle(30);
                int value = board[row][col];
                if (value == 1) {
                    circle.getStyleClass().add("game-circle-red");
                } else if (value == 2) {
                    circle.getStyleClass().add("game-circle-yellow");
                } else {
                    circle.getStyleClass().add("game-circle");
                }

                int clickedCol = col;
                circle.setOnMouseClicked(event -> {
                    handleColumnClick(clickedCol);
                });

                boardGrid.add(circle, col, row);
            }
        }
    }

    private void handleColumnClick(int col) {
        GuiClient.getClient().sendMove(col);
    }

    // will send a message to the server for a rematch
    @FXML
    private void handleRematchButtonClick() {
        Client client = GuiClient.getClient();
        if (client != null) {
            client.resetGame();  // Clear local game state
            client.sendRematchRequest();  // Ask server to re-pair players
            drawEmptyBoard();  // Reset board visuals
            showMessage("Rematch request sent. Waiting for opponent...", false);
            // Will disable the rematch button as it is waiting
            rematchButton.setVisible(false);
        }
    }
    @FXML
    private void handleBackButtonClick() throws Exception {
        // Disconnect from the server
        Client client = GuiClient.getClient();
        if (client != null) {
            client.disconnect();
            GuiClient.setClient(null);
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
            client.sendUserMessage(client.getUsername() + ": " + message + "\n");
        }
    }
    public void receiveMessage(String message) {
        Platform.runLater(() -> chatOutput.appendText(message));
    }

    // Show a message when the game ends
    public void showEndMessage(String message) {
        Platform.runLater(() -> {
            // Determine background color based on game outcome
            String backgroundColor = "";
            String textColor = "";
            if (message.toLowerCase().contains("draw")) {
                backgroundColor = "rgba(255, 255, 0, 0.7)"; // Yellow for draw
                textColor = "Black";
            } else if (message.toLowerCase().contains("won")) {
                backgroundColor = "rgba(0, 128, 0, 0.7)"; // Green for win
                textColor = "White";
            } else if (message.toLowerCase().contains("lost")) {
                backgroundColor = "rgba(255, 0, 0, 0.7)"; // Red for loss
                textColor = "White";
            }

            fullscreenMessage.setText(message);
            fullscreenMessage.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + ";");

            // Fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), fullscreenMessage);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fullscreenMessage.setVisible(true);
            fadeIn.play();

            // Fade out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), fullscreenMessage);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(2));
            fadeOut.setOnFinished(event -> {
                fullscreenMessage.setVisible(false);
                rematchButton.setVisible(true);
            });
            fadeOut.play();
        });
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

            // Fade out
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

    public void showUsername(String username) {
    	usernameLabel.setText(username);
    }

}
