package message_database;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private long timestamp;
    private String sender;
    private String text;

    public Message(String sender, String text, long timestamp) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.text = text;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return this.text;
    }

    public void setContent(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "[" + sender + "] - " + text;
    }
}
