package Gui;

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

	public static void main(String[] args) {
//		Client clientThread = new Client();
//		clientThread.start();
//		Scanner s = new Scanner(System.in);
//		while (s.hasNext()){
//			String x = s.next();
//			clientThread.send(x);
//		}

		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		clientThread = new Client();
		clientThread.start();

		Parent root = FXMLLoader.load(getClass().getResource("clientLayout.fxml"));
		primaryStage.setScene(new Scene(root));
		primaryStage.setTitle("Connect Four");
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
