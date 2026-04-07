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

    public boolean sendMessage(int channelId, int userId, String messageText) {
        String sql = "INSERT INTO messages (channel_id, user_id, message_text) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, userId);
            stmt.setString(3, messageText);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Message> getMessagesByChannel(int channelId) {
        List<Message> messages = new ArrayList<>();

        String sql = """
            SELECT m.message_id, m.channel_id, m.user_id, m.message_text, m.created_at,
                   u.username
            FROM messages m
            JOIN users u ON m.user_id = u.user_id
            WHERE m.channel_id = ?
            ORDER BY m.created_at ASC
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.setMessageId(rs.getInt("message_id"));
                    message.setChannelId(rs.getInt("channel_id"));
                    message.setUserId(rs.getInt("user_id"));
                    message.setUsername(rs.getString("username"));
                    message.setMessageText(rs.getString("message_text"));
                    message.setCreatedAt(rs.getTimestamp("created_at"));
                    messages.add(message);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public boolean deleteMessage(int messageId, int userId) {
        String sql = "DELETE FROM messages WHERE message_id = ? AND user_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}