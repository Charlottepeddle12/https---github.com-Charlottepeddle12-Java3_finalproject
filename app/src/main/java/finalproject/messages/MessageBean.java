package finalproject.messages;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("messageBean")
@SessionScoped
public class MessageBean implements Serializable {

    private Integer channelID = 1;
    private String messageText;
    private String message;
    private List<Message> messages = new ArrayList<>();

    public void loadMessages() {
        messages = new ArrayList<>();
    }

    public String sendMessage() {
        Message msg = new Message();
        msg.setUsername("User");
        msg.setMessageText(messageText);

        messages.add(msg);
        messageText = "";
        message = "Message sent.";

        return null;
    }

    public Integer getChannelID() {
        return channelID;
    }

    public void setChannelID(Integer channelID) {
        this.channelID = channelID;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessage() {
        return message;
    }

    public List<Message> getMessages() {
        return messages;
    }
}