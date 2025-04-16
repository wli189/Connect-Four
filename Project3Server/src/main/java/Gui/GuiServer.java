package Gui;

import javafx.application.Application;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GuiServer extends Application{

	public static void main(String[] args) {
		Server serv = new Server();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setScene(new Scene(new TextField("I am not yet implemented")));
		primaryStage.setTitle("Server");
		primaryStage.show();
		
	}



}
