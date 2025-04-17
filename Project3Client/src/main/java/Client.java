import Message.*;

import javafx.application.Platform;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class Client extends Thread {
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private boolean gameEnded = false;

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
			System.out.println("Client connected to server");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while (true) {
			try {
				Object obj = in.readObject();

				if (obj instanceof GameState gameState) {
					int[][] board = gameState.getBoard();
					String status = gameState.getStatus();
					int currentPlayer = gameState.getCurrentPlayer();

					Platform.runLater(() -> {
						try {
							GameLayout controller = GuiClient.getGameController();
							System.out.println("Updating board in UI with: " + Arrays.deepToString(board));
							controller.updateBoard(board);
							System.out.println("Board updated in UI");

							if (!gameEnded) {
								if (status.equals("WIN")) {
									controller.showEndMessage("Player " + currentPlayer + " wins!");
									gameEnded = true; // Prevent further end messages
								} else if (status.equals("DRAW")) {
									controller.showEndMessage("It's a draw!");
									gameEnded = true; // Prevent further end messages
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				} else if (obj instanceof String message) {
					System.out.println(message);
					if (message.startsWith("ERROR:") || message.startsWith("PLAYER_ID:")) {
						Platform.runLater(() -> {
							try {
								GameLayout controller = GuiClient.getGameController();
								if (message.startsWith("ERROR:")) {
									controller.showError(message.substring(6));
								} else {
									controller.showMessage("You are Player " + message.substring(10));
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}

	public void disconnect() {
		try {
			if (in != null) in.close();
			if (out != null) out.close();
			if (socketClient != null && !socketClient.isClosed()) {
				socketClient.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(String data) {
		try {
			out.writeObject(data);
			out.flush();
			System.out.println("Sent String: " + data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMove(int col) {
		try {
			Message moveMessage = new Message("MAKE_MOVE", String.valueOf(col));
			out.writeObject(moveMessage);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Reset game state for a new game
	public void resetGame() {
		gameEnded = false;
	}
}