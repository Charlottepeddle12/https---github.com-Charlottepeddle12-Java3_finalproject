package finalproject;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import finalproject.Users.UserLogin;

import javax.naming.Context;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("chatLogBean")
@SessionScoped
public class Messages implements Serializable{
    private List<String> chatLog = new LinkedList<>();
    private Connection conn;
    private String messageToPost;
    @Inject private UserLogin login;

    @PostConstruct
    public void openConnection(){
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (NamingException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @PreDestroy
    public void closeConnection(){
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void pullLog() {
        try (PreparedStatement ps = conn.prepareStatement("SELECT message, sentOn, username FROM messages JOIN users ON messages.userID = users.userID");
             ResultSet rs = ps.executeQuery()) {
            chatLog.clear();
            while (rs.next()) {
                String logEntry =  rs.getString("username") + "@" + rs.getTimestamp("sentOn") + ": " + rs.getString("message");
                chatLog.add(logEntry);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void postMessage(){
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO messages (message, sentOn, userID) VALUES (?, ?, ?)")) {
            ps.setString(1, messageToPost);
            ps.setTimestamp(2, new Timestamp(Instant.now().toEpochMilli()));
            ps.setInt(3, login.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        pullLog();
    }

    public List<String> getChatLog(){
        return chatLog;
    }

    public String getMessageToPost() {
        return messageToPost;
    }

    public void setMessageToPost(String messageToPost) {
        this.messageToPost = messageToPost;
    }
}
