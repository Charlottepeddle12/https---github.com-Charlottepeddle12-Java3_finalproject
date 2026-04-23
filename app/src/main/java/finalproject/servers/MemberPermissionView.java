package finalproject.servers;

public class MemberPermissionView {

    private String username;

    private boolean canInvite;
    private boolean canKick;
    private boolean canCreateChannel;
    private boolean canManageRoles;
    private boolean canDeleteMessages;
    private boolean canDeleteServer;

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isCanInvite() {
        return canInvite;
    }

    public void setCanInvite(boolean canInvite) {
        this.canInvite = canInvite;
    }

    public boolean isCanKick() {
        return canKick;
    }

    public void setCanKick(boolean canKick) {
        this.canKick = canKick;
    }

    public boolean isCanCreateChannel() {
        return canCreateChannel;
    }

    public void setCanCreateChannel(boolean canCreateChannel) {
        this.canCreateChannel = canCreateChannel;
    }

    public boolean isCanManageRoles() {
        return canManageRoles;
    }

    public void setCanManageRoles(boolean canManageRoles) {
        this.canManageRoles = canManageRoles;
    }

    public boolean isCanDeleteMessages() {
        return canDeleteMessages;
    }

    public void setCanDeleteMessages(boolean canDeleteMessages) {
        this.canDeleteMessages = canDeleteMessages;
    }

    public boolean isCanDeleteServer() {
        return canDeleteServer;
    }

    public void setCanDeleteServer(boolean canDeleteServer) {
        this.canDeleteServer = canDeleteServer;
    }
}