import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

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

    static final String DB_URL = "jdbc:sqlite:../user_records.db";
    private static Connection dbConnection;

    @FXML
    private void initialize() {
        // Configure TableColumn bindings
        rankColumn.setCellValueFactory(cellData -> cellData.getValue().rank());
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().username());
        winsColumn.setCellValueFactory(cellData -> cellData.getValue().wins());
        lossesColumn.setCellValueFactory(cellData -> cellData.getValue().losses());

        loadUserRecords();
    }

    private void loadUserRecords() {
        try {
            ObservableList<UserRecord> userRecords = FXCollections.observableArrayList();

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Connect to database
            dbConnection = DriverManager.getConnection(DB_URL);
            Statement stmt = dbConnection.createStatement();

            // Query to fetch all user records
            String query = """
                            SELECT username, wins, losses FROM user_records ORDER BY wins DESC, losses ASC, username ASC
                            """;
            ResultSet rs = stmt.executeQuery(query);

            // Add data to userRecords list
            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                userRecords.add(new UserRecord(rank++, username, wins, losses));
            }

            // Set the items in the TableView
            leaderboardTable.setItems(userRecords);

        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("SQL Exception: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseButtonClick() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
        Parent root = loader.load();

        // Create a new scene with the previous scene layout
        Scene previousScene = new Scene(root);

        // Get current window from the button
        Stage currentStage = (Stage) closeButton.getScene().getWindow();

        // Set the new scene on the same stage
        currentStage.setScene(previousScene);
        currentStage.show();
    }
}
