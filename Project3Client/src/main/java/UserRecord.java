import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class UserRecord {
    private final SimpleIntegerProperty rank;
    private final SimpleStringProperty username;
    private final SimpleIntegerProperty wins;
    private final SimpleIntegerProperty losses;

    public UserRecord(int rank, String username, int wins, int losses) {
        this.rank = new SimpleIntegerProperty(rank);
        this.username = new SimpleStringProperty(username);
        this.wins = new SimpleIntegerProperty(wins);
        this.losses = new SimpleIntegerProperty(losses);
    }

    public SimpleIntegerProperty rank() {
        return rank;
    }

    public SimpleStringProperty username() {
        return username;
    }

    public SimpleIntegerProperty wins() {
        return wins;
    }

    public SimpleIntegerProperty losses() {
        return losses;
    }
}
