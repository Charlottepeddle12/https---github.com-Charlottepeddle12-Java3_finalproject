package finalproject.messages;

import java.sql.Timestamp;

public class Message {
    private int messageID;
    private Integer channelID;
    private Integer conversationID;
    private int senderID;
    private String username;
    private String messageText;
    private byte[] imageData;
    private String imageMimeType;
    private Timestamp sentOn;
    private Timestamp editedAt;
    private Timestamp deletedAt;

    // Getters & Setters
    public int getMessageID() { 
        return messageID; 
    }
    public void setMessageID(int messageID) { 
        this.messageID = messageID; 
    }
    public Integer getChannelID() { 
        return channelID; 
    }
    public void setChannelID(Integer channelID) { 
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
    public String getImageMimeType() { 
        return imageMimeType; 
    }
    public void setImageMimeType(String imageMimeType) { 
        this.imageMimeType = imageMimeType; 
    }
    public Timestamp getSentOn() { 
        return sentOn; 
    }
    public void setSentOn(Timestamp sentOn) { 
        this.sentOn = sentOn; 
    }
    public Timestamp getEditedAt() {
         return editedAt; 
        }
    public void setEditedAt(Timestamp editedAt) { 
        this.editedAt = editedAt; 
    }
    public Timestamp getDeletedAt() { 
        return deletedAt; 
    }
    public void setDeletedAt(Timestamp deletedAt) { 
        this.deletedAt = deletedAt; 
    }
}