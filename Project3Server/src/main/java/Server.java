import Message.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	ArrayList<GameThread> games = new ArrayList<>();
	ArrayList<String> usernames = new ArrayList<>();
	TheServer server;
	static final String RECORDS_FILE = "../user_records.json";
	private static Map<String, UserRecord> userRecords = new HashMap<>(); // Store records in memory
	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	Server() {
		loadUserRecords();
		server = new TheServer();
		server.start();
	}

	private void loadUserRecords() {
		try (FileReader reader = new FileReader(RECORDS_FILE)) {
			ArrayList<UserRecord> records = gson.fromJson(reader, new TypeToken<ArrayList<UserRecord>>(){}.getType());  // Read JSON file to java arraylist
			if (records != null) {
				for (UserRecord record : records) {
					userRecords.put(record.getUsername(), record);  // Add each record to the map
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("No existing user records found. Creating new user_records.json.");
			try (FileWriter writer = new FileWriter(RECORDS_FILE)) {
				// Write an empty array to the file
				gson.toJson(new ArrayList<UserRecord>(), writer);
			} catch (IOException ex) {
				System.err.println("Error creating user_records.json: " + ex.getMessage());
			}
		} catch (IOException e) {
			System.err.println("Error loading user records: " + e.getMessage());
		}
	}

	// Save user records to JSON file
	private static void saveUserRecords() {
		try (FileWriter writer = new FileWriter(RECORDS_FILE)) {
			ArrayList<UserRecord> records = new ArrayList<>(userRecords.values());
			gson.toJson(records, writer);
		} catch (IOException e) {
			System.err.println("Error saving user records: " + e.getMessage());
		}
	}

	// create a user record if it doesn't exist or get the existing one if it does exist
	private static UserRecord createNewUserRecord(String username) {
		return userRecords.computeIfAbsent(username, k -> new UserRecord(username, "", 0, 0));
	}

	// Update win/loss record
	public static void updateUserRecord(String username, boolean isWin) {
		UserRecord record = createNewUserRecord(username);
		if (isWin) {
			record.incrementWins();
		} else {
			record.incrementLosses();
		}
		saveUserRecords(); // Save after every update
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
								joinableGame.player1.sendToSelf("SERVER: Player 2 has joined: " + c.getDisplayName()); // Tell player 1 that player 2 joined
								joinableGame.player1.sendToSelf("OPPONENT_PLAYER: 1 - "+ c.getDisplayName());  // Tell player 1 the player 2's username
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
					String requestedUsername = message.substring(9);
					// checks if username is taken
					synchronized (usernames) {
						if (usernames.contains(requestedUsername)) {
							sendToSelf("ERROR: Username already taken. Please choose another.");
							connection.close();
							return;
						} else {
							usernames.add(requestedUsername);
							this.username = requestedUsername;
						}
					}
//					System.out.println("Client #" + count + " set username to: " + username);
				}
				if (playerID == 2) {
					sendToSelf("OPPONENT_PLAYER: 2 - "+ gameThread.player1.getDisplayName()); // Tell player 2 the player 1's username
				}
				sendToSelf("PLAYER: " + playerID + " - " + getDisplayName()); // Send player ID to client

				createNewUserRecord(getDisplayName());
				saveUserRecords();
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