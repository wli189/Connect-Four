import Message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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
					clients.add(c);
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
							System.out.println("Paired client #" + count + " as Player 2");
						}
						// Create new game
						else {
							GameThread newGame = new GameThread(c);
							games.add(newGame);
							c.setGame(newGame, 1);
							System.out.println("No one waiting, created new game for client #" + count + " as Player 1");
						}
					}
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

		// Set game for this client and assign player ID
		void setGame(GameThread game, int playerID) {
			this.gameThread = game;
			this.playerID = playerID;
		}

		// Get username for this client
		private String getDisplayName() {
			return username != null ? username : "Client #" + count;
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
		public void sendToSelf(String message) {
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
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}

			try {
				Object data = in.readObject();
				if (data instanceof String message && message.startsWith("USERNAME:")) {
					username = message.substring(9); // Extract username after "USERNAME:"
					System.out.println("Client #" + count + " set username to: " + username);
				} else {
					System.err.println("Client #" + count + " did not send a valid username");
					username = "Player" + count; // Fallback username
				}
			} catch (Exception e) {
				System.err.println("Error receiving username from client #" + count + ": " + e.getMessage());
				username = "Player" + count; // Fallback username
			}

			// Notice that new client connected
			updateClients("new client on server: client #" + count);

			// Send player ID to client
			if (playerID == 1) {
				sendToSelf("PLAYER_ID: 1 - " + getDisplayName());
			}

			if (playerID == 2) {
				sendToSelf("PLAYER_ID: 2 - " + getDisplayName());
			}

			while (true) {
				try {
					Object data = in.readObject();
					System.out.println("Client #" + count + " sent: " + data);

					if (data instanceof Message message) {
						// Handle move if the message is a move
						if ("MAKE_MOVE".equals(message.getType())) {
							handleMove(message);
						}
					}
				} catch (Exception e) {
					System.err.println("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					updateClients("Client #" + count + " has left the server!");
					clients.remove(this);
					// Clean up game if any player dc
					synchronized (games) {
						if (gameThread != null) {
							ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
							if (opponent != null) {
								try {
									opponent.sendToSelf("ERROR: Opponent disconnected \nBack to start a new game");
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

		private void handleMove(Message message) {
			try {
				int col = Integer.parseInt(message.toString());

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