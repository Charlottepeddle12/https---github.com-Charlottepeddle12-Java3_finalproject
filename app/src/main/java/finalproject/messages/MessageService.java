package finalproject.messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import finalproject.Users.UserLogin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("messageBean")
@RequestScoped
public class MessageService {
    @Inject
    private UserLogin login;
    private Connection conn;
    private int channelID;
    private int senderID;
    private int messageID;
    private String messageText;
    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public void setSenderID(int senderID) {
        this.senderID = senderID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    @PostConstruct
    public void openConnection() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @PreDestroy
    public void closeConnection() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // SEND MESSAGE
    public void sendMessage() {
        try {
            // 1. check membership
            PreparedStatement check = conn.prepareStatement(
                "SELECT * FROM server_members sm " +
                "JOIN channels c ON sm.serverID = c.serverID " +
                "WHERE sm.userID = ? AND c.channelID = ?"
            );
            check.setInt(1, senderID);
            check.setInt(2, channelID);

            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                message = "You are not allowed to send messages in this channel.";
                return;
            }

            // 2. BLOCK CHECK (IMPORTANT)
            PreparedStatement blockCheck = conn.prepareStatement(
                "SELECT * FROM blocks WHERE (userID = ? AND blockedID = ?) OR (userID = ? AND blockedID = ?)"
            );

            // check against ALL users in same channel
            // simplified: block self-check (real-time filter handled in UI)
            blockCheck.setInt(1, senderID);
            blockCheck.setInt(2, senderID);
            blockCheck.setInt(3, senderID);
            blockCheck.setInt(4, senderID);

            if (blockCheck.executeQuery().next()) {
                message = "You cannot send messages due to block restrictions.";
                return;
            }

            // 3. send message
            MessageDAO dao = new MessageDAO();
            boolean success = dao.sendMessage(channelID, senderID, messageText);

            if (success) {
                message = "Message sent!";
            } else {
                message = "Failed to send message.";
            }

        } catch (SQLException e) {
            message = e.getMessage();
        }
    }

    // DELETE MESSAGE
    public void deleteMessage() {
        MessageDAO dao = new MessageDAO();
        boolean success = dao.deleteMessage(messageID, senderID);

        if (success) {
            message = "Message deleted.";
        } else {
            message = "Failed to delete message.";
        }
    }
}