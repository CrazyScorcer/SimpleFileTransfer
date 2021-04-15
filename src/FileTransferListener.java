public interface FileTransferListener 
{
    void connected(String ipAddress);
    void disconnected();

    void transferRequest(String fileName, long fileSize);
    void requestAccepted();
    void requestRejected();
    void transferProgress(float percentComplete, long bytesPerSecond);
    void transferDone();
}
