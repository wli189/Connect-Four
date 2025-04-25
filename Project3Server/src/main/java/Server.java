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
	private static void createNewUserRecord(String username, String password) {
		try {
			String createUserSQL = """
					INSERT INTO user_records (username, password, wins, losses) VALUES (?, ?, ?, ?)
					""";
			PreparedStatement pstmt = dbConnection.prepareStatement(createUserSQL);
			pstmt.setString(1, username);
			pstmt.setString(2, password);
			pstmt.setInt(3, 0);
			pstmt.setInt(4, 0);
			pstmt.executeUpdate();


		} catch (SQLException e) {
			System.err.println("Error creating new user record: " + e.getMessage());
		}
	}

	public static void updateUserRecord(String username, boolean isWin) throws SQLException {
		// Ensure user exists
		createNewUserRecord(username, "");
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
								Message msg = new Message("SERVER", "Player 2 has joined: " + c.getDisplayName());  // Tell player 1 that player 2 joined
								joinableGame.player1.sendToSelf(msg);
								Message opponetMsg = new Message("OPPONENT_PLAYER", "1 - " + c.getDisplayName());  // Tell player 1 the player 2's username
								joinableGame.player1.sendToSelf(opponetMsg);
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
		private String password;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
			this.username = null;
		}

		// Rematch
		private void handleRematchRequest() {
			wantsRematch = true;
			ClientThread opponent = (playerID == 1) ? gameThread.player2 : gameThread.player1;
			Message msg = new Message("SERVER", "Opponent wants rematch");
			opponent.sendToSelf(msg);

			if (opponent != null && opponent.wantsRematch) {
				// Both players want a rematch, create a new game

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

				Message rematchMsg = new Message("SERVER", "Starting rematch as Player 1");
				this.sendToSelf(rematchMsg);
				Message opponetMsg = new Message("SERVER", "Starting rematch as Player 2");
				opponent.sendToSelf(opponetMsg);
			} else {
				// Waiting for opponent's rematch decision
				Message waitMsg = new Message("SERVER", "Waiting for opponent to accept rematch...");
				sendToSelf(waitMsg);
			}
		}

		boolean verifyCredentials(String username, String password, String dbPassword) {
			if (dbPassword.equals(password) && dbPassword != null) {
				this.password = password;
				return true;
			} else {
				return false;
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
				if (data instanceof Message message) {
					if ("LOGIN".equals(message.getType())) {
						String loginInfo = message.toString();
						String[] loginInfoArr = loginInfo.split(":");
						String requestedUsername = loginInfoArr[0];
						String requestedPassword = loginInfoArr[1];
						synchronized (usernames) {
							String matchSQL = "SELECT password FROM user_records WHERE username = ?";
							PreparedStatement pstmt = dbConnection.prepareStatement(matchSQL);
							pstmt.setString(1, requestedUsername);
							ResultSet rs = pstmt.executeQuery();

							if (rs.next()) {
								// Username exists, verify password
								String dbPassword = rs.getString("password");
								if (verifyCredentials(requestedUsername, requestedPassword, dbPassword)) {
									// Check if the user has logged in
									if (usernames.contains(requestedUsername)) {
										Message msg = new Message("USERNAME_ERROR", "User already login in. Please try again later.");
										sendToSelf(msg);
										return false;
									}
									usernames.add(requestedUsername);
									this.username = requestedUsername;
									Message msg = new Message("LOGIN_SUCCESS", requestedUsername);
									sendToSelf(msg);
									return true;
								} else {
									Message msg = new Message("LOGIN_ERROR", "Invalid username or password.");
									sendToSelf(msg);
									return false;
								}
							} else {
								// Username doesn't exist, create new account
								createNewUserRecord(requestedUsername, requestedPassword);
								usernames.add(requestedUsername);
								this.username = requestedUsername;
								Message msg = new Message("LOGIN_SUCCESS", requestedUsername);
								sendToSelf(msg);
								return true;
							}
						}
					}
					if ("LEADERBOARD_REQUEST".equals(message.getType())) {
						handleLeaderboardRequest();
					}
				}
			} catch (Exception e) {
				System.err.println("Error preinitializing client #" + count + ": " + e.getMessage());
			}
			return false;
		}

		void postInitialize() {
			try {
				if (playerID == 2) {
					Message msg = new Message("OPPONENT_PLAYER", "2 - " + gameThread.player1.getDisplayName());
					sendToSelf(msg);  // Tell player 2 the player 1's username
				}
				Message msg = new Message("PLAYER", playerID + " - " + getDisplayName());
				sendToSelf(msg);  // Send player ID to client
			} catch (Exception e) {
				System.err.println("Error postinitializing client #" + count + ": " + e.getMessage());
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

		public String getPassword() {
			return password;
		}

		// Broadcast message to all clients
		public void updateClients(Object message) {
			for (ClientThread t : clients) {
				try {
					if (t.out != null) {
						t.out.writeObject(message);
						t.out.flush();
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
					out.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void sendToOpponent(Object message) {
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

					if (data instanceof Message message) {
						// Handle move if the message is a move
						if ("MAKE_MOVE".equals(message.getType())) {
							handleMove(message);
						}

						// Receive chat message and send it to the opponent
						if ("CHAT".equals(message.getType())) {
							sendChatToOpponent(message);
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
									Message msg = new Message("SERVER", this.getDisplayName() + " disconnected \nGame ended! Back to start a new game");
									opponent.sendToSelf(msg);
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

		// Get leaderboard data
		private void handleLeaderboardRequest() {
            String leaderboardData = fetchLeaderboardData();
			Message msg = new Message("LEADERBOARD_DATA", leaderboardData);
			sendToSelf(msg);
		}

		private String fetchLeaderboardData() {
			StringBuilder data = new StringBuilder();
			int rank = 1;
			try {
				String leaderboardSQL = "SELECT username, wins, losses FROM user_records ORDER BY wins DESC, losses ASC, username ASC";
				PreparedStatement pstmt = dbConnection.prepareStatement(leaderboardSQL);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					String username = rs.getString("username");
					int wins = rs.getInt("wins");
					int losses = rs.getInt("losses");
					data.append(rank).append(",").append(username).append(",").append(wins).append(",").append(losses).append(";");
					rank++;
				}
			} catch (SQLException e) {
                System.err.println("Error fetching leaderboard: " + e.getMessage());
            }
            return data.toString();
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
						Message waitMsg = new Message("ERROR", "Waiting for opponent...");
						sendToSelf(waitMsg);
						return;
					}

					// Check if the game has already ended
					if (!gameThread.isGameOngoing()) {
						Message msg = new Message("ERROR", "Game has ended");
						sendToSelf(msg);
						return;
					}

					// Check if it is your turn
					if (gameThread.game.getCurrentPlayer() != playerID) {
						Message msg = new Message("ERROR", "Not your turn");
						sendToSelf(msg);
						return;
					}

					if (gameThread.game.getCurrentPlayer() == playerID) {
						Message msg = new Message("SERVER", "It is your turn");
						sendToOpponent(msg);
					}

					// Make move
					boolean valid = gameThread.game.makeMove(col);
					if (!valid) {
						Message msg = new Message("ERROR", "Invalid move");
						sendToSelf(msg);
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