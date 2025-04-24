import Message.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	ArrayList<GameThread> games = new ArrayList<>();
	ArrayList<String> usernames = new ArrayList<>();
	TheServer server;
	static final String DB_URL = "jdbc:sqlite:../user_records.db";
	private static Connection dbConnection;

	Server() {
		initializeDatabase();
		server = new TheServer();
		server.start();
	}

	private void initializeDatabase() {
		try {
			// Load SQLite JDBC driver
			Class.forName("org.sqlite.JDBC");
			// Connect to database
			dbConnection = DriverManager.getConnection(DB_URL);
			Statement stmt = dbConnection.createStatement();
			// Create user_records table if it doesn't exist
			String createTableSQL = """
                CREATE TABLE IF NOT EXISTS user_records (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0
                )
                """;

			stmt.executeUpdate(createTableSQL);
		} catch (ClassNotFoundException e) {
			System.err.println("SQLite JDBC driver not found: " + e.getMessage());
		} catch (SQLException e) {
			System.err.println("Error initializing database: " + e.getMessage());
		}
    }

	// Create a new user record in the database if it doesn't exist
	private static void createNewUserRecord(String username) {
		try {
			String createUserSQL = """
					INSERT INTO user_records (username, password, wins, losses) VALUES (?, ?, ?, ?)
					""";
			PreparedStatement pstmt = dbConnection.prepareStatement(createUserSQL);
			pstmt.setString(1, username);
			pstmt.setString(2, "");
			pstmt.setInt(3, 0);
			pstmt.setInt(4, 0);
			pstmt.executeUpdate();


		} catch (SQLException e) {
			System.err.println("Error creating new user record: " + e.getMessage());
		}
	}

	public static void updateUserRecord(String username, boolean isWin) throws SQLException {
		// Ensure user exists
		createNewUserRecord(username);
		String updateSQL = "";
		// Update wins or losses
		if (isWin) {
			updateSQL = """
						UPDATE user_records SET wins = wins + 1 WHERE username = ?
						""";
		} else if (!isWin) {
			updateSQL = """
						UPDATE user_records SET losses = losses + 1 WHERE username = ?
						""";
		}
		PreparedStatement pstmt = dbConnection.prepareStatement(updateSQL);
		pstmt.setString(1, username);
		pstmt.executeUpdate();
	}

	public class TheServer extends Thread{

		public void run() {

			try(ServerSocket mysocket = new ServerSocket(5555);){
				System.out.println("Server is waiting for a client!");

				while(true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					// Protect the games list and make sure there is no concurrent modification

					// Initialize client to validate username before pairing
					if (!c.preInitialize()) {
						// Username validation failed; client connection is already closed
						count++;
						continue;
					}
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
							c.postInitialize();
							System.out.println("Paired client #" + count + " as Player 2");
							if (joinableGame.player1 != null) {
								joinableGame.player1.sendToSelf("SERVER: Player 2 has joined: " + c.getDisplayName()); // Tell player 1 that player 2 joined
								joinableGame.player1.sendToSelf("OPPONENT_PLAYER: 1 - "+ c.getDisplayName());  // Tell player 1 the player 2's username
							}
						}
						// Create new game
						else {
							GameThread newGame = new GameThread(c);
							games.add(newGame);
							c.setGame(newGame, 1);
							c.postInitialize();
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
		boolean wantsRematch = false;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
			this.username = null;
		}

		// Rematch
		private void handleRematchRequest() {
			wantsRematch = true;
			ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
			opponent.sendToSelf( "SERVER: Opponent wants rematch.");

			if (opponent != null && opponent.wantsRematch) {
				// Both players want a rematch, create a new game
//				System.out.println("Both players want rematch. Starting a new game.");

				GameThread newGame = new GameThread(this, opponent);
				synchronized (games) {
					games.add(newGame);
				}

				// Reset flags
				wantsRematch = false;
				opponent.wantsRematch = false;

				// Set new game
				this.setGame(newGame, 1);
				opponent.setGame(newGame, 2);

				this.postInitialize();
				opponent.postInitialize();

				this.sendToSelf("SERVER: Starting rematch as Player 1");
				opponent.sendToSelf("SERVER: Starting rematch as Player 2");
			} else {
				// Waiting for opponent's rematch decision
				sendToSelf("SERVER: Waiting for opponent to accept rematch...");
			}
		}

		// Initialize client by reading username from client and send it to client
		boolean preInitialize() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);

				Object data = in.readObject();
				if (data instanceof String message && message.startsWith("LOGIN:")) {
					String loginInfo = message.substring(6);
					String[] loginInfoArr = loginInfo.split(":");
					String requestedUsername = loginInfoArr[0];
					String requestedPassword = loginInfoArr[1];
					// checks if username is taken
					synchronized (usernames) {
						if (usernames.contains(requestedUsername)) {
							sendToSelf("USERNAME_ERROR: User already login in. Please try again later or choose another username.");
							return false;
						} else {
							usernames.add(requestedUsername);
							this.username = requestedUsername;
							sendToSelf("LOGIN_SUCCESS: " + requestedUsername);
						}
					}
					return true;
//					System.out.println("Client #" + count + " set username to: " + username);
				}
			} catch (Exception e) {
				System.err.println("Error initializing client #" + count + ": " + e.getMessage());
			}
			return false;
		}

		void postInitialize() {
			try {
				if (playerID == 2) {
					sendToSelf("OPPONENT_PLAYER: 2 - " + gameThread.player1.getDisplayName()); // Tell player 2 the player 1's username
				}
				sendToSelf("PLAYER: " + playerID + " - " + getDisplayName()); // Send player ID to client

				createNewUserRecord(getDisplayName());
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
        public String getDisplayName() {
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
//					System.out.println("Client #" + count + " sent: " + data);

					if (data instanceof Message message) {
						// Handle move if the message is a move
						if ("MAKE_MOVE".equals(message.getType())) {
							handleMove(message);
						}
						if ("CHAT".equals(message.getType())) {
							sendChatToOpponent(message);
//							System.out.println(message.toString());
						}
						// if rematch is requested it will make new game
						if ("REMATCH".equals(message.getType())) {
							handleRematchRequest();
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
									opponent.sendToSelf("SERVER: " + this.getDisplayName() + " disconnected \nGame ended! Back to start a new game");
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
							games.remove(gameThread);
							System.out.println("Removed game because client #" + count + " (Game " + gameThread.getGameId() + " Player "+ playerID + ") disconnected");
						}
					}
					// removes names from the usernames list to allow for other to use the name
					synchronized (usernames) {
						if (username != null) {
							usernames.remove(username);
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