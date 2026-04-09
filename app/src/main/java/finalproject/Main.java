package finalproject;

import java.sql.*;
import java.util.*;

// =====================
// MAIN CLASS
// =====================
public class Main {

    static final String URL = "jdbc:mariadb://localhost:3308/Java_Project";
    static final String USER = "root";
    static final String PASSWORD = "Lynn";

    public static void main(String[] args) {
        System.out.println("Starting Java Final app...");

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MariaDB JDBC driver was not found on the classpath.");
            e.printStackTrace();
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            UserService service = new UserService(conn);
            service.initializeDatabase();

            User connor = service.ensureUser("connor", "123");
            User alex = service.ensureUser("alex", "456");
            User sam = service.ensureUser("sam", "789");

            User user = service.login("connor", "123");
            if (user != null) {
                System.out.println("Logged in: " + user.getUsername());
            } else {
                System.out.println("Login failed for user connor.");
                return;
            }

            service.addFriend(connor.getId(), alex.getId());
            service.blockUser(connor.getId(), sam.getId());
            service.sendDM(connor.getId(), alex.getId(), "Hello from Connor!");

            List<DM> messages = service.getDMs(connor.getId(), alex.getId());
            for (DM dm : messages) {
                System.out.println(dm.getSenderId() + " -> " + dm.getReceiverId() + ": " + dm.getContent());
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed. Update URL/USER/PASSWORD in Main.java or start MariaDB.");
            System.out.println("Tried to connect to: " + URL);
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("Error code: " + e.getErrorCode());
            System.out.println("Reason: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class User {
        int id;
        String username;
        String password;

        public User() {}

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }

        public void setId(int id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
    }

    static class DM {
        int senderId;
        int receiverId;
        String content;

        public DM(int senderId, int receiverId, String content) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.content = content;
        }

        public int getSenderId() { return senderId; }
        public int getReceiverId() { return receiverId; }
        public String getContent() { return content; }
    }

   
    static class UserDAO {

        private Connection conn;

        public UserDAO(Connection conn) {
            this.conn = conn;
        }

        public void initializeDatabase() throws Exception {
            String createUsers = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL
                    )
                    """;

            String createFriends = """
                    CREATE TABLE IF NOT EXISTS friends (
                        user_id INT NOT NULL,
                        friend_id INT NOT NULL
                    )
                    """;

            String createBlockedUsers = """
                    CREATE TABLE IF NOT EXISTS blocked_users (
                        user_id INT NOT NULL,
                        blocked_id INT NOT NULL
                    )
                    """;

            String createDmMessages = """
                    CREATE TABLE IF NOT EXISTS dm_messages (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        sender_id INT NOT NULL,
                        receiver_id INT NOT NULL,
                        content TEXT NOT NULL
                    )
                    """;

            Statement statement = conn.createStatement();
            statement.executeUpdate(createUsers);
            statement.executeUpdate(createFriends);
            statement.executeUpdate(createBlockedUsers);
            statement.executeUpdate(createDmMessages);
            statement.close();
        }

        public User createUser(User user) throws Exception {
            String sql = "INSERT INTO users(username, password) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.username);
            ps.setString(2, user.password);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                user.setId(keys.getInt(1));
            }

            keys.close();
            ps.close();
            return user;
        }

        public User getUserByUsername(String username) throws Exception {
            String sql = "SELECT * FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.password = rs.getString("password");
                rs.close();
                ps.close();
                return user;
            }

            rs.close();
            ps.close();
            return null;
        }

        public User ensureUser(User user) throws Exception {
            User existingUser = getUserByUsername(user.username);
            if (existingUser != null) {
                return existingUser;
            }
            return createUser(user);
        }

        // LOGIN
        public User login(String username, String password) throws Exception {
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                rs.close();
                ps.close();
                return user;
            }
            rs.close();
            ps.close();
            return null;
        }

        // ADD FRIEND
        public void addFriend(int userId, int friendId) throws Exception {
            if (relationshipExists("friends", "friend_id", userId, friendId)) {
                return;
            }

            String sql = "INSERT INTO friends(user_id, friend_id) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.executeUpdate();
            ps.close();
        }

        // BLOCK USER
        public void blockUser(int userId, int blockedId) throws Exception {
            if (relationshipExists("blocked_users", "blocked_id", userId, blockedId)) {
                return;
            }

            String sql = "INSERT INTO blocked_users(user_id, blocked_id) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, blockedId);
            ps.executeUpdate();
            ps.close();
        }

        // SEND DM
        public void sendDM(DM dm) throws Exception {
            String sql = "INSERT INTO dm_messages(sender_id, receiver_id, content) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, dm.senderId);
            ps.setInt(2, dm.receiverId);
            ps.setString(3, dm.content);
            ps.executeUpdate();
            ps.close();
        }

        // GET DMs
        public List<DM> getDMs(int u1, int u2) throws Exception {
            List<DM> list = new ArrayList<>();

            String sql = "SELECT * FROM dm_messages WHERE " +
                    "(sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, u1);
            ps.setInt(2, u2);
            ps.setInt(3, u2);
            ps.setInt(4, u1);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new DM(
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getString("content")
                ));
            }

            rs.close();
            ps.close();
            return list;
        }

        private boolean relationshipExists(String tableName, String targetColumn, int userId, int targetId) throws Exception {
            String sql = "SELECT 1 FROM " + tableName + " WHERE user_id=? AND " + targetColumn + "=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, targetId);

            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();

            rs.close();
            ps.close();
            return exists;
        }
    }

    static class UserService {

        private UserDAO dao;

        public UserService(Connection conn) {
            this.dao = new UserDAO(conn);
        }

        public void initializeDatabase() throws Exception {
            dao.initializeDatabase();
        }

        public User ensureUser(String username, String password) throws Exception {
            return dao.ensureUser(new User(username, password));
        }

        public User login(String username, String password) throws Exception {
            return dao.login(username, password);
        }

        public void addFriend(int u1, int u2) throws Exception {
            dao.addFriend(u1, u2);
        }

        public void blockUser(int u1, int u2) throws Exception {
            dao.blockUser(u1, u2);
        }

        public void sendDM(int sender, int receiver, String msg) throws Exception {
            dao.sendDM(new DM(sender, receiver, msg));
        }

        public List<DM> getDMs(int u1, int u2) throws Exception {
            return dao.getDMs(u1, u2);
        }
    }
}
