package Game;

import java.util.Arrays;

public class GameMechanics {
    private final int rows = 6;
    private final int cols = 7;
    private int[][] board;
    private int currentPlayer;  // 1 for player 1, 2 for player 2

    public GameMechanics() {
        board = new int[rows][cols]; // Initialize a new board for this game
        currentPlayer = 1; // Player 1 starts
    }

    // Getters
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int[][] getBoard() {
        return deepCopy(board);
    }

    // Make a move
    public boolean makeMove(int col) {
        // Check if the col is out of range or the col is full
        if (col < 0 || col >= cols || isColumnFull(col)) return false;

        for (int row = rows - 1; row >= 0; row--) {
            if (board[row][col] == 0) {
                board[row][col] = currentPlayer;
                return true;
            }
        }
        return false;
    }

    public boolean checkWin() {
        return checkHorizontal() || checkVertical() || checkDiagonal();
    }

    // Helper functions
    // Check if a column is full
    public boolean isColumnFull(int col) {
        return board[0][col] != 0;
    }

    // Check if there is a winning combination in the column
    public boolean checkHorizontal() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col <= cols - 4; col++) {
                int player = board[row][col];
                if (player != 0 &&
                        board[row][col + 1] == player &&
                        board[row][col + 2] == player &&
                        board[row][col + 3] == player)
                    return true;
            }
        }
        return false;
    }

    public boolean checkVertical() {
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row <= rows - 4; row++) {
                int player = board[row][col];
                if (player != 0 &&
                        board[row + 1][col] == player &&
                        board[row + 2][col] == player &&
                        board[row + 3][col] == player)
                    return true;
            }
        }
        return false;
    }

    public boolean checkDiagonal() {
        // \ direction
        for (int row = 0; row <= rows - 4; row++) {
            for (int col = 0; col <= cols - 4; col++) {
                int player = board[row][col];
                if (player != 0 &&
                        board[row + 1][col + 1] == player &&
                        board[row + 2][col + 2] == player &&
                        board[row + 3][col + 3] == player)
                    return true;
            }
        }
        // / direction
        for (int row = 3; row < rows; row++) {
            for (int col = 0; col <= cols - 4; col++) {
                int player = board[row][col];
                if (player != 0 &&
                        board[row - 1][col + 1] == player &&
                        board[row - 2][col + 2] == player &&
                        board[row - 3][col + 3] == player)
                    return true;
            }
        }
        return false;
    }

    public boolean isDraw() {
        for (int col = 0; col < cols; col++) {
            if (!isColumnFull(col)) return false;
        }
        return !checkWin();
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    // Deep copy board
    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = Arrays.copyOf(original[i], original[i].length);;
        }
        return copy;
    }
}
