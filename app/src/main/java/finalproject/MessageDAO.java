package finalproject;


//import main.model.Message;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    private DataSource dataSource;

    public MessageDAO() {
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            dataSource = (DataSource) envContext.lookup("jdbc/chatdb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendMessage(int channelID, int senderID, String messageText) {
        String sql = "INSERT INTO messages (channelID, conversationID, senderID, message_text) VALUES (?, NULL, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelID);
            stmt.setInt(2, senderID);
            stmt.setString(3, messageText);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Message> getMessagesByChannel(int channelID) {
        List<Message> messages = new ArrayList<>();

        String sql = """
            SELECT m.messageID, m.channelID, m.conversationID, m.senderID,
                   m.message_text, m.image_data, m.created_at,
                   u.username
            FROM messages m
            LEFT JOIN users u ON m.senderID = u.userID
            WHERE m.channelID = ?
            ORDER BY m.created_at ASC
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.setMessageID(rs.getInt("messageID"));
                    message.setChannelID((Integer) rs.getObject("channelID"));
                    message.setConversationID((Integer) rs.getObject("conversationID"));
                    message.setSenderID((Integer) rs.getObject("senderID"));
                    message.setUsername(rs.getString("username"));
                    message.setMessageText(rs.getString("message_text"));
                    message.setImageData(rs.getBytes("image_data"));
                    message.setCreatedAt(rs.getTimestamp("created_at"));
                    messages.add(message);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public boolean deleteMessage(int messageID, int senderID) {
        String sql = "DELETE FROM messages WHERE messageID = ? AND senderID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageID);
            stmt.setInt(2, senderID);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}