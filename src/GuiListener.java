import java.io.File;

public interface GuiListener 
{
    void host(String ipAddress, int port);
    void connect(String ipAddress, int port);
    void cancel();
    void sendFile(File file);

    void acceptRequest(File saveLocation);
    void rejectRequest();
}
