import Message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	ArrayList<GameThread> games = new ArrayList<>();
	TheServer server;


	Server() {
		server = new TheServer();
		server.start();
	}

	public class TheServer extends Thread{

		public void run() {

			try(ServerSocket mysocket = new ServerSocket(5555);){
				System.out.println("Server is waiting for a client!");

				while(true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					// Protect the games list and make sure there is no concurrent modification
					synchronized (games) {
						// Pair clients
						GameThread joinableGame = null;

						for (GameThread g : games) {
							if (g.player2 == null && g.isGameValid()) {
								joinableGame = g;
								break;
							}
						}

						// Found a game
						if (joinableGame != null) {
							joinableGame.setPlayer2(c);
							c.setGame(joinableGame, 2);
							c.initialize();
							System.out.println("Paired client #" + count + " as Player 2");
							if (joinableGame.player1 != null) {
								joinableGame.player1.sendToSelf("SERVER: Player 2 has joined: " + c.getDisplayName());
							}
						}
						// Create new game
						else {
							GameThread newGame = new GameThread(c);
							games.add(newGame);
							c.setGame(newGame, 1);
							c.initialize();
							System.out.println("No one waiting, created new game for client #" + count + " as Player 1");
						}
					}
					clients.add(c);
					c.start();
					count++;
				}
			} catch(Exception e) {
				System.err.println("Server did not launch");
			}
		}
	}

	public class ClientThread extends Thread{
		Socket connection;
		public int count;
		ObjectInputStream in;
		public ObjectOutputStream out;
		GameThread gameThread;
		int playerID;
		String username;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
			this.username = null;
		}

		// Initialize client by reading username from client and send it to client
		void initialize() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);

				Object data = in.readObject();
				if (data instanceof String message && message.startsWith("USERNAME:")) {
					username = message.substring(9);
//					System.out.println("Client #" + count + " set username to: " + username);
				}
				sendToSelf("PLAYER_ID: " + playerID + " - " + getDisplayName()); // Send player ID to client
			} catch (Exception e) {
				System.err.println("Error initializing client #" + count + ": " + e.getMessage());
			}
		}

		// Set game for this client and assign player ID
		void setGame(GameThread game, int playerID) {
			this.gameThread = game;
			this.playerID = playerID;
		}

		// Get username for this client
		private String getDisplayName() {
			if (username != null && !username.equals("")) {
				return username;
			}
			return "Client #" + count;
		}

		// Broadcast message to all clients
		public void updateClients(Object message) {
			for (ClientThread t : clients) {
				try {
					if (t.out != null) {
						t.out.writeObject(message);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// Send message to self
		public void sendToSelf(Object message) {
			try {
				if (out != null) {
					out.writeObject(message);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void sendToOpponent(String message) {
			ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
			if (opponent != null) {
				try {
					opponent.sendToSelf(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void run() {
			// Notice that new client connected
			updateClients("new client on server: client #" + count);

			while (true) {
				try {
					Object data = in.readObject();
					System.out.println("Client #" + count + " sent: " + data);

					if (data instanceof Message message) {
						// Handle move if the message is a move
						if ("MAKE_MOVE".equals(message.getType())) {
							handleMove(message);
						}
						if ("CHAT".equals(message.getType())) {
							sendChatToOpponent(message);
//							System.out.println(message.toString());
						}
					}
				} catch (Exception e) {
					System.err.println("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					updateClients("Client #" + count + " has left the server!");
					clients.remove(this);
					// Clean up game if any player dc
					// Protect the games list and make sure there is no concurrent modification
					synchronized (games) {
						if (gameThread != null) {
							ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
							if (opponent != null) {
								try {
									opponent.sendToSelf("SERVER: " + this.getDisplayName() + " disconnected \nBack to start a new game");
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
							games.remove(gameThread);
							System.out.println("Removed game because client #" + count + " (Game " + gameThread.getGameId() + " Player "+ playerID + ") disconnected");
						}
					}
					break;
				}
			}
		}
		private void sendChatToOpponent(Message message) {
			ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
			if (opponent != null) {
				try {
					opponent.sendToSelf(message);  // Send the chat message to the opponent
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		private void handleMove(Message message) {
			try {
				int col = Integer.parseInt(message.toString());

				// Protect the gameThread and make sure there is only one move at a time
				synchronized (gameThread.gameLock) {
					// Wait for opponent
					if (gameThread.player2 == null) {
						sendToSelf("ERROR: Waiting for opponent");
						return;
					}

					// Check if it is your turn
					if (gameThread.game.getCurrentPlayer() != playerID) {
						sendToSelf("ERROR: Not your turn");
						return;
					}

					if (gameThread.game.getCurrentPlayer() == playerID) {
						sendToOpponent("SERVER: It is your turn");
					}

					// Make move
					boolean valid = gameThread.game.makeMove(col);
					if (!valid) {
						sendToSelf("ERROR: Invalid move");
						return;
					}

					gameThread.sendGameState();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}