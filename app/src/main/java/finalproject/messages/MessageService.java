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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.Part;
import java.util.Base64;
import java.io.IOException;
@Named("messageService")
@SessionScoped
public class MessageService implements Serializable {
    @Inject private UserLogin login;
    @Inject private ServerService serverService;
    private Connection conn;
    private List<Message> messages = new ArrayList<>();
    private String messageText;
    private Integer  channelID;
    private Integer conversationID;
    private String sendMessageStatus;
    private String deleteMessageStatus;
    private String editMessageStatus;
    private String loadMessageStatus;
    private Part uploadedFile;
    private Integer editingMessageID;
    private String targetUsername;
    private String createConversationStatus;
    private List<Conversation> conversations = new ArrayList<>();
    private String editText;
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
        loadConversations(); 
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
        boolean hasText = messageText != null && !messageText.trim().isEmpty();
        boolean hasImage = uploadedFile != null && uploadedFile.getSize() > 0;
        if (hasImage) {
            String type = uploadedFile.getContentType();
            if (type == null || !type.startsWith("image/")) {
                sendMessageStatus = "Upload failed: only images are allowed.";
                return;
            }
        }
        if (!hasText && !hasImage) {
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
        "INSERT INTO messages (channelID, conversationID, senderID, message_text, image_data, image_mime_type, sentOn) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            if (conversationID != null) {
                stmt.setNull(1, java.sql.Types.INTEGER);   
                stmt.setInt(2, conversationID);
            } else if (channelID != null) {
                stmt.setInt(1, channelID);
                stmt.setNull(2, java.sql.Types.INTEGER);   
            }
            stmt.setInt(3, login.getUserId());
            stmt.setString(4, hasText ? messageText : null);
            if (hasImage) {
                stmt.setBytes(5, uploadedFile.getInputStream().readAllBytes());
                stmt.setString(6, uploadedFile.getContentType());
            } else {
                stmt.setNull(5, java.sql.Types.BLOB);
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));            stmt.executeUpdate();
            sendMessageStatus = "Message sent.";
        } catch (SQLException | IOException e) {
            sendMessageStatus = e.getMessage();
        }
        if (conversationID != null) {
            loadDM(conversationID);
        } else if (channelID != null) {
            loadChannel(channelID);
        }
        messageText = null;
        uploadedFile = null;
    }
    //  LOAD dm
    public void loadDM(int conversationID) {
        this.conversationID = conversationID;
        this.channelID = null;
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
                    msg.setImageData(rs.getBytes("image_data"));        
                    msg.setImageMimeType(rs.getString("image_mime_type"));
                }
                msg.setSentOn(rs.getTimestamp("sentOn"));
                msg.setEditedAt(rs.getTimestamp("edited_at"));
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
        this.channelID = channelID;
        this.conversationID = null;
        messages.clear();
        int currentUser = login.getUserId();
        boolean isOwner = false;
        int serverID = -1;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT serverID FROM channels WHERE channelID = ?")) {
            stmt.setInt(1, channelID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                serverID = rs.getInt("serverID");
            }
        } catch (SQLException ignored) {}
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ownerID FROM servers WHERE serverID = ?")) {
            stmt.setInt(1, serverID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                isOwner = (rs.getInt("ownerID") == currentUser);
            }
        } catch (SQLException ignored) {}
        boolean hasPermission = serverService.hasPermission(serverID, "can_delete_messages");
        boolean isAdminOrOwner = isOwner || hasPermission;
        try {
            PreparedStatement stmt;
            if (isAdminOrOwner) {
                stmt = conn.prepareStatement(
                    "SELECT m.*, u.username FROM messages m " +
                    "JOIN users u ON m.senderID = u.userID " +
                    "WHERE m.channelID = ? " +
                    "ORDER BY m.sentOn ASC");
                stmt.setInt(1, channelID);
            } else {
                stmt = conn.prepareStatement(
                    "SELECT m.*, u.username FROM messages m " +
                    "JOIN users u ON m.senderID = u.userID " +
                    "WHERE m.channelID = ? " +
                    "AND m.senderID NOT IN (SELECT blockedID FROM blocks WHERE userID = ?) " +
                    "AND m.senderID NOT IN (SELECT userID FROM blocks WHERE blockedID = ?) " +
                    "ORDER BY m.sentOn ASC");
                stmt.setInt(1, channelID);
                stmt.setInt(2, currentUser);
                stmt.setInt(3, currentUser);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setMessageID(rs.getInt("messageID"));
                msg.setSenderID(rs.getInt("senderID"));
                msg.setUsername(rs.getString("username"));
                Timestamp deletedAt = rs.getTimestamp("deleted_at");
                msg.setDeletedAt(deletedAt);
                if (deletedAt != null) {
                    if (rs.getInt("senderID") == currentUser) {
                        msg.setMessageText("You deleted a message");
                    } else {
                        msg.setMessageText(rs.getString("username") + " deleted a message");
                    }
                    msg.setImageData(null);
                    msg.setImageMimeType(null);
                } else {
                    msg.setMessageText(rs.getString("message_text"));
                    msg.setImageData(rs.getBytes("image_data"));
                    msg.setImageMimeType(rs.getString("image_mime_type"));
                }
                msg.setSentOn(rs.getTimestamp("sentOn"));
                msg.setEditedAt(rs.getTimestamp("edited_at"));
                if (msg.getSenderID() != currentUser) {
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
        boolean isOwner = false;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ownerID FROM servers WHERE serverID = ?")) {
            stmt.setInt(1, serverID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                isOwner = (rs.getInt("ownerID") == currentUser);
            }
        } catch (SQLException e) {
            deleteMessageStatus = e.getMessage();
            return;
        }
        boolean hasPermission = serverService.hasPermission(serverID, "can_delete_messages");
        if (!isSender && !hasPermission) {
            deleteMessageStatus = "No permission.";
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE messages SET deleted_at = ?, image_data = NULL, image_mime_type = NULL WHERE messageID = ?")) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, messageID);
            stmt.executeUpdate();
            deleteMessageStatus = "Deleted.";
        } catch (SQLException e) {
            deleteMessageStatus = e.getMessage();
        }
        if (conversationID != null) {
            loadDM(conversationID);
        } else if (channelID != null) {
            loadChannel(channelID);
        }
    }
    // EDIT 
    public void editMessage(int messageID) {
        editMessageStatus = "";
        if (editText == null || editText.trim().isEmpty()){
            editMessageStatus = "Message cannot be empty.";
            return;
        }
        if (isSeenByOtherUser(messageID)) {
            editMessageStatus = "Cannot edit. Message already seen.";
            return;
        }
        try (PreparedStatement checkStmt = conn.prepareStatement(
            "SELECT deleted_at FROM messages WHERE messageID = ?")) {
            checkStmt.setInt(1, messageID);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getTimestamp("deleted_at") != null) {
                editMessageStatus = "Cannot edit deleted message.";
                return;
            }
        } catch (SQLException e) {
            editMessageStatus = e.getMessage(); 
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE messages SET message_text = ?, edited_at = ? WHERE messageID = ? AND senderID = ?")) {
            stmt.setString(1, editText);
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
        editingMessageID = null;
        editText = null;
        if (conversationID != null) {
            loadDM(conversationID);
        } else if (channelID != null) {
            loadChannel(channelID);
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
    public void createConversation() {
        createConversationStatus = "";
        if (conn == null || login == null) {
            createConversationStatus = "Not connected.";
            return;
        }
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            createConversationStatus = "Enter username.";
            return;
        }
        try {
            PreparedStatement findUser = conn.prepareStatement(
                "SELECT userID FROM users WHERE username = ?");
            findUser.setString(1, targetUsername);
            ResultSet rs = findUser.executeQuery();
            if (!rs.next()) {
                createConversationStatus = "User not available.";
                return;
            }
            int otherUserId = rs.getInt("userID");
            if (otherUserId == login.getUserId()) {
                createConversationStatus = "Cannot DM yourself.";
                return;
            }
            if (isBlocked(login.getUserId(), otherUserId)) {
                createConversationStatus = "User not available.";
                return;
            }
            PreparedStatement check = conn.prepareStatement(
                "SELECT conversationID FROM direct_conversations " +
                "WHERE (userOneID = ? AND userTwoID = ?) " +
                "OR (userOneID = ? AND userTwoID = ?)");
            check.setInt(1, login.getUserId());
            check.setInt(2, otherUserId);
            check.setInt(3, otherUserId);
            check.setInt(4, login.getUserId());
            ResultSet existing = check.executeQuery();
            if (existing.next()) {
                createConversationStatus = "Conversation already exists.";
                return;
            }
            int currentUser = login.getUserId();

            // 🔥 FIX: sort IDs
            int userOneID = Math.min(currentUser, otherUserId);
            int userTwoID = Math.max(currentUser, otherUserId);

            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO direct_conversations (userOneID, userTwoID) VALUES (?, ?)");

            insert.setInt(1, userOneID);
            insert.setInt(2, userTwoID);
            insert.executeUpdate();
            createConversationStatus = "Conversation started.";
            targetUsername = null;
            loadConversations();
        } catch (SQLException e) {
            createConversationStatus = e.getMessage();
        }
    }
    // LOAD CONVERSATIONS
    public void loadConversations() {
        conversations.clear();
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT dc.conversationID, " +
                "CASE WHEN dc.userOneID = ? THEN u2.username ELSE u1.username END AS username " +
                "FROM direct_conversations dc " +
                "JOIN users u1 ON dc.userOneID = u1.userID " +
                "JOIN users u2 ON dc.userTwoID = u2.userID " +
                "WHERE dc.userOneID = ? OR dc.userTwoID = ?"
            );
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, login.getUserId());
            stmt.setInt(3, login.getUserId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Conversation c = new Conversation();
                c.setConversationID(rs.getInt("conversationID"));
                c.setUsername(rs.getString("username"));
                conversations.add(c);
            }
        } catch (SQLException e) {
            loadMessageStatus = e.getMessage();
        }
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
    public Integer  getChannelID() { 
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
    public Part getUploadedFile() {
        return uploadedFile;
    }
    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
    public String encodeImage(byte[] data) {
        if (data == null) return "";
        return Base64.getEncoder().encodeToString(data);
    }
    public String formatDateTime(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("dd MMM HH:mm").format(ts);
    }
    public String getSeenByUsersString(int messageID) {
        return String.join(", ", getSeenByUsers(messageID));
    }
    public Integer getEditingMessageID() {
        return editingMessageID;
    }
    public void setEditingMessageID(Integer editingMessageID) {
        this.editingMessageID = editingMessageID;
    }
    public void startEdit(int messageID, String currentText) {
        this.editingMessageID = messageID;
        this.editText = currentText; 
    }
    public String getTargetUsername() {
    return targetUsername;
    }
    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
    public List<Conversation> getConversations() {
        return conversations;
    }
    public String getCreateConversationStatus() {
        return createConversationStatus;
    }
    public String getEditText() {
        return editText;
    }
    public void setEditText(String editText) {
        this.editText = editText;
    }
    public boolean isCurrentDMBlocked() {
        if (conversationID == null) return false;
        int otherUser = getOtherUserId(conversationID);
        return isBlocked(login.getUserId(), otherUser);
    }
}