import Game.GameMechanics;
import Message.GameState;

import java.sql.SQLException;

public class GameThread {
    public Object gameLock = new Object();
    public GameMechanics game = new GameMechanics();
    public Server.ClientThread player1;
    public Server.ClientThread player2;
    private final int gameId;
    private static int gameCounter = 0;

    // Constructor
    public GameThread(Server.ClientThread player1) {
        this.player1 = player1;
        this.gameId = ++gameCounter;
    }
    public GameThread(Server.ClientThread player1, Server.ClientThread player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.gameId = ++gameCounter;
    }

    // Set player 2
    public void setPlayer2(Server.ClientThread player2) {
        this.player2 = player2;
    }

    // Get game ID
    public int getGameId() {
        return gameId;
    }

    // Check if both players are connected
    public boolean isGameValid() {
        boolean player1Connected = player1 != null && !player1.connection.isClosed();
        boolean player2Connected = player2 == null || (player2 != null && !player2.connection.isClosed());
        return player1Connected && player2Connected;
    }

    // Send game state to both players
    public void sendGameState() throws SQLException {
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

        Server.ClientThread winner = (game.getCurrentPlayer() == 1) ? player1 : player2;
        Server.ClientThread loser = (game.getCurrentPlayer() == 1) ? player2 : player1;

        if (win) {
            Server.updateUserRecord(winner.getDisplayName(), true);
            Server.updateUserRecord(loser.getDisplayName(), false);
        }

        GameState gameState = new GameState(board, game.getCurrentPlayer(), status);
        try {
            if (player1 != null && !player1.connection.isClosed()) {
                player1.out.writeObject(gameState);
                player1.out.flush();
                System.out.println("GameThread #" + gameId + " Sent GameState to client #" + player1.count);
            }
            if (player2 != null && !player2.connection.isClosed()) {
                player2.out.writeObject(gameState);
                player2.out.flush();
                System.out.println("GameThread #" + gameId + " Sent GameState to client #" + player2.count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}