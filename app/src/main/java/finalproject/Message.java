package finalproject;

import java.sql.Timestamp;

public class Message {
    private int messageID;
    private int channelID;
    private Integer conversationID;
    private int senderID;
    private String username;
    private String messageText;
    private byte[] imageData;
    private Timestamp createdAt;

    public Message() {}

    public Message(int messageID, int channelID, Integer conversationID, int senderID, String username, String messageText, byte[] imageData, Timestamp createdAt) {
        this.messageID = messageID;
        this.channelID = channelID;
        this.conversationID = conversationID;
        this.senderID = senderID;
        this.username = username;
        this.messageText = messageText;
        this.imageData = imageData;
        this.createdAt = createdAt;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public Integer getConversationID() {
        return conversationID;
    }

    public void setConversationID(Integer conversationID) {
        this.conversationID = conversationID;
    }

    public int getSenderID() {
        return senderID;
    }

    public void setSenderID(int senderID) {
        this.senderID = senderID;
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

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}