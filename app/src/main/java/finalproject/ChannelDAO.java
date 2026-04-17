// package finalproject;

// //import main.model.Channel;

// import javax.naming.Context;
// import javax.naming.InitialContext;
// import javax.sql.DataSource;
// import java.sql.*;
// import java.util.ArrayList;
// import java.util.List;

// public class ChannelDAO {

//     private DataSource dataSource;

//     public ChannelDAO() {
//         try {
//             Context initContext = new InitialContext();
//             Context envContext = (Context) initContext.lookup("java:/comp/env");

//             dataSource = (DataSource) envContext.lookup("jdbc/javaproject");
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     public boolean isValidChannelName(String name) {
//         return name != null
//                 && !name.trim().isEmpty()
//                 && name.trim().length() <= 100;
//     }

//     public boolean channelNameExistsInServer(int serverID, String name) {
//         String sql = "SELECT COUNT(*) FROM channels WHERE serverID = ? AND LOWER(name) = LOWER(?)";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, serverID);
//             stmt.setString(2, name.trim());

//             try (ResultSet rs = stmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt(1) > 0;
//                 }
//             }
//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return false;
//     }

//     //memebership and permission checks
//     public boolean isServerMember(int userID, int serverID) {
//         String sql = "SELECT COUNT(*) FROM server_members WHERE userID = ? AND serverID = ?";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, userID);
//             stmt.setInt(2, serverID);

//             try (ResultSet rs = stmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt(1) > 0;
//                 }
//             }
//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return false;
//     }

//     public boolean isServerOwner(int userID, int serverID) {
//         String sql = "SELECT COUNT(*) FROM servers WHERE serverID = ? AND ownerID = ?";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, serverID);
//             stmt.setInt(2, userID);

//             try (ResultSet rs = stmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt(1) > 0;
//                 }
//             }
//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return false;
//     }

// public boolean hasCreateChannelPermission(int userID, int serverID) {
//         if (isServerOwner(userID, serverID)) {
//             return true;
//         }

//         String sql = """
//             SELECT COUNT(*)
//             FROM server_member_roles smr
//             JOIN server_roles sr ON smr.roleID = sr.roleID
//             WHERE smr.userID = ?
//               AND smr.serverID = ?
//               AND sr.serverID = ?
//               AND sr.canCreateChannels = TRUE
//         """;

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, userID);
//             stmt.setInt(2, serverID);
//             stmt.setInt(3, serverID);

//             try (ResultSet rs = stmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt(1) > 0;
//                 }
//             }

//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return false;
//     }

//     public boolean createChannel(int currentUserID, int serverID, String name) {
//         if (!isValidChannelName(name)) {
//             return false;
//         }

//         if (!isServerMember(currentUserID, serverID)) {
//             return false;
//         }

//         if (!hasCreateChannelPermission(currentUserID, serverID)) {
//             return false;
//         }

//         if (channelNameExistsInServer(serverID, name)) {
//             return false;
//         }
    
//         String sql = "INSERT INTO channels (serverID, name, created_by) VALUES (?, ?, ?)";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, serverID);
//             stmt.setString(2, name.trim());
//             stmt.setInt(3, currentUserID);

//             return stmt.executeUpdate() > 0;

//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return false;
//     }
    
//     public List<Channel> getChannelsByServer(int currentUserID, int serverID) {
//         List<Channel> channels = new ArrayList<>();

//         if (!isServerMember(currentUserID, serverID)) {
//             return channels;
//         }

//         String sql = "SELECT channelID, serverID, name, created_by, created_at FROM channels WHERE serverID = ? ORDER BY name ASC";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, serverID);

//             try (ResultSet rs = stmt.executeQuery()) {
//                 while (rs.next()) {
//                     Channel channel = new Channel();
//                     channel.setChannelID(rs.getInt("channelID"));
//                     channel.setServerID(rs.getInt("serverID"));
//                     channel.setName(rs.getString("name"));
//                     channel.setCreatedBy((Integer) rs.getObject("created_by"));
//                     channel.setCreatedAt(rs.getTimestamp("created_at"));
//                     channels.add(channel);
//                 }
//             }
//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return channels;
//     }

//     public Channel getChannelById(int currentUserID, int channelID) {
//         String sql = "SELECT channelID, serverID, name, created_by, created_at FROM channels WHERE channelID = ? AND userID = ?";

//         try (Connection conn = dataSource.getConnection();
//              PreparedStatement stmt = conn.prepareStatement(sql)) {

//             stmt.setInt(1, channelID);
//             stmt.setInt(2, currentUserID);

//             try (ResultSet rs = stmt.executeQuery()) {
//                 if (rs.next()) {
//                     Channel channel = new Channel();
//                     channel.setChannelID(rs.getInt("channelID"));
//                     channel.setServerID(rs.getInt("serverID"));
//                     channel.setName(rs.getString("name"));
//                     channel.setCreatedBy((Integer) rs.getObject("created_by"));
//                     channel.setCreatedAt(rs.getTimestamp("created_at"));
//                     return channel;
//                 }
//             }

//         } catch (SQLException e) {
//             e.printStackTrace();
//         }

//         return null;
//     }
// }