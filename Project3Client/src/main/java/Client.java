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
	private String username;
	private String player1Username;
	private String player2Username;
	private int playerID;

	public Client() {
		this.username = "";
		this.player1Username = "";
		this.player2Username = "";
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
			System.out.println("Client connected to server");

			// Send username to server
			out.writeObject("USERNAME:" + username);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while (true) {
			try {
				Object obj = in.readObject();

				// Handle game state update
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

				}
				// gets the users message to send to the other player
				else if (obj instanceof Message message) {
					if ("CHAT".equals(message.getType())) {
						String chatText = message.toString();  // message that was sent
						Platform.runLater(() -> {
							GameLayout controller = GuiClient.getGameController();
							controller.receiveMessage(chatText);
						});
					}
				}
				// Print out message from server
				else if (obj instanceof String message) {
//					System.out.println(message);
					if (message.startsWith("ERROR:") || message.startsWith("PLAYER_ID:") || message.startsWith("SERVER:")) {
						Platform.runLater(() -> {
							try {
								GameLayout controller = GuiClient.getGameController();
								if (message.startsWith("ERROR:")) {
									controller.showMessage(message.substring(7));
								} else if (message.startsWith("PLAYER_ID:")) {
									// Parse out the player ID and username from server
									String[] parts = message.split(" - ", 2);
									String id = parts[0].substring(11);
									playerID = Integer.parseInt(id.trim());
									if (playerID == 1) {
										player1Username = parts[1];
									} else if (playerID == 2) {
										player2Username = parts[1];
									}
                                    controller.showMessage("You are Player " + playerID + " - " + this.getUsername() + "\n" + player1Username + " goes first");
                                } else if (message.startsWith("SERVER:")) {
									controller.showMessage(message.substring(8));
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

	// Disconnect from server
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

	public void sendUserMessage(String data) {
		try {
			Message chatMessage = new Message("CHAT", data);
			out.writeObject(chatMessage);
			out.flush();
//			System.out.println("Sent String: " + data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// Send a move to the server
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

	// Set the username for the client
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		if (playerID == 1) {
			return player1Username;
		} else if (playerID == 2) {
			return player2Username;
		} else {
			return null;
		}
	}
}