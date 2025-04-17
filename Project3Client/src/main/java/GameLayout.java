import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GameLayout {
    @FXML
    private GridPane boardGrid;

    @FXML
    private Label messageLabel;

    private final int rows = 6;
    private final int cols = 7;

    @FXML
    public void initialize() {
        drawEmptyBoard(); // Draw an empty board before the first move
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
                });
                boardGrid.add(circle, col, row);
            }
        }
    }

    public void updateBoard(int[][] board) {
        boardGrid.getChildren().clear(); // Clear previous board

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
                });

                boardGrid.add(circle, col, row);
            }
        }
    }
    private void handleColumnClick(int col) {
        System.out.println("Clicked column: " + col);
        GuiClient.getClient().sendMove(col);
    }

    public void showEndMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showError(String error) {
        messageLabel.setText("Error: " + error);
    }

    public void showMessage(String message) {
        messageLabel.setText(message);
    }
}
