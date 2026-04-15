package finalproject.messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("dmBean")
@RequestScoped
public class DirectMessageService {

    private Connection conn;
    private int senderID;
    private int receiverID;
    private String messageText;
    private String message;

    @PostConstruct
    public void init() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            message = e.getMessage();
        }
    }

    public void sendDM() {
        try {
            // BLOCK CHECK
            PreparedStatement blockCheck = conn.prepareStatement(
                "SELECT * FROM blocks WHERE (userID = ? AND blockedID = ?) OR (userID = ? AND blockedID = ?)"
            );
            blockCheck.setInt(1, senderID);
            blockCheck.setInt(2, receiverID);
            blockCheck.setInt(3, receiverID);
            blockCheck.setInt(4, senderID);

            if (blockCheck.executeQuery().next()) {
                message = "Cannot send message. User is blocked.";
                return;
            }

            DirectConversationDAO dao = new DirectConversationDAO();
            int convoID = dao.getOrCreateConversation(senderID, receiverID);

            MessageDAO messageDAO = new MessageDAO();
            boolean success = messageDAO.sendDirectMessage(convoID, senderID, messageText);

            message = success ? "DM sent!" : "Failed to send DM.";

        } catch (Exception e) {
            message = e.getMessage();
        }
    }

    public void setSenderID(int senderID) {
        this.senderID = senderID;
    }

    public void setReceiverID(int receiverID) {
        this.receiverID = receiverID;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessage() {
        return message;
    }
}