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

    public boolean createChannel(int serverID, String name) {
        String sql = "INSERT INTO channels (serverID, name) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serverID);
            stmt.setString(2, name);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Channel> getChannelsByServer(int serverID) {
        List<Channel> channels = new ArrayList<>();
        String sql = "SELECT channelID, serverID, name, created_at FROM channels WHERE serverID = ? ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serverID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Channel channel = new Channel();
                    channel.setChannelID(rs.getInt("channelID"));
                    channel.setServerID(rs.getInt("serverID"));
                    channel.setName(rs.getString("name"));
                    channel.setCreatedAt(rs.getTimestamp("created_at"));
                    channels.add(channel);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return channels;
    }

    public Channel getChannelById(int channelID) {
        String sql = "SELECT channelID, serverID, name, created_at FROM channels WHERE channelID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Channel channel = new Channel();
                    channel.setChannelID(rs.getInt("channelID"));
                    channel.setServerID(rs.getInt("serverID"));
                    channel.setName(rs.getString("name"));
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