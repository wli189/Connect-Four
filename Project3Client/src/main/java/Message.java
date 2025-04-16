import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    String message, type;

    public Message(String input){
        message = input;
    }

    public Message(String type, String input) {
        this.type = type;
        this.message = input;
    }

    public String toString(){
        return message;
    }

    public String getType() {
        return type;
    }

}
