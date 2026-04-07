package finalproject;
import java.sql.Timestamp;

public class Channel {
    private int channelId;
    private int serverId;
    private String channelName;
    private int createdBy;
    private Timestamp createdAt;

    public Channel() {}

    public Channel(int channelId, int serverId, String channelName, int createdBy, Timestamp createdAt) {
        this.channelId = channelId;
        this.serverId = serverId;
        this.channelName = channelName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}