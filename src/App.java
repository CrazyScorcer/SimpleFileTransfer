import java.io.File;

import javax.swing.SwingUtilities;

public class App 
{
    public static void main(String[] args) throws Exception 
    {
        Gui gui = new Gui();
        FileTransfer transfer = new FileTransfer();

        gui.setListener(new GuiListener() {
            @Override
            public void host(String ipAddress, int port) {
                transfer.host(ipAddress, port);
            }
            @Override
            public void cancel() {
                transfer.disconnect();
            }
            @Override
            public void connect(String ipAddress, int port) {
                transfer.connect(ipAddress, port);
            }
            @Override
            public void sendFile(File file) {
                transfer.startTransfer(file);
            }
			@Override
			public void acceptRequest(File saveLocation) {
                transfer.setSaveLocation(saveLocation);
                transfer.replyToTransferRequest(true);
			}
			@Override
			public void rejectRequest() {
                transfer.replyToTransferRequest(false);
			}
        });

        transfer.setListener(new FileTransferListener() {
            @Override
            public void connected(String ipAddress) {
                SwingUtilities.invokeLater(() -> {
                    gui.connected(ipAddress);
                });
            }
            @Override
            public void disconnected() {
                SwingUtilities.invokeLater(() -> {
                    gui.disconnected();
                });
            }
			@Override
			public void transferRequest(String fileName, long fileSize) {
                SwingUtilities.invokeLater(() -> {
                    gui.transferRequest(fileName, fileSize);
                });
			}
			@Override
			public void requestAccepted() {
                SwingUtilities.invokeLater(() -> {
                    gui.requestAccepted();
                });
			}
			@Override
			public void requestRejected() {
                SwingUtilities.invokeLater(() -> {
                    gui.requestRejected();
                });
			}
			@Override
			public void transferProgress(float percentComplete, long bytesPerSecond) {
                SwingUtilities.invokeLater(() -> {
                    gui.transferProgress(percentComplete, bytesPerSecond);
                });
			}
            @Override
            public void transferDone() {
                SwingUtilities.invokeLater(() -> {
                    gui.transferDone();
                });
            }
        });
    }
}
