public class UserRecord {
    private String username;
    private int wins;
    private int losses;
    private String password;

    public UserRecord(String username, String password, int wins, int losses) {
        this.username = username;
        this.password = password;
        this.wins = wins;
        this.losses = losses;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void incrementWins() {
        this.wins++;
    }

    public void incrementLosses() {
        this.losses++;
    }
}
