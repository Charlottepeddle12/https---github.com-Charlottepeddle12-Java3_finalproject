package finalproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // USERS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL
                );
            """);

            // SERVERS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS servers (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    owner_id INT,
                    is_public BOOLEAN,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                );
            """);

            // SERVER MEMBERS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS server_members (
                    user_id INT,
                    server_id INT,
                    role VARCHAR(50),
                    PRIMARY KEY (user_id, server_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (server_id) REFERENCES servers(id)
                );
            """);

            // CHANNELS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS channels (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    server_id INT,
                    name VARCHAR(100),
                    FOREIGN KEY (server_id) REFERENCES servers(id)
                );
            """);

            // MESSAGES
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    channel_id INT,
                    user_id INT,
                    content TEXT,
                    sent_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (channel_id) REFERENCES channels(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
            """);

            // FRIENDS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friends (
                    user_id INT,
                    friend_id INT,
                    PRIMARY KEY (user_id, friend_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (friend_id) REFERENCES users(id)
                );
            """);

            // BLOCKS
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS blocks (
                    user_id INT,
                    blocked_id INT,
                    PRIMARY KEY (user_id, blocked_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (blocked_id) REFERENCES users(id)
                );
            """);

            // DIRECT MESSAGES
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS direct_messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender_id INT,
                    receiver_id INT,
                    content TEXT,
                    sent_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (sender_id) REFERENCES users(id),
                    FOREIGN KEY (receiver_id) REFERENCES users(id)
                );
            """);

            System.out.println("All tables created successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        private static final String URL = "jdbc:mariadb://localhost:3306/javaproject";
        private static final String USER = "root"; //Database username here
        private static final String PWORD = "Bokagi89."; //Database password here

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PWORD)) {
            init(conn);
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
