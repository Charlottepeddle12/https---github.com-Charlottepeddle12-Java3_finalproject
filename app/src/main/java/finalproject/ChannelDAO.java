package finalproject;

//import main.model.Channel;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelDAO {

    private DataSource dataSource;

    public ChannelDAO() {
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            dataSource = (DataSource) envContext.lookup("jdbc/chatdb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean createChannel(int serverId, String channelName, int createdBy) {
        String sql = "INSERT INTO channels (server_id, channel_name, created_by) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serverId);
            stmt.setString(2, channelName);
            stmt.setInt(3, createdBy);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Channel> getChannelsByServer(int serverId) {
        List<Channel> channels = new ArrayList<>();
        String sql = "SELECT * FROM channels WHERE server_id = ? ORDER BY channel_name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serverId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Channel channel = new Channel();
                    channel.setChannelId(rs.getInt("channel_id"));
                    channel.setServerId(rs.getInt("server_id"));
                    channel.setChannelName(rs.getString("channel_name"));
                    channel.setCreatedBy(rs.getInt("created_by"));
                    channel.setCreatedAt(rs.getTimestamp("created_at"));
                    channels.add(channel);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return channels;
    }

    public Channel getChannelById(int channelId) {
        String sql = "SELECT * FROM channels WHERE channel_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Channel channel = new Channel();
                    channel.setChannelId(rs.getInt("channel_id"));
                    channel.setServerId(rs.getInt("server_id"));
                    channel.setChannelName(rs.getString("channel_name"));
                    channel.setCreatedBy(rs.getInt("created_by"));
                    channel.setCreatedAt(rs.getTimestamp("created_at"));
                    return channel;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}