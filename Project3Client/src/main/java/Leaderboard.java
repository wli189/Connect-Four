import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Leaderboard {
    @FXML
    private TableView<UserRecord> leaderboardTable;

    @FXML
    private TableColumn<UserRecord, String> usernameColumn;

    @FXML
    private TableColumn<UserRecord, Number> winsColumn;

    @FXML
    private TableColumn<UserRecord, Number> lossesColumn;

    @FXML
    private Button closeButton;

    private static final String RECORDS_FILE = "../user_records.json";

    @FXML
    private void initialize() {
        // Configure TableView columns
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        winsColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getWins()));
        lossesColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLosses()));

        // Table sorting by wins
        winsColumn.setSortType(TableColumn.SortType.DESCENDING);

        // Load user_records.json
        loadUserRecords();
    }

    private void loadUserRecords() {
        try (FileReader reader = new FileReader(RECORDS_FILE)) {
            // Initialize Gson
            Gson gson = new GsonBuilder().create();

            // Read JSON file to java arraylist
            ArrayList<UserRecord> userRecords = gson.fromJson(reader, new TypeToken<ArrayList<UserRecord>>(){}.getType());
            if (userRecords == null) {
                userRecords = new ArrayList<>();
            }

            // Convert List to ObservableList for TableView
            ObservableList<UserRecord> observableRecords = FXCollections.observableArrayList(userRecords);

            // Set the items in the TableView
            leaderboardTable.setItems(observableRecords);

            // Show sorted table
            Platform.runLater(() -> {
                leaderboardTable.getSortOrder().add(winsColumn);
                leaderboardTable.sort();
            });
        } catch (IOException e) {
            System.err.println("Error reading user_records.json: " + e.getMessage());
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
