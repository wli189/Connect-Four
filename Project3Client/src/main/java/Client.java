import Message.*;

import javafx.application.Platform;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class Client extends Thread {
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private boolean gameEnded = false;
	private String username;
	private String password;
	private String player1Username;
	private String player2Username;
	private String opponentPlayerUsername;
	private int playerID;
	private int opponentPlayerID;
	private String winner;
	private ClientLayout controller;
	private GameLayout gameController;
	private final Queue<Runnable> pendingMessages = new LinkedList<>();
	private Leaderboard leaderboardController;

	public Client(ClientLayout controller) {
		this.controller = controller;
		this.username = "";
		this.player1Username = "";
		this.player2Username = "";
	}

	public void setGameController(GameLayout gameController) {
		this.gameController = gameController;
		// Process any pending messages
		synchronized (pendingMessages) {
			while (!pendingMessages.isEmpty()) {
				Platform.runLater(pendingMessages.poll());
			}
		}
	}

	public void setLeaderboardController(Leaderboard leaderboardController) {
		this.leaderboardController = leaderboardController;
	}

	public void sendLeaderboardRequest() {
		try {
			Message leaderboardRequest = new Message("LEADERBOARD_REQUEST", "Requesting leaderboard");
			out.writeObject(leaderboardRequest);
			out.flush();
		} catch (IOException e) {
			System.err.println("Error sending leaderboard request: " + e.getMessage());
			Platform.runLater(() -> controller.showLoginMessage("Error sending leaderboard request!", false));
		}
	}

	public void sendLoginRequest() {
		try {
			Message loginRequest = new Message("LOGIN", username + ":" + password);
			out.writeObject(loginRequest);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			Platform.runLater(() -> controller.showLoginMessage("Error sending login request!", false));
			disconnect();
		}
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> controller.showLoginMessage("Failed to connect to server!", false));
			return;
		}

		while (true) {
			try {
				Object obj = in.readObject();
				// Handle login responses
//				if (obj instanceof String message) {
//					if (message.startsWith("LOGIN_SUCCESS:")) {
//						Platform.runLater(() -> controller.showLoginMessage("Successfully Logged In!", true));
//						continue;
//					} else if (message.startsWith("LOGIN_ERROR:") || message.startsWith("USERNAME_ERROR:")) {
//						String errorMessage = message.startsWith("LOGIN_ERROR:") ? message.substring(12) : message.substring(15);
//						Platform.runLater(() -> controller.showLoginMessage(errorMessage, false));
//						disconnect();
//						break;
//					}
//				}

				// Handle game state update
				if (obj instanceof GameState gameState) {
					int[][] board = gameState.getBoard();
					String status = gameState.getStatus();
					int currentPlayer = gameState.getCurrentPlayer();

					Platform.runLater(() -> {
						handleGameState(gameState);
					});
				}
				// Handle messages
				if (obj instanceof Message message) {
					if ("LOGIN_SUCCESS".equals(message.getType())) {
						Platform.runLater(() -> {
							controller.showLoginMessage("Successfully Logged In!", true);
						});
					}
					if ("LOGIN_ERROR".equals(message.getType()) || "USERNAME_ERROR".equals(message.getType())) {
						String errorMessage = message.toString();
						Platform.runLater(() -> {
							controller.showLoginMessage(errorMessage, false);
						});
						disconnect();
						break;
					}
					if ("CHAT".equals(message.getType())) {
						String chatText = message.toString();
						Platform.runLater(() -> {
							gameController.receiveMessage(chatText);
						});
					}
					if ("ERROR".equals(message.getType()) || "SERVER".equals(message.getType()) || "OPPONENT_PLAYER".equals(message.getType()) || "GAME_OVER".equals(message.getType()) || "LEADERBOARD_DATA".equals(message.getType()) || "LEADERBOARD_ERROR:".equals(message.getType())) {
						Platform.runLater(() -> {
							handleServerMessage(message);
						});
					}
					if ("PLAYER".equals(message.getType())) {
						Platform.runLater(() -> {
							synchronized (pendingMessages) {
								pendingMessages.add(() -> handleServerMessage(message));
							}
						});
					}
				}
//				// Handle server messages
//				else if (obj instanceof String message) {
//					System.out.println(message);
//					if (message.startsWith("ERROR:") || message.startsWith("SERVER:") || message.startsWith("OPPONENT_PLAYER:") || message.startsWith("GAME_OVER:") || message.startsWith("LEADERBOARD_DATA:") || message.startsWith("LEADERBOARD_ERROR:")) {
//						Platform.runLater(() -> {
//							handleServerMessage(message);
//						});
//					} else if (message.startsWith("PLAYER:")) {
//						Platform.runLater(() -> {
//							synchronized (pendingMessages) {
//								pendingMessages.add(() -> handleServerMessage(message));
//							}
//						});
//					}
//				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}

	private void handleGameState(GameState gameState) {
		try {
			int[][] board = gameState.getBoard();
			String status = gameState.getStatus();
			int currentPlayer = gameState.getCurrentPlayer();

			gameController.updateBoard(board);

			if (!gameEnded) {
				if (status.equals("WIN")) {
					if (currentPlayer == 1) {
						if (playerID == 1) {
							winner = player1Username;
						} else if (playerID == 2) {
							winner = opponentPlayerUsername;
						}
					} else if (currentPlayer == 2) {
						if (playerID == 1) {
							winner = opponentPlayerUsername;
						} else if (playerID == 2) {
							winner = player2Username;
						}
					}
					gameController.showEndMessage(winner + " wins!");
					gameEnded = true;
				} else if (status.equals("DRAW")) {
					gameController.showEndMessage("It's a draw!");
					gameEnded = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleServerMessage(Message message) {
		try {
			if ("ERROR".equals(message.getType())) {
				gameController.showMessage(message.toString(), true);
			}
			if ("SERVER".equals(message.getType())) {
				gameController.showMessage(message.toString(), false);
			}
			if ("OPPONENT_PLAYER".equals(message.getType())) {
				String[] parts = message.toString().split(" - ", 2);
				String id = parts[0];
				opponentPlayerID = Integer.parseInt(id.trim());
				opponentPlayerUsername = parts[1];
			}
			if ("PLAYER".equals(message.getType())) {
				String[] parts = message.toString().split(" - ", 2);
				String id = parts[0];
				playerID = Integer.parseInt(id.trim());
				if (playerID == 1) {
					player1Username = parts[1];
					gameController.showMessage("You are Player " + playerID + " - " + this.getUsername() + "\n" + player1Username + " goes first. Wait for your opponent to join.", false);
				} else if (playerID == 2) {
					player2Username = parts[1];
					gameController.showMessage("You are Player " + playerID + " - " + this.getUsername() + "\n" + opponentPlayerUsername + " goes first", false);
				}
			}
			if ("LEADERBOARD_DATA".equals(message.getType())) {
				String leaderboardData = message.toString();
				if (leaderboardController != null) {
					leaderboardController.updateLeaderboard(leaderboardData);
				}
			}
			if ("LEADERBOARD_ERROR".equals(message.getType())) {
				Platform.runLater(() -> controller.showLoginMessage(message.toString(), false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Disconnect a client
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

	// Send chat message
	public void sendUserMessage(String data) {
		try {
			Message chatMessage = new Message("CHAT", data);
			out.writeObject(chatMessage);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send a move
	public void sendMove(int col) {
		try {
			Message moveMessage = new Message("MAKE_MOVE", String.valueOf(col));
			out.writeObject(moveMessage);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Rematch will be sent to server
	public void sendRematchRequest() {
		try {
			Message rematchMessage = new Message("REMATCH", "Requesting rematch");
			out.writeObject(rematchMessage);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Reset game state for a new game
	public void resetGame() {
		gameEnded = false;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
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