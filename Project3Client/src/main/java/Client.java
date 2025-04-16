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

							if (status.equals("WIN")) {
								controller.showEndMessage("Player " + currentPlayer + " wins!");
							} else if (status.equals("DRAW")) {
								controller.showEndMessage("It's a draw!");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
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
}