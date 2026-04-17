package finalproject.messages;

import finalproject.Users.UserLogin;
import finalproject.servers.ServerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Named("messageService")
@SessionScoped
public class MessageService implements Serializable {
    @Inject private UserLogin login;
    @Inject private ServerService serverService;
    private Connection conn;
    private List<Message> messages = new ArrayList<>();
    private String messageText;
    private int channelID;
    private Integer conversationID;
    private String sendMessageStatus;
    private String deleteMessageStatus;
    private String editMessageStatus;
    private String loadMessageStatus;

    //  DB
    @PostConstruct
    public void openConnection() {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            loadMessageStatus = e.getMessage();
        }
    }

    @PreDestroy
    public void closeConnection() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }

    //HELPERS
    private boolean isBlocked(int userA, int userB) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM blocks WHERE (userID = ? AND blockedID = ?) OR (userID = ? AND blockedID = ?)")) {
            stmt.setInt(1, userA);
            stmt.setInt(2, userB);
            stmt.setInt(3, userB);
            stmt.setInt(4, userA);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    private int getOtherUserId(int conversationID) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT userOneID, userTwoID FROM direct_conversations WHERE conversationID = ?")) {
            stmt.setInt(1, conversationID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int u1 = rs.getInt("userOneID");
                int u2 = rs.getInt("userTwoID");
                return (u1 == login.getUserId()) ? u2 : u1;
            }
        } catch (SQLException ignored) {}
        return -1;
    }
    // SEND
    public void sendMessage() {
        sendMessageStatus = "";
        if (conn == null || login == null) {
            sendMessageStatus = "Not connected.";
            return;
        }
        if ((messageText == null || messageText.trim().isEmpty())) {
            sendMessageStatus = "Message cannot be empty.";
            return;
        }
        if (conversationID != null) {
            int otherUser = getOtherUserId(conversationID);
            if (isBlocked(login.getUserId(), otherUser)) {
                sendMessageStatus = "User unavailable.";
                return;
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (channelID, conversationID, senderID, message_text, sentOn) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setObject(1, channelID == 0 ? null : channelID);
            stmt.setObject(2, conversationID);
            stmt.setInt(3, login.getUserId());
            stmt.setString(4, messageText);
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
            sendMessageStatus = "Message sent.";
        } catch (SQLException e) {
            sendMessageStatus = e.getMessage();
        }
    }
    //  LOAD dm
    public void loadDM(int conversationID) {
        messages.clear();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.*, u.username FROM messages m " +
                "LEFT JOIN users u ON m.senderID = u.userID " +
                "WHERE m.conversationID = ? ORDER BY m.sentOn ASC")) {
            stmt.setInt(1, conversationID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setMessageID(rs.getInt("messageID"));
                msg.setSenderID(rs.getInt("senderID"));
                msg.setUsername(rs.getString("username"));
                if (rs.getTimestamp("deleted_at") != null) {
                    if (rs.getInt("senderID") == login.getUserId()) {
                        msg.setMessageText("You deleted a message");
                    } else {
                        msg.setMessageText(rs.getString("username") + " deleted a message");
                    }
                } else {
                    msg.setMessageText(rs.getString("message_text"));
                }
                msg.setSentOn(rs.getTimestamp("sentOn"));
                msg.setDeletedAt(rs.getTimestamp("deleted_at"));
                if (msg.getSenderID() != login.getUserId()) {
                    markAsSeen(msg.getMessageID());
                }
                messages.add(msg);
            }
        } catch (SQLException e) {
            loadMessageStatus = e.getMessage();
        }
    }

    public void loadChannel(int channelID) {
        messages.clear();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.*, u.username FROM messages m " +
                "JOIN users u ON m.senderID = u.userID " +
                "WHERE m.channelID = ? " +
                "AND m.senderID NOT IN (SELECT blockedID FROM blocks WHERE userID = ?) " +
                "AND m.senderID NOT IN (SELECT userID FROM blocks WHERE blockedID = ?) " +
                "ORDER BY m.sentOn ASC")) {
            stmt.setInt(1, channelID);
            stmt.setInt(2, login.getUserId());
            stmt.setInt(3, login.getUserId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setMessageID(rs.getInt("messageID"));
                msg.setSenderID(rs.getInt("senderID"));
                msg.setUsername(rs.getString("username"));
                if (rs.getTimestamp("deleted_at") != null) {
                    if (rs.getInt("senderID") == login.getUserId()) {
                        msg.setMessageText("You deleted a message");
                    } else {
                        msg.setMessageText(rs.getString("username") + " deleted a message");
                    }
                } else {
                    msg.setMessageText(rs.getString("message_text"));
                }
                msg.setSentOn(rs.getTimestamp("sentOn"));
                if (msg.getSenderID() != login.getUserId()) {
                    markAsSeen(msg.getMessageID());
                }
                messages.add(msg);
            }
        } catch (SQLException e) {
            loadMessageStatus = e.getMessage();
        }
    }
    // DELETE
    public void deleteMessage(int messageID, int senderID, int serverID) {
        deleteMessageStatus = "";
        int currentUser = login.getUserId();
        boolean isSender = (currentUser == senderID);
        boolean hasPermission = serverService.hasPermission(serverID, "can_delete_messages");
        if (!isSender && !hasPermission) {
            deleteMessageStatus = "No permission.";
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE messages SET deleted_at = ? WHERE messageID = ?")) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, messageID);
            stmt.executeUpdate();
            deleteMessageStatus = "Deleted.";
        } catch (SQLException e) {
            deleteMessageStatus = e.getMessage();
        }
    }
    // EDIT 
    public void editMessage(int messageID) {
        editMessageStatus = "";
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE messages SET message_text = ?, edited_at = ? WHERE messageID = ? AND senderID = ?")) {
            stmt.setString(1, messageText);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, messageID);
            stmt.setInt(4, login.getUserId());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                editMessageStatus = "Updated.";
            } else {
                editMessageStatus = "No permission.";
            }
        } catch (SQLException e) {
            editMessageStatus = e.getMessage();
        }
    }
    // SEEN
    public void markAsSeen(int messageID) {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT IGNORE INTO message_reads (messageID, userID) VALUES (?, ?)")) {
            stmt.setInt(1, messageID);
            stmt.setInt(2, login.getUserId());
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
    public boolean isSeenByOtherUser(int messageID) {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT 1 FROM message_reads r " +
            "WHERE r.messageID = ? AND r.userID != ?")) {
            stmt.setInt(1, messageID);
            stmt.setInt(2, login.getUserId());
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }
    public List<String> getSeenByUsers(int messageID) {
        List<String> users = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT u.username FROM message_reads r " +
            "JOIN users u ON r.userID = u.userID " +
            "WHERE r.messageID = ? AND r.userID != ?")) {
            stmt.setInt(1, messageID);
            stmt.setInt(2, login.getUserId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException ignored) {}
        return users;
    }
    //  GETTERS 
    public List<Message> getMessages() { 
        return messages; 
    }
    public String getMessageText() { 
        return messageText; 
    }
    public void setMessageText(String messageText) { 
        this.messageText = messageText; 
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
    public String getSendMessageStatus() { 
        return sendMessageStatus; 
    }
    public String getDeleteMessageStatus() { 
        return deleteMessageStatus; 
    }
    public String getEditMessageStatus() { 
        return editMessageStatus; 
    }
    public String getLoadMessageStatus() { 
        return loadMessageStatus; 
    }
}