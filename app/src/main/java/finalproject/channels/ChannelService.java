package finalproject.channels;

import finalproject.Users.UserLogin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import javax.naming.*;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

@Named("channelBean")
@SessionScoped
public class ChannelService implements Serializable {

    @Inject
    private UserLogin login;
    private Connection conn;
    private String message = "";
    private int serverID;
    private String channelName;
    private String createMessage;
    private String serverName;
    private List<Channel> channels = new ArrayList<>();
    private String loadChannelsMessage;
    private String deleteChannelMessage;

    @PostConstruct
    public void open() {
        try {
            Context ctx = new InitialContext();
            conn = ((DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject")).getConnection();
        } catch (Exception e) { message = e.getMessage(); }
    }

    @PreDestroy
    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
    //create channel
    public void createChannel() {
        createMessage = "";
        deleteChannelMessage = "";
        loadChannelsMessage = "";
        if (conn == null || login == null) {
            createMessage = "Not connected or user not logged in.";
            return;
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            createMessage = "No server selected.";
            return;
        }
        if (channelName == null || channelName.trim().isEmpty()) {
            createMessage = "Channel name cannot be empty.";
            return;
        }
        int resolvedServerID = -1;
        try {
            String serverSql = "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)";
            int ownerID = -1;
            try (PreparedStatement stmt = conn.prepareStatement(serverSql)) {
                stmt.setString(1, serverName.trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    resolvedServerID = rs.getInt("serverID");
                    ownerID = rs.getInt("ownerID");
                } else {
                    createMessage = "Server not found.";
                    return;
                }
            }
            String memberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
                stmt.setInt(1, login.getUserId());
                stmt.setInt(2, resolvedServerID);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    createMessage = "You are not a member of this server.";
                    return;
                }
            }
            boolean canCreate = false;
            if (ownerID == login.getUserId()) {
                canCreate = true;
            } else {
                String permSql = "SELECT can_create_channel FROM server_member_permissions WHERE userID = ? AND serverID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(permSql)) {
                    stmt.setInt(1, login.getUserId());
                    stmt.setInt(2, resolvedServerID);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        canCreate = rs.getBoolean("can_create_channel");
                    }
                }
            }
            if (!canCreate) {
                createMessage = "You do not have permission to create channels.";
                return;
            }
            String checkSql = "SELECT 1 FROM channels WHERE serverID = ? AND LOWER(name) = LOWER(?)";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setInt(1, resolvedServerID);
                stmt.setString(2, channelName.trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    createMessage = "Channel name already exists in this server.";
                    return;
                }
            }
            String insertSql = "INSERT INTO channels (serverID, name, created_at) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, resolvedServerID);
                stmt.setString(2, channelName.trim());
                stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            }
            createMessage = "Channel created successfully.";
            channelName = null;
        } catch (SQLException e) {
            createMessage = "Error: " + e.getMessage();
        }
        loadChannels();
    }
    //load channels for server
    public void loadChannels() {
        loadChannelsMessage = "";
        channels.clear();
        if (conn == null || login == null) {
            loadChannelsMessage = "Not connected to database or user not logged in.";
            return;
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            loadChannelsMessage = "No server selected.";
            return;
        }
        try {
            int resolvedServerID = -1;
            String serverSql = "SELECT serverID FROM servers WHERE LOWER(name)=LOWER(?)";
            try (PreparedStatement stmt = conn.prepareStatement(serverSql)) {
                stmt.setString(1, serverName.trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    resolvedServerID = rs.getInt("serverID");
                } else {
                    loadChannelsMessage = "Server not found.";
                    return;
                }
            }
            String memberSql = "SELECT 1 FROM server_members WHERE userID=? AND serverID=?";
            try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
            stmt.setInt(1, login.getUserId());
                stmt.setInt(2, resolvedServerID);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    loadChannelsMessage = "You are not a member of this server.";
                    return;
                }
            }
            String sql = """
                SELECT channelID, serverID, name, created_at
                FROM channels
                WHERE serverID = ?
                ORDER BY created_at ASC
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, resolvedServerID);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Channel c = new Channel();
                    c.setChannelID(rs.getInt("channelID"));
                    c.setServerID(rs.getInt("serverID"));
                    c.setName(rs.getString("name"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    channels.add(c);
                }
            }
            if (channels.isEmpty()) {
                loadChannelsMessage = "No channels found for this server.";
            } else {
                loadChannelsMessage = "";
            }
        } catch (SQLException e) {
            loadChannelsMessage = "Failed to load channels: " + e.getMessage();
        }
    }
    //delete channel
    public void deleteChannel(int chId) {
        createMessage = "";
        deleteChannelMessage = "";
        loadChannelsMessage = "";
        if (conn == null || login == null) {
            deleteChannelMessage = "Not connected or user not logged in.";
            return;
        }
        try {
            int serverId = -1;
            int ownerId = -1;
            String channelSql = "SELECT serverID FROM channels WHERE channelID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(channelSql)) {
                stmt.setInt(1, chId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    serverId = rs.getInt("serverID");
                } else {
                    deleteChannelMessage = "Channel not found.";
                    return;
                }
            }
            String memberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
                stmt.setInt(1, login.getUserId());
                stmt.setInt(2, serverId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    deleteChannelMessage = "You are not a member of this server.";
                    return;
                }
            }
            String ownerSql = "SELECT ownerID FROM servers WHERE serverID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(ownerSql)) {
                stmt.setInt(1, serverId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    ownerId = rs.getInt("ownerID");
                }
            }
            if (ownerId != login.getUserId()) {
                deleteChannelMessage = "Only the server owner can delete channels.";
                return;
            }
            String deleteSql = "DELETE FROM channels WHERE channelID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, chId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    deleteChannelMessage = "Channel deleted successfully.";
                    loadChannels(); 
                } else {
                    deleteChannelMessage = "Channel deletion failed.";
                }
            }
        } catch (SQLException e) {
            deleteChannelMessage = "Error deleting channel: " + e.getMessage();
        }
    }
    //getters/setters
    public int getServerID() {
        return serverID;
    }
    public void setServerID(int serverID) {
        this.serverID = serverID;
    }
    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
    public String getCreateMessage() {
        return createMessage;
    }
    public String getServerName() {
        return serverName;
    }
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    public List<Channel> getChannels() {
        return channels;
    }
    public String getLoadChannelsMessage() {
        return loadChannelsMessage;
    }
    public String getDeleteChannelMessage() {
        return deleteChannelMessage;
    }
}