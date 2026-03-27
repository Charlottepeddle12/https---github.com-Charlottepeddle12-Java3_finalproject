package finalproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.naming.Context;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("chatLogBean")
@RequestScoped
public class Messages {
    private List<String> chatLog = new LinkedList<>();
    private Connection conn;
    private String messageToPost;

    public String getMessageToPost() {
        return messageToPost;
    }

    public void setMessageToPost(String messageToPost) {
        this.messageToPost = messageToPost;
    }

    @PostConstruct
    public void openConnection(){
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup("java:/comp/env/jdbc/DsExampleDB");
            this.conn = ds.getConnection();
        } catch (NamingException | SQLException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("PostConstruct Finished Successfully");
    }

    public void pullLog() {
        try (
            PreparedStatement stmt = conn.prepareStatement("SELECT message, sentOn FROM messages;");
            ResultSet rs = stmt.executeQuery();
        ) {
            chatLog.clear();
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append(rs.getTimestamp(2));
                sb.append(": ");
                sb.append(rs.getString(1));
                chatLog.add(sb.toString());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("PullLog Finished Successfully");
    }

    public void postMessage(){
        // TODO: Post a message to the DB, and update the chat log
    }

    public List<String> getChatLog(){
        return chatLog;
    }

    @PreDestroy
    public void closeConnection(){
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("PreDestroy Finished Successfully");
    }
}

