public class GameMechanics {
    private final int rows = 6;
    private final int cols = 7;
    private final int[][] board = new int[rows][cols];

    private int currentPlayer = 1;  // 1 for player 1, 2 for player 2

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int[][] getBoard() {
        return board;
    }

    public boolean isColumnFull(int col) {
        return board[0][col] != 0;
    }

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
}
