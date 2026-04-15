package finalproject;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.naming.Context;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Named("userLoginBean")
@SessionScoped
public class UserLogin implements Serializable{
        // JSF navigation login method for dashboard
        public String login() {
            updateToken();
            if (token != null && userId > 0) {
                return "dashboard.xhtml?faces-redirect=true";
            }
            return null; // Stay on the same page if login fails
        }
    @NotNull
    private String userName;
    @NotNull
    @NotBlank
    @Size(min=12,max=36)
    private String userPassword;
    private String token;
    private String message;
    private int userId;
    private Connection conn;

    private boolean verifyPassword(byte[] bytes) throws UnsupportedEncodingException{
        return BCrypt.verifyer().verify(userPassword.getBytes("UTF-16"), bytes).verified;
    }

    @PostConstruct
    public void openConnection(){
        try {            
            token = null;
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/javaproject");
            conn = ds.getConnection();
        } catch (Exception e) {
            message = e.getMessage();
        }
        System.out.println(token);
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
        
        System.out.println(token);
    }

    public void updateToken() {
        if (conn == null) {
            message = "Database connection is not available.";
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT userID, token, PW_Hash FROM users WHERE username = ?")) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && verifyPassword(rs.getBytes("PW_Hash"))) {
                    token = rs.getString("token");
                    userId = rs.getInt("userID");
                    message = "";
                } else {
                    message = "Invalid login";
                }
            } catch (SQLException | UnsupportedEncodingException e) {
                message = e.getMessage();
                return;
            }
        } catch (SQLException e) {
            message = e.getMessage();
            return;
        }
    }

    public void signup() {
        if (conn == null) {
            message = "Database connection is not available.";
            return;
        }

        try(PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, PW_Hash, token) VALUES (?, ?, SHA2(RAND(), 256))")) {
            byte[] hash = BCrypt.withDefaults().hash(12, userPassword.getBytes("UTF-16"));
            stmt.setString(1, userName);
            stmt.setBytes(2, hash);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                message = "Failed to create new user: " + userName;
            } else {
                message = "Successfully created new user: " + userName;
            }
        }catch (SQLException | UnsupportedEncodingException e) {
            message = e.getMessage();
            return;
        }
    }

    public void logout() {
        token = null;
        userId = 0;
        userPassword = null;
        message = "Logged out.";
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getUserPassword() {
        return userPassword;
    }
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
    public String getToken() {
        return token;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getMessage() {
        return message;
    }
}


