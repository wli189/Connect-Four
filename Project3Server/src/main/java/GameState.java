import java.io.Serializable;
import java.util.Arrays;

public class GameState implements Serializable {
    private final int[][] board;
    private final int currentPlayer;
    private final String status; // "ONGOING", "WIN", "DRAW"

    public GameState(int[][] board, int currentPlayer, String status) {
        this.board = deepCopy(board);
        this.currentPlayer = currentPlayer;
        this.status = status;
    }

    public int[][] getBoard() {
        return deepCopy(board);
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public String getStatus() {
        return status;
    }

    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], original[i].length);;
        }
        return copy;
    }
}