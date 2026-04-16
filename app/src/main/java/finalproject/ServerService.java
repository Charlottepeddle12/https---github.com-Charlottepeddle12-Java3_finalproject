package finalproject;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Named("serverBean")
@SessionScoped
public class ServerService implements Serializable {

    @Inject
    private UserLogin login;

    private Connection conn;

    private int serverID;
    private String serverName;
    private boolean publicServer;
    private String message = "";
    private String targetUserName;
    private int inviteID;
    private List<Server> servers = new ArrayList<>();
    private List<Server> publicServers = new ArrayList<>();

    // DB
    @PostConstruct
    public void openConnection() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            message = e.getMessage();
        }
    }

    @PreDestroy
    public void closeConnection() {
        try {
            if (conn != null)
                conn.close();
        } catch (Exception ignored) {
        }
    }

    // Helper method to add a user as a member to a server
    private void addMembership(int serverId, int userId) {
        String sql = "INSERT INTO server_members (userID, serverID) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Membership error: " + e.getMessage());
        }
    }

    // Server Creation
    public void createServer() {
        System.out.println("[ServerService] createServer called");
        if (serverName == null || serverName.trim().isEmpty()) {
            message = "Server name cannot be empty.";
            System.out.println("[ServerService] Server name is empty");
            return;
        }
        System.out.println("[ServerService] Attempting to create server: " + serverName + ", ownerId="
                + login.getUserId() + ", publicServer=" + publicServer);
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO servers (name, ownerId, is_public) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, serverName);
            stmt.setInt(2, login.getUserId());
            stmt.setBoolean(3, publicServer);
            int affectedRows = stmt.executeUpdate();
            System.out.println("[ServerService] Inserted server, affectedRows=" + affectedRows);
            if (affectedRows == 0) {
                message = "Creating server failed, no rows affected.";
                System.out.println("[ServerService] No rows affected on insert");
                return;
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newServerID = generatedKeys.getInt(1);
                    System.out.println("[ServerService] New server ID: " + newServerID);
                    addMembership(newServerID, login.getUserId());
                    message = "Server created successfully.";
                } else {
                    message = "Creating server failed, no ID obtained.";
                    System.out.println("[ServerService] No ID obtained after insert");
                }
            }
        } catch (SQLException e) {
            message = e.getMessage();
            System.out.println("[ServerService] SQLException: " + e.getMessage());
        }
    }

    // Server Deletion
    public void deleteServer() {
        System.out.println("[ServerService] deleteServer called");
        if (serverName == null || serverName.trim().isEmpty()) {
            message = "Server name cannot be empty.";
            System.out.println("[ServerService] Server name is empty");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ownerID FROM servers WHERE serverID = ?;")) {
            stmt.setInt(1, serverID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getInt("ownerID") == login.getUserId()) {
                    System.out.println("[ServerService] Attempting to delete server: " + serverName + ", ownerId="
                            + login.getUserId() + ", publicServer=" + publicServer);
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM servers WHERE serverID = ?;")) {
                        stmt.setInt(1, serverID);
                        int affectedRows = deleteStmt.executeUpdate();
                        System.out.println("[ServerService] Deleted server, affectedRows=" + affectedRows);
                        if (affectedRows == 0) {
                            message = "Deleting server failed, no rows affected.";
                            System.out.println("[ServerService] No rows affected on delete");
                            return;
                        }
                    } catch (SQLException e) {
                        message = e.getMessage();
                        System.out.println("[ServerService] SQLException: " + e.getMessage());
                    }
                } else {
                    System.out.println("[ServerService] you to not have permission to delete " + serverName + ".");
                    return;
                }
            } else if (!rs.first()) {
                System.out.println("[ServerService]" + serverName + "does not exist.");
                return;
            }
        } catch (SQLException e) {
            message = e.getMessage();
            System.out.println("[ServerService] SQLException: " + e.getMessage());
        }
    }

    // Server Member Role Permission
    public void givePermission(String memberUsername, String serverName, String roleName) {
        if (conn == null || login == null) {
            message = "Not connected to database or user not logged in.";
        }
        System.out.println("[ServerService] givePermission called");
        if (serverName == null || serverName.trim().isEmpty()) {
            message = "Server name cannot be empty.";
            System.out.println("[ServerService] Server name is empty");
            return;
        }
        else if (roleName == null || roleName.trim().isEmpty()) {
            message = "Role name cannot be empty.";
            System.out.println("[ServerService] Role name is empty");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ownerID FROM servers WHERE name = ?;")) {
            stmt.setString(1, serverName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int serverID = rs.getInt("serverID");
                if (rs.getInt("ownerID") == login.getUserId()) {
                    try (PreparedStatement getUserStmt = conn.prepareStatement(
                            "SELECT userID FROM users WHERE username = ?;");) {
                        getUserStmt.setString(1, memberUsername);
                        ResultSet userRS = getUserStmt.executeQuery();
                        if (userRS.next()) {
                            int memberuserID = userRS.getInt("userID");
                            try (
                                    PreparedStatement isUserInDBStmt = conn.prepareStatement(
                                            "SELECT * FROM server_members WHERE userID = ? AND serverID = ?;")) {
                                isUserInDBStmt.setInt(1, memberuserID);
                                isUserInDBStmt.setInt(2, serverID);
                                ResultSet userInDBRS = isUserInDBStmt.executeQuery();
                                if (userInDBRS.next()) {
                                    try (
                                            PreparedStatement getRoleStmt = conn.prepareStatement(
                                                    "SELECT roleID FROM server_roles WHERE role_name = ? AND serverID = ?;")) {
                                        getRoleStmt.setString(1, roleName);
                                        getRoleStmt.setInt(2, serverID);
                                        ResultSet roleRS = getRoleStmt.executeQuery();
                                        if (roleRS.next()) {
                                            int roleID = roleRS.getInt("roleID");
                                            try (
                                                    PreparedStatement setuserRoleStmt = conn.prepareStatement(
                                                            "INSERT INTO server_member_roles (userID, serverID, roleID) VALUES (?, ?, ?);")) {
                                                setuserRoleStmt.setInt(1, memberuserID);
                                                setuserRoleStmt.setInt(2, serverID);
                                                setuserRoleStmt.setInt(3, roleID);
                                                setuserRoleStmt.execute();
                                                message = "[ServerService] " + memberUsername
                                                        + " has been given the role of " + roleName + "!";
                                            } catch (Exception e) {
                                                message = "[ServerService] SQLException: " + e.getMessage();
                                            }
                                        } else {
                                            message = "[ServerService] " + roleName + " does not exist.";
                                            return;
                                        }
                                    } catch (SQLException e) {
                                        message = "[ServerService] SQLException: " + e.getMessage();
                                    }
                                } else {
                                    message = "[ServerService] " + memberUsername + " is not in " + serverName + ".";
                                    return;
                                }
                            } catch (SQLException e) {
                                message = "[ServerService] SQLException: " + e.getMessage();
                            }
                        } else {
                            message = "[ServerService] No users with " + memberUsername + " exists.";
                            return;
                        }
                    } catch (SQLException e) {
                        message = "[ServerService] SQLException: " + e.getMessage();
                    }

                } else {
                    message = "[ServerService] you to not have permission to grant roles in " + serverName + ".";
                    return;
                }
            } else {
                message ="[ServerService]" + serverName + "does not exist.";
                return;
            }
        } catch (SQLException e) {
            message = "[ServerService] SQLException: " + e.getMessage();
        }

    }

    // Load User Servers
    public void loadUserServers() {
        servers.clear(); // important: reset list

        if (conn == null || login == null) {
            message = "Not connected to database or user not logged in.";
            return;
        }

        String sql = "SELECT s.serverID, s.name, s.ownerID, s.is_public " +
                "FROM servers s " +
                "JOIN server_members sm ON s.serverID = sm.serverID " +
                "WHERE sm.userID = ? " +
                "ORDER BY s.created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, login.getUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Server server = new Server();
                    server.setServerID(rs.getInt("serverID"));
                    server.setName(rs.getString("name"));
                    server.setOwnerID(rs.getInt("ownerID"));
                    server.setPublic(rs.getBoolean("is_public"));
                    servers.add(server); // ← store in field
                }
            }

        } catch (SQLException e) {
            message = "Failed to load user servers: " + e.getMessage();
        }
    }

    // Load Public Servers
    public void loadPublicServers() {
        publicServers.clear(); // important: reset list

        if (conn == null || login == null) {
            message = "Not connected to database or user not logged in.";
            return;
        }

        String sql = "SELECT s.serverID, s.name, s.ownerID, s.is_public " +
                "FROM servers s " +
                "WHERE s.is_public = TRUE " +
                "AND s.serverID NOT IN (SELECT sm.serverID FROM server_members sm WHERE sm.userID = ?) " +
                "ORDER BY s.created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, login.getUserId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Server server = new Server();
                    server.setServerID(rs.getInt("serverID"));
                    server.setName(rs.getString("name"));
                    server.setOwnerID(rs.getInt("ownerID"));
                    server.setPublic(rs.getBoolean("is_public"));
                    publicServers.add(server); // ← store in field
                }
            }
        } catch (SQLException e) {
            message = "Failed to load public servers: " + e.getMessage();
        }
    }

    // Join Public Server
    public void joinPublicServer(int serverId) {
        if (conn == null || login == null) {
            message = "Not connected to database or user not logged in.";
            return;
        }
        String sql = "INSERT INTO server_members (userID, serverID) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            message = "Joined server successfully.";
        } catch (SQLException e) {
            message = "Failed to join server: " + e.getMessage();
        }
    }

    // Getters and Setters
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<Server> getServers() {
        return servers;
    }

    public boolean isPublicServer() {
        return publicServer;
    }

    public void setPublicServer(boolean publicServer) {
        this.publicServer = publicServer;
    }

    public List<Server> getPublicServers() {
        return publicServers;
    }

    public int getUserId() {
        return login != null ? login.getUserId() : -1;
    }
}