
package finalproject.servers;

import finalproject.Users.UserLogin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.Context;
import javax.naming.InitialContext;
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
    private List<Invite> myInvites = new ArrayList<>();
    private int serverID;
    private String serverName;
    private boolean publicServer;
    private String message;
    private String createMessage;
    private String inviteMessage;
    private String kickMessage;
    private String transferMessage;
    private String leaveMessage;
    private String permissionMessage;
    private String deleteServerMessage;
    private String joinPublicServerMessage;
    private String loadInviteMessage;
    private String grantRemovePermisionMessage;
    private String targetUserName;
    private int inviteID;
    private List<Server> servers = new ArrayList<>();
    private List<Server> publicServers = new ArrayList<>();
    private String inviteServerName;
    private String inviteTargetUserName;
    private String kickServerName;
    private String kickTargetUserName;
    private String transferServerName;
    private String transferTargetUserName;
    private String leaveServerName;
    private String permissionName;
    private List<MemberPermissionView> permissionList = new ArrayList<>();
    private String permissionServerName;
    private String manageServerName; 
    private boolean publicLoaded = false;   
    
    //  DB
    @PostConstruct
    public void openConnection() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            message = e.getMessage();
        }
        message = null;
        createMessage = null;
        inviteMessage = null;
        kickMessage = null;
        transferMessage = null;
        leaveMessage = null;
        deleteServerMessage = null;
        joinPublicServerMessage = null;
    }

    @PreDestroy
    public void closeConnection() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
    // Helper method to add a user as a member to a server
    private void addMembership(int serverId, int userId, boolean isOwner) {
        String sql = "INSERT INTO server_members (userID, serverID) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);   
            stmt.setInt(2, serverId); 
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed - no rows added");
            }
            boolean isPublic = false;
            String checkSql = "SELECT is_public FROM servers WHERE serverID = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, serverId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    isPublic = rs.getBoolean("is_public");
                }
            }
            } catch (SQLException e) {
                throw new RuntimeException("Membership error: " + e.getMessage());
            }
    }
    //  Server Creation
    public void createServer() {
        createMessage  = "";
        if (conn == null || login == null) {
            createMessage  = "Not connected to database or user not logged in.";
            return;
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            createMessage  = "Server name cannot be empty.";
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO servers (name, ownerId, is_public) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, serverName.trim());
            stmt.setInt(2, login.getUserId());
            stmt.setBoolean(3, publicServer);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                createMessage  = "Creating server failed, no rows affected.";
                return;
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newServerID = generatedKeys.getInt(1);
                    createMessage  = "Server created successfully.";
                } else {
                    createMessage  = "Creating server failed, no ID obtained.";
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                createMessage = "Server name already exists. Please choose another name.";
            } else {
                createMessage = "Error creating server.";
            }
        }
        serverName = null;
        publicServer = false;
    }
    //  Load User Servers 
    public void loadUserServers() {
        message = "";
        servers.clear();
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
                    servers.add(server); 
                }
            }
        } catch (SQLException e) {
            message = "Failed to load user servers: " + e.getMessage();
        }
    }
    //  Load Public Servers 
    public void loadPublicServers() {
        publicLoaded = true;
        message = "";
        publicServers.clear(); 
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
        joinPublicServerMessage = "";
        if (conn == null || login == null) {
            joinPublicServerMessage = "Not connected to database or user not logged in.";
            return;
        }
        try {
            String checkSql = "SELECT is_public FROM servers WHERE serverID = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, serverId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    joinPublicServerMessage = "Server not found.";
                    return;
                }
                if (!rs.getBoolean("is_public")) {
                    joinPublicServerMessage = "This server is private. You need an invite.";
                    return;
                }
            }
            String existsSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            try (PreparedStatement existsStmt = conn.prepareStatement(existsSql)) {
                existsStmt.setInt(1, login.getUserId());
                existsStmt.setInt(2, serverId);
                ResultSet rs = existsStmt.executeQuery();
                if (rs.next()) {
                    joinPublicServerMessage = "You are already a member of this server.";
                    return;
                }
            }
            try {
                addMembership(serverId, login.getUserId(), false);
                joinPublicServerMessage = "Joined server successfully.";
            } catch (Exception e) {
                joinPublicServerMessage = e.getMessage();
                return;
            }
        } catch (SQLException e) {
            joinPublicServerMessage = "Failed to join server: " + e.getMessage();
            e.printStackTrace();
        }
    }
    // Invite User to Server by server name and username
    public void inviteUserToServer() {
        inviteMessage  = "";
        String serverName = this.inviteServerName;
        String targetUserName = this.inviteTargetUserName;
        if (conn == null || login == null) {
            inviteMessage  = "Not connected to database or user not logged in.";
            return;
        }
        if (inviteServerName == null || inviteServerName.trim().isEmpty()) {
            inviteMessage  = "Server name required.";
            return;
        }
        int serverId = -1;
        boolean isPublic = false;
        String getServerSql = "SELECT serverID, is_public FROM servers WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement serverStmt = conn.prepareStatement(getServerSql)) {
            serverStmt.setString(1, serverName.trim());
            try (ResultSet rs = serverStmt.executeQuery()) {
                if (rs.next()) {
                    serverId = rs.getInt("serverID");
                    isPublic = rs.getBoolean("is_public");
                } else {
                    inviteMessage  = "Server not found.";
                    return;
                }
            }
        } catch (SQLException e) {
            inviteMessage  = "Error finding server: " + e.getMessage();
            return;
        }
        boolean canInvite = false;
        if (isPublic) {
            String permSql = "SELECT can_invite FROM server_member_permissions WHERE userID = ? AND serverID = ?";
            try (PreparedStatement permStmt = conn.prepareStatement(permSql)) {
                permStmt.setInt(1, login.getUserId());
                permStmt.setInt(2, serverId);
                try (ResultSet rs = permStmt.executeQuery()) {
                    if (rs.next()) {
                        canInvite = rs.getBoolean("can_invite");
                    }
                }
            } catch (SQLException e) {
                inviteMessage = "Error checking permissions: " + e.getMessage();
                return;
            }
        } else {
            String permSql = "SELECT can_invite FROM server_member_permissions WHERE userID = ? AND serverID = ?";
            try (PreparedStatement permStmt = conn.prepareStatement(permSql)) {
                permStmt.setInt(1, login.getUserId());
                permStmt.setInt(2, serverId);
                try (ResultSet rs = permStmt.executeQuery()) {
                    if (rs.next()) {
                        canInvite = rs.getBoolean("can_invite");
                    }
                }
            } catch (SQLException e) {
                inviteMessage  = "Error checking permissions: " + e.getMessage();
                return;
            }
        }
        if (!canInvite) {
            inviteMessage  = "You do not have permission to invite users to this server.";
            return;
        }
        int targetUserId = -1;
        String getUserSql = "SELECT userID FROM users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement userStmt = conn.prepareStatement(getUserSql)) {
            userStmt.setString(1, targetUserName);
            try (ResultSet rs = userStmt.executeQuery()) {
                if (rs.next()) {
                    targetUserId = rs.getInt("userID");
                } else {
                    inviteMessage  = "User not found.";
                    return;
                }
            }
        } catch (SQLException e) {
            inviteMessage  = "Error finding user: " + e.getMessage();
            return;
        }
        if (targetUserId == login.getUserId()) {
            inviteMessage  = "You cannot invite yourself.";
            return;
        }
        String checkMemberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkMemberSql)) {
            checkStmt.setInt(1, targetUserId);
            checkStmt.setInt(2, serverId);
            try (ResultSet rsCheck = checkStmt.executeQuery()) {
                if (rsCheck.next()) {
                    inviteMessage  = "User is already a member of this server.";
                    return;
                }
            }
        } catch (SQLException e) {
            inviteMessage  = "Error checking membership: " + e.getMessage();
            return;
        }
        String sql = "INSERT INTO server_invites (serverID, invitedID, invited_by) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, serverId);
            stmt.setInt(2, targetUserId);
            stmt.setInt(3, login.getUserId());
            stmt.executeUpdate();
            inviteMessage  = "Invite sent successfully.";
        } catch (SQLException e) {
            if (e.getMessage().contains("uq_server_invites_target")) {
                inviteMessage  = "User already has a pending invite.";
            } else {
                inviteMessage  = "Failed to send invite: " + e.getMessage();
            }
        }
        inviteServerName = null;
        inviteTargetUserName = null;
    }
    //  load Invites for User
    public List<Invite> loadInvites() {
        loadInviteMessage = "";
        List<Invite> invites = new ArrayList<>();
        if (conn == null) {
            loadInviteMessage = "Not connected to database.";
            return invites;
        }
        if (login == null) {
            loadInviteMessage = "User not logged in.";
            return invites;
        }
        int userId = login.getUserId();
        String sql = "SELECT i.inviteID, s.name AS serverName, u.username AS inviterName " +
                     "FROM server_invites i " +
                     "JOIN servers s ON i.serverID = s.serverID " +
                     "JOIN users u ON i.invited_by = u.userID " +
                     "WHERE i.invitedID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    Invite invite = new Invite();
                    invite.setInviteID(rs.getInt("inviteID"));
                    invite.setServerName(rs.getString("serverName"));
                    invite.setInviterName(rs.getString("inviterName"));
                    invites.add(invite);
                    count++;
                }
            }
        } catch (SQLException e) {
            loadInviteMessage = "Failed to load invites: " + e.getMessage();
        }
        if (invites.isEmpty()) {
            loadInviteMessage = "No invites found for userId: " + userId;
        }
        return invites;
    }
    //Accept Invite
    public void acceptInvite(int inviteId) {
        loadInviteMessage = "";
        if (conn == null || login == null) {
            loadInviteMessage = "Not connected to database or user not logged in.";
            return;
        }
        int serverId = -1;
        String getInviteSql = "SELECT serverID FROM server_invites WHERE inviteID = ? AND invitedID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(getInviteSql)) {
            stmt.setInt(1, inviteId);
            stmt.setInt(2, login.getUserId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    serverId = rs.getInt("serverID");
                } else {
                    loadInviteMessage = "Invite not found.";
                    return;
                }
            }
        } catch (SQLException e) {
            loadInviteMessage = "Error finding invite: " + e.getMessage();
            return;
        }
        addMembership(serverId, login.getUserId(), false);
        String deleteSql = "DELETE FROM server_invites WHERE inviteID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, inviteId);
            stmt.executeUpdate();
            loadInviteMessage = "Invite accepted and joined server successfully.";
        } catch (SQLException e) {
            loadInviteMessage = "Failed to accept invite: " + e.getMessage();
        }
    }
    // Decline Invite
    public void declineInvite(int inviteId) {  
        loadInviteMessage = ""; 
        if (conn == null || login == null) {
            loadInviteMessage = "Not connected to database or user not logged in.";
            return;
        }
        String deleteSql = "DELETE FROM server_invites WHERE inviteID = ? AND invitedID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, inviteId);
            stmt.setInt(2, login.getUserId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                loadInviteMessage = "Invite declined successfully.";
            } else {
                loadInviteMessage = "Invite not found or you do not have permission to decline this invite.";
            }
        } catch (SQLException e) {
            loadInviteMessage = "Failed to decline invite: " + e.getMessage();
        }
    }
    //Kick User from Server 
    public void kickUserFromServer() {
        kickMessage  = "";
        if (conn == null || login == null) {
            kickMessage  = "Not connected.";
            return;
        }
        int serverId = -1;
        int targetUserId = -1;
        try {
            String serverSql = "SELECT serverID, is_public FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(serverSql);
            stmt.setString(1, kickServerName.trim());
            ResultSet rs = stmt.executeQuery();
            boolean isPublic = false;
            if (rs.next()) {
                serverId = rs.getInt("serverID");
                isPublic = rs.getBoolean("is_public");
            } else {
                kickMessage  = "Server not found.";
                return;
            }
            if (isPublic) {
                kickMessage  = "Cannot kick from public server.";
                return;
            }
            String permSql = "SELECT can_kick FROM server_member_permissions WHERE userID = ? AND serverID = ?";
            PreparedStatement permStmt = conn.prepareStatement(permSql);
            permStmt.setInt(1, login.getUserId()); // current user
            permStmt.setInt(2, serverId);
            ResultSet permRs = permStmt.executeQuery();
            if (!permRs.next() || !permRs.getBoolean("can_kick")) {
                kickMessage  = "You do not have permission to kick users.";
                return;
            }
            String userSql = "SELECT userID FROM users WHERE LOWER(username) = LOWER(?)";
            stmt = conn.prepareStatement(userSql);
            stmt.setString(1, kickTargetUserName.trim());
            rs = stmt.executeQuery();
            if (rs.next()) {
                targetUserId = rs.getInt("userID");
            } else {
                kickMessage  = "User not found.";
                return;
            }
            String deleteSql = "DELETE FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(deleteSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                kickMessage  = "User kicked successfully.";
            } else {
                kickMessage  = "User not in server.";
            }
        } catch (SQLException e) {
            kickMessage  = "Error: " + e.getMessage();
        }
        kickServerName = null;
        kickTargetUserName = null;
    }
    //transfer ownership of server 
    public void transferServerOwnership() {
        transferMessage = "";
        if (conn == null || login == null) {
            transferMessage = "Not connected.";
            return;
        }
        int serverId = -1;
        int targetUserId = -1;
        int oldOwnerId = -1;
        try {
            String serverSql = "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(serverSql);
            stmt.setString(1, transferServerName.trim());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                serverId = rs.getInt("serverID");
                oldOwnerId = rs.getInt("ownerID");
                if (oldOwnerId != login.getUserId()) {
                    transferMessage = "You are not the owner.";
                    return;
                }
            } else {
                transferMessage = "Server not found.";
                return;
            }
            String userSql = "SELECT userID FROM users WHERE LOWER(username) = LOWER(?)";
            stmt = conn.prepareStatement(userSql);
            stmt.setString(1, transferTargetUserName.trim());
            rs = stmt.executeQuery();
            if (rs.next()) {
                targetUserId = rs.getInt("userID");
            } else {
                transferMessage = "User not found.";
                return;
            }
            if (targetUserId == oldOwnerId) {
                transferMessage = "You are already the owner.";
                return;
            }
            String memberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(memberSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                transferMessage = "New owner must be a member.";
                return;
            }
            String ensurePerm = "INSERT IGNORE INTO server_member_permissions (userID, serverID) VALUES (?, ?)";
            stmt = conn.prepareStatement(ensurePerm);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            String updateOwner = "UPDATE servers SET ownerID = ? WHERE serverID = ?";
            stmt = conn.prepareStatement(updateOwner);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            String updateSql =  "UPDATE server_member_permissions SET " +
                                "can_invite=1, can_kick=1, can_create_channel=1, " +
                                "can_manage_roles=1, can_delete_messages=1, can_delete_server=1 " +
                                "WHERE userID=? AND serverID=?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            String removeRoles = "UPDATE server_member_permissions SET " +
                                "can_kick=0, can_manage_roles=0, can_delete_messages=0, can_delete_server=0 " +
                                "WHERE userID=? AND serverID=?";
            stmt = conn.prepareStatement(removeRoles);
            stmt.setInt(1, oldOwnerId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            transferMessage = "Ownership transferred successfully.";
        } catch (SQLException e) {
            transferMessage = "Error: " + e.getMessage();
        }
        transferServerName = null;
        transferTargetUserName = null;
    }
    //leave server
    public void leaveServer() {
        leaveMessage = "";
        if (conn == null || login == null) {
            leaveMessage = "Not connected.";
            return;
        }
        if (leaveServerName == null || leaveServerName.trim().isEmpty()) {
            leaveMessage = "Server name required.";
            return;
        }
        try {
            String ownerSql = "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(ownerSql);
            stmt.setString(1, leaveServerName.trim());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                leaveMessage = "Server not found.";
                return;
            }
            int serverId = rs.getInt("serverID");
            int ownerId = rs.getInt("ownerID");
            if (ownerId == login.getUserId()) {
                leaveMessage = "You are the owner. Transfer ownership before leaving the server.";
                return;
            }
            String checkSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, serverId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                leaveMessage = "You are not a member of this server.";
                return;
            }
            String deleteSql = "DELETE FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(deleteSql);
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            leaveMessage = "Left server successfully.";
        } catch (SQLException e) {
            leaveMessage = "Error: " + e.getMessage();
        }
        leaveServerName = null;
    }
    //has permission
    public boolean hasPermission(int serverId, String permissionColumn) {
        if (conn == null || login == null) return false;

        String sql = "SELECT " + permissionColumn +
                    " FROM server_member_permissions " +
                    "WHERE userID = ? AND serverID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, serverId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean(permissionColumn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
    //load server members 
    public List<String> loadServerMembers(int serverId) {
        message = "";
        List<String> members = new ArrayList<>();
        if (conn == null || login == null) {
            return members;
        }
        try {
            String checkMemberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            PreparedStatement stmt = conn.prepareStatement(checkMemberSql);
            stmt.setInt(1, login.getUserId());
            stmt.setInt(2, serverId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                message = "You are not a member of this server.";
                return members;
            }
            String ownerSql = "SELECT ownerID FROM servers WHERE serverID = ?";
            stmt = conn.prepareStatement(ownerSql);
            stmt.setInt(1, serverId);
            rs = stmt.executeQuery();
            boolean isAdmin = false;
            if (rs.next()) {
                isAdmin = (rs.getInt("ownerID") == login.getUserId());
            }
            if (isAdmin) {
                String sql = "SELECT u.username FROM users u " +
                            "JOIN server_members sm ON u.userID = sm.userID " +
                            "WHERE sm.serverID = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, serverId);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    members.add(rs.getString("username"));
                }
                return members;
            }
            String sql =
                "SELECT u.username FROM users u " +
                "JOIN server_members sm ON u.userID = sm.userID " +
                "WHERE sm.serverID = ? " +
                "AND u.userID NOT IN ( " +
                "   SELECT blockedID FROM blocks WHERE userID = ? " +
                ") " +
                "AND u.userID NOT IN ( " +
                "   SELECT userID FROM blocks WHERE blockedID = ? " +
                ")";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, serverId);
            stmt.setInt(2, login.getUserId());
            stmt.setInt(3, login.getUserId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            message = "Error loading members: " + e.getMessage();
        }
        return members;
    }
    // delete server by name (ONLY OWNER)
    public void deleteServerByName(String name){
        deleteServerMessage = "";
        if (conn == null || login == null) {
            deleteServerMessage = "Not connected.";
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            deleteServerMessage = "Server name cannot be empty.";
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name.trim());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                deleteServerMessage = "Server not found.";
                return;
            }
            int serverId = rs.getInt("serverID");
            int ownerId = rs.getInt("ownerID");
            if (ownerId != login.getUserId()) {
                deleteServerMessage = "Only the owner can delete this server.";
                return;
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM servers WHERE serverID = ?")) {
                deleteStmt.setInt(1, serverId);
                int rows = deleteStmt.executeUpdate();
                if (rows > 0) {
                    deleteServerMessage = "Server deleted successfully.";
                } else {
                    deleteServerMessage = "Delete failed.";
                }
            }
        } catch (SQLException e) {
            deleteServerMessage = "Error: " + e.getMessage();
        }
    }
    //grant role to user
    public void givePermission() {
        grantRemovePermisionMessage = "";
        if (conn == null || login == null) {
            grantRemovePermisionMessage = "Not connected.";
            return;
        }
        if (manageServerName == null || manageServerName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Server name required.";
            return;
        }

        if (targetUserName == null || targetUserName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Username required.";
            return;
        }

        if (permissionName == null || permissionName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Please select a permission.";
            return;
        }
        int serverId = -1;
        int ownerId = -1;
        int targetUserId = -1;
        try {
            String serverSql = "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(serverSql);
            stmt.setString(1, manageServerName.trim());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                serverId = rs.getInt("serverID");
                ownerId = rs.getInt("ownerID");
            } else {
                grantRemovePermisionMessage = "Server not found.";
                return;
            }
            if (ownerId != login.getUserId()) {
                grantRemovePermisionMessage = "You are not the owner.";
                return;
            }
            String userSql = "SELECT userID FROM users WHERE LOWER(username) = LOWER(?)";
            stmt = conn.prepareStatement(userSql);
            stmt.setString(1, targetUserName.trim());
            rs = stmt.executeQuery();
            if (rs.next()) {
                targetUserId = rs.getInt("userID");
            } else {
                grantRemovePermisionMessage = "User not found.";
                return;
            }
            String memberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(memberSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                grantRemovePermisionMessage = "User is not a member.";
                return;
            }
            List<String> validPermissions = List.of(
                "can_invite",
                "can_kick",
                "can_create_channel",
                "can_manage_roles",
                "can_delete_messages",
                "can_delete_server"
            );
            if (!validPermissions.contains(permissionName)) {
                grantRemovePermisionMessage = "Permission does not exist.";
                return;
            }
            if (permissionName.equals("can_delete_server") ||
                permissionName.equals("can_delete_messages")) {
                grantRemovePermisionMessage = "Only owner can have this permission.";
                return;
            }
            String insertSql = "INSERT IGNORE INTO server_member_permissions (userID, serverID) VALUES (?, ?)";
            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            String updateSql = "UPDATE server_member_permissions SET " + permissionName + " = TRUE WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
            grantRemovePermisionMessage = "Permission granted successfully.";
        } catch (SQLException e) {
            grantRemovePermisionMessage = "Error: " + e.getMessage();
        }
    }
    //remove permission from user
    public void removePermission() {
        grantRemovePermisionMessage = "";
        if (conn == null || login == null) {
            grantRemovePermisionMessage = "Not connected.";
            return;
        }
        if (manageServerName == null || manageServerName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Server name required.";
            return;
        }

        if (targetUserName == null || targetUserName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Username required.";
            return;
        }

        if (permissionName == null || permissionName.trim().isEmpty()) {
            grantRemovePermisionMessage = "Please select a permission.";
            return;
        }
        int serverId = -1;
        int ownerId = -1;
        int targetUserId = -1;
        try {
            String serverSql = "SELECT serverID, ownerID FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(serverSql);
            stmt.setString(1, manageServerName.trim());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                serverId = rs.getInt("serverID");
                ownerId = rs.getInt("ownerID");
            } else {
                grantRemovePermisionMessage = "Server not found.";
                return;
            }
            if (ownerId != login.getUserId()) {
                grantRemovePermisionMessage = "Only the owner can remove permissions.";
                return;
            }
            String userSql = "SELECT userID FROM users WHERE LOWER(username) = LOWER(?)";
            stmt = conn.prepareStatement(userSql);
            stmt.setString(1, targetUserName.trim());
            rs = stmt.executeQuery();
            if (rs.next()) {
                targetUserId = rs.getInt("userID");
            } else {
                grantRemovePermisionMessage = "User not found.";
                return;
            }
            if (targetUserId == ownerId) {
                grantRemovePermisionMessage = "Owner cannot remove their own permissions.";
                return;
            }

            // 4. Check membership
            String memberSql = "SELECT 1 FROM server_members WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(memberSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                grantRemovePermisionMessage = "User is not a member of this server.";
                return;
            }
            List<String> validPermissions = List.of(
                "can_invite",
                "can_kick",
                "can_create_channel",
                "can_manage_roles",
                "can_delete_messages",
                "can_delete_server"
            );
            if (!validPermissions.contains(permissionName)) {
                grantRemovePermisionMessage = "Permission does not exist.";
                return;
            }
            String updateSql = "UPDATE server_member_permissions SET " + permissionName + " = FALSE WHERE userID = ? AND serverID = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, targetUserId);
            stmt.setInt(2, serverId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                grantRemovePermisionMessage = "Permission removed successfully.";
            } else {
                grantRemovePermisionMessage = "No permission found to remove.";
            }
        } catch (SQLException e) {
            grantRemovePermisionMessage = "Error: " + e.getMessage();
        }
    }
    //load permisions in a server
    public void loadPermissions() {
        permissionMessage = "";
        permissionList.clear();
        if (conn == null || login == null) {
            permissionMessage = "Not connected.";
            return;
        }
        if (permissionServerName == null || permissionServerName.trim().isEmpty()) {
            permissionMessage = "Server name required.";
            return;
        }
        int serverId = -1;
        try {
            String serverSql = "SELECT serverID FROM servers WHERE LOWER(name) = LOWER(?)";
            PreparedStatement stmt = conn.prepareStatement(serverSql);
            stmt.setString(1, permissionServerName.trim());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                serverId = rs.getInt("serverID");
            } else {
                permissionMessage = "Server not found.";
                return;
            }
            int ownerId = -1;
            String ownerSql = "SELECT ownerID FROM servers WHERE serverID = ?";
            PreparedStatement ownerStmt = conn.prepareStatement(ownerSql);
            ownerStmt.setInt(1, serverId);
            ResultSet ownerRs = ownerStmt.executeQuery();
            if (ownerRs.next()) {
                ownerId = ownerRs.getInt("ownerID");
            }
            boolean isOwner = (ownerId == login.getUserId());
            String sql;
            if (isOwner) {
                sql =
                    "SELECT u.username, " +
                    "COALESCE(p.can_invite, FALSE) AS can_invite, " +
                    "COALESCE(p.can_kick, FALSE) AS can_kick, " +
                    "COALESCE(p.can_create_channel, FALSE) AS can_create_channel, " +
                    "COALESCE(p.can_manage_roles, FALSE) AS can_manage_roles, " +
                    "COALESCE(p.can_delete_messages, FALSE) AS can_delete_messages, " +
                    "COALESCE(p.can_delete_server, FALSE) AS can_delete_server " +
                    "FROM server_members sm " +
                    "JOIN users u ON sm.userID = u.userID " +
                    "LEFT JOIN server_member_permissions p " +
                    "ON sm.userID = p.userID AND sm.serverID = p.serverID " +
                    "WHERE sm.serverID = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, serverId);
            } else {
                sql =
                    "SELECT u.username, " +
                    "COALESCE(p.can_invite, FALSE) AS can_invite, " +
                    "COALESCE(p.can_kick, FALSE) AS can_kick, " +
                    "COALESCE(p.can_create_channel, FALSE) AS can_create_channel, " +
                    "COALESCE(p.can_manage_roles, FALSE) AS can_manage_roles, " +
                    "COALESCE(p.can_delete_messages, FALSE) AS can_delete_messages, " +
                    "COALESCE(p.can_delete_server, FALSE) AS can_delete_server " +
                    "FROM server_members sm " +
                    "JOIN users u ON sm.userID = u.userID " +
                    "LEFT JOIN server_member_permissions p " +
                    "ON sm.userID = p.userID AND sm.serverID = p.serverID " +
                    "WHERE sm.serverID = ? AND sm.userID = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, serverId);
                stmt.setInt(2, login.getUserId());
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                MemberPermissionView mp = new MemberPermissionView();
                mp.setUsername(rs.getString("username"));
                mp.setCanInvite(rs.getBoolean("can_invite"));
                mp.setCanKick(rs.getBoolean("can_kick"));
                mp.setCanCreateChannel(rs.getBoolean("can_create_channel"));
                mp.setCanManageRoles(rs.getBoolean("can_manage_roles"));
                mp.setCanDeleteMessages(rs.getBoolean("can_delete_messages"));
                mp.setCanDeleteServer(rs.getBoolean("can_delete_server"));
                permissionList.add(mp);
            }
            permissionMessage = "Permissions loaded successfully.";
        } catch (SQLException e) {
            permissionMessage = "Error: " + e.getMessage();
        }
    }
    //  Getters and Setters 
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
    public String getInviteServerName() { 
        return inviteServerName; 
    }
    public void setInviteServerName(String inviteServerName) { 
        this.inviteServerName = inviteServerName; 
    }
    public String getInviteTargetUserName() { 
        return inviteTargetUserName; 
    }
    public void setInviteTargetUserName(String inviteTargetUserName) { 
        this.inviteTargetUserName = inviteTargetUserName; 
    }
    public String getMessage() {
        return message;
    }
    public void loadMyInvites() {
        myInvites = loadInvites();
    }
    public List<Invite> getMyInvites() {
        return myInvites;
    }
    public String getKickServerName() {
        return kickServerName;
    }
    public void setKickServerName(String kickServerName) {
        this.kickServerName = kickServerName;
    }
    public String getKickTargetUserName() {
        return kickTargetUserName;
    }
    public void setKickTargetUserName(String kickTargetUserName) {
        this.kickTargetUserName = kickTargetUserName;
    }
    public String getTransferServerName() {
        return transferServerName;
    }
    public void setTransferServerName(String transferServerName) {
        this.transferServerName = transferServerName;
    }
    public String getTransferTargetUserName() {
        return transferTargetUserName;
    }
    public void setTransferTargetUserName(String transferTargetUserName) {
        this.transferTargetUserName = transferTargetUserName;
    }
    public String getLeaveServerName() {
        return leaveServerName;
    }
    public void setLeaveServerName(String leaveServerName) {
        this.leaveServerName = leaveServerName;
    }
    public String getPermissionName() {
        return permissionName;
    }
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
    public List<MemberPermissionView> getPermissionList() {
        return permissionList;
    }
    public String getTargetUserName() {
        return targetUserName;
    }
    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }
    public String getCreateMessage() {
        return createMessage;
    }
    public void setCreateMessage(String createMessage) {
        this.createMessage = createMessage;
    }
    public String getInviteMessage() {
        return inviteMessage;
    }
    public void setInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
    }
    public String getKickMessage() {
        return kickMessage;
    }
    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }
    public String getTransferMessage() {
        return transferMessage;
    }
    public void setTransferMessage(String transferMessage) {
        this.transferMessage = transferMessage;
    }
    public String getLeaveMessage() {
        return leaveMessage;
    }
    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }
    public String getPermissionMessage() {
        return permissionMessage;
    }
    public void setPermissionMessage(String permissionMessage) {
        this.permissionMessage = permissionMessage;
    }
    public String getDeleteServerMessage() {
        return deleteServerMessage;
    }
    public void setDeleteServerMessage(String deleteServerMessage) {
        this.deleteServerMessage = deleteServerMessage;
    }
    public String getJoinPublicServerMessage() {
        return joinPublicServerMessage;
    }
    public void setJoinPublicServerMessage(String joinPublicServerMessage) {
        this.joinPublicServerMessage = joinPublicServerMessage;
    }
    public String getLoadInviteMessage() {
        return loadInviteMessage;
    }
    public void setLoadInviteMessage(String loadInviteMessage) {
        this.loadInviteMessage = loadInviteMessage;
    }
    public String getGrantRemovePermisionMessage() {
        return grantRemovePermisionMessage;
    }
    public void setGrantRemovePermisionMessage(String grantRemovePermisionMessage) {
        this.grantRemovePermisionMessage = grantRemovePermisionMessage;
    }
    public String getPermissionServerName() {
        return permissionServerName;
    }
    public void setPermissionServerName(String permissionServerName) {
        this.permissionServerName = permissionServerName;
    }
    public String getManageServerName() {
        return manageServerName;
    }
    public void setManageServerName(String manageServerName) {
        this.manageServerName = manageServerName;
    }
    public boolean isPublicLoaded() {
        return publicLoaded;
    }
    
}