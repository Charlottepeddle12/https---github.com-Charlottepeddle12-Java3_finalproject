package finalproject;

public class Server {
    private String name;
    private int ownerID;
    private boolean isPublic;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getOwnerID() {
        return ownerID;
    }
    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }
    public boolean isPublic() {
        return isPublic;
    }
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
