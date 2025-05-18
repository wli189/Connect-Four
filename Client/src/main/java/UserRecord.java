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

    public SimpleIntegerProperty rankProperty() {
        return rank;
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public SimpleIntegerProperty winsProperty() {
        return wins;
    }

    public SimpleIntegerProperty lossesProperty() {
        return losses;
    }
}
