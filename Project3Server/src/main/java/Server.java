import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	TheServer server;
	GameMechanics game = new GameMechanics();
	Object gameLock = new Object();

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
					c.start();
					count++;
				}
			} catch(Exception e) {
				System.err.println("Server did not launch");
			}
		}
	}

	class ClientThread extends Thread{
		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
		}

		public void updateClients(String message) {
			for (ClientThread t : clients) {
				try {
					t.out.writeObject(message);
					t.out.flush();
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

			updateClients("new client on server: client #" + count);

			while (true) {
				try {
					Object data = in.readObject();
					System.out.println("Client #" + count + " sent: " + data);

					if (data instanceof Message message) {
						if ("MAKE_MOVE".equals(message.getType())) {
							handleMove(message);
						}
					}
				} catch (Exception e) {
					System.err.println("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					updateClients("Client #"+count+" has left the server!");
					clients.remove(this);
					break;
				}
			}
		}

		private void handleMove(Message message) {
			try {
				int col = Integer.parseInt(message.toString());

				synchronized (gameLock) {
					if (game.getCurrentPlayer() != count) {
						out.writeObject("ERROR:Not your turn");
						out.flush();
						return;
					}
					boolean valid = game.makeMove(col);
					if (!valid) {
						out.writeObject("ERROR:Invalid move");
						out.flush();
						return;
					}

					// check win status
					boolean win = game.checkWin();
					boolean draw = game.isDraw();
					int[][] board = game.getBoard();

					String status;
					if (win) {
						status = "WIN";
 					} else if (draw) {
						status = "DRAW";
					} else {
						game.switchPlayer();
						status = "ONGOING";
					}

					GameState gameState = new GameState(board, game.getCurrentPlayer(), status);
					for (ClientThread ct : clients) {
						try {
							System.out.println("Sending GameState to client #" + ct.count);
							ct.out.writeObject(gameState);
							ct.out.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					System.out.println("GameState sent to all clients");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}