package finalproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.sql.DataSource;


import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("serverBean")
@RequestScoped
public class ServerService {
    private Connection conn;
    private int serverID;
    private int userID;
    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getServerID() {
        return serverID;
    }

    public void setServerID(int serverID) {
        this.serverID = serverID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    @PostConstruct
    public void openConnection(){
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup("java:/comp/env/jdbc/javaproject");
            this.conn = ds.getConnection();
            this.message = "";

        } catch (NamingException | SQLException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("PostConstruct Finished Successfully");
    }

/**
 * This method uses a prepared statement to see if the user is aleady in the server.
 * If not, then another prepared statemwnt is used to add them to the server. 
 * @param serverID Server ID
 * @param userID User ID
 */
    public void joinServer(){
        try (
            PreparedStatement stmt = conn.prepareStatement(
            "SELECT user_id, server_id FROM server_members WHERE user_id = ? AND server_id = ?;");
        ) {
            stmt.setInt(1, this.userID);
            stmt.setInt(2, this.serverID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                this.message = "User is already in the server.";
            }
            else{
                try (
                    PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO server_members (user_id, server_id) VALUES (?, ?);");
                ) {
                    insertStmt.setInt(1, this.userID);
                    insertStmt.setInt(2, this.serverID);
                    insertStmt.execute();
                    this.message = "User sucessfully added to the server!";
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This method is used when a memeber is removed/kicked from a server.
     * A prepared statement is used to see if they are in the server.
     * if so, then they are removed.
     * @param serverID Server ID
     * @param userID user ID
     */
    public void leaveServer(){
        try (
            PreparedStatement stmt = conn.prepareStatement(
            "SELECT user_id, server_id FROM server_members WHERE user_id = ? AND server_id = ?;");
        ) {
            stmt.setInt(1, this.userID);
            stmt.setInt(2, this.serverID);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                this.message = "User is not in the server.";
            }
            else{
                try (
                    PreparedStatement deleteStmt = conn.prepareStatement(
            "DELETE FROM server_members WHERE user_id = ? AND server_id = ?;");
                ) {
                    deleteStmt.setInt(1, this.userID);
                    deleteStmt.setInt(2, this.serverID);
                    deleteStmt.execute();
                    this.message = "User sucessfully removed from the server!";
                    
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
}
