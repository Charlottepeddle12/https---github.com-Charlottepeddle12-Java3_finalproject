package finalproject.messages;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("dmBean")
@SessionScoped
public class DMBean implements Serializable {

    private String receiverUserName;
    private String messageText;
    private String message;

    public String sendDM() {
        if (receiverUserName == null || receiverUserName.trim().isEmpty()) {
            message = "Recipient username is required.";
            return null;
        }

        if (messageText == null || messageText.trim().isEmpty()) {
            message = "Message cannot be empty.";
            return null;
        }

        message = "DM sent to " + receiverUserName + ".";

        receiverUserName = "";
        messageText = "";

        return null;
    }

    public String getReceiverUserName() {
        return receiverUserName;
    }

    public void setReceiverUserName(String receiverUserName) {
        this.receiverUserName = receiverUserName;
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
}