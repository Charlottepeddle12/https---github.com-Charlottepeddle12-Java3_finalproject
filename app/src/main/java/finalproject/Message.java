package finalproject;

import java.sql.Timestamp;

public class Message {
    private int messageId;
    private int channelId;
    private int userId;
    private String username;
    private String messageText;
    private Timestamp createdAt;

    public Message() {}

    public Message(int messageId, int channelId, int userId, String username, String messageText, Timestamp createdAt) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.userId = userId;
        this.username = username;
        this.messageText = messageText;
        this.createdAt = createdAt;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}