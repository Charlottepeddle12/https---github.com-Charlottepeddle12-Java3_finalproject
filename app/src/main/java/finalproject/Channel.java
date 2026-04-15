package finalproject;
import java.sql.Timestamp;

public class Channel {
    private int channelID;
    private int serverID;
    private String name;
    private Timestamp createdAt;

    public Channel() {}

    public Channel(int channelID, int serverID, String name, Timestamp createdAt) {
        this.channelID = channelID;
        this.serverID = serverID;
        this.name = name;
        this.createdAt = createdAt;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public int getServerID() {
        return serverID;
    }

    public void setServerID(int serverID) {
        this.serverID = serverID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}