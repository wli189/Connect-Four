import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.stage.Stage;

public class GuiClient extends Application{

	private static Client clientThread;

	public static Client getClient() {
		return clientThread;
	}

	public static void setClient(Client client) {
		clientThread = client;
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Load the FXML file from the resources directory
		FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
		Parent root = loader.load();

		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	private static GameLayout gameController;

	public static void setGameController(GameLayout controller) {
		gameController = controller;
	}

	public static GameLayout getGameController() {
		return gameController;
	}

}
