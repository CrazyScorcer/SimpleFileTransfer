import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Gui
{
    private JFrame _frame;
    private JPanel _mainPanel;
    private WaitingScreen _waitingScreen;
    private SendScreen _sendScreen;
    
    private Popup _popup;

    private static String MAIN_MENU = "MM";
    private static String HOST_MENU = "HM";
    private static String CONNECT_MENU = "CM";
    private static String WAITING_SCREEN = "WS";
    private static String SEND_SCREEN = "SS";

    private GuiListener _listener = null;

    private JPanel createMainMenu()
    {
        JPanel mainMenu = new JPanel();
        mainMenu.setLayout(new BorderLayout(0, 10));
        mainMenu.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        JLabel title = new JLabel("SimpleFileTransfer");
        title.setHorizontalAlignment(JLabel.CENTER);

        JButton hostButton = new JButton("Host");
        hostButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                changeScreen(HOST_MENU);
            }
        });

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                changeScreen(CONNECT_MENU);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1));
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        ((GridLayout) buttons.getLayout()).setVgap(20);
        buttons.add(hostButton);
        buttons.add(connectButton);

        mainMenu.add(title, BorderLayout.PAGE_START);
        mainMenu.add(buttons, BorderLayout.CENTER);

        return mainMenu;
    }

    private JPanel createHostMenu()
    {
        JPanel hostMenu = new JPanel();
        hostMenu.setLayout(new BorderLayout(0, 10));
        hostMenu.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        
        JLabel title = new JLabel("Host Settings");
        title.setHorizontalAlignment(JLabel.CENTER);

        JPanel dataEntry = new JPanel(new GridBagLayout());
        dataEntry.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints constraints = new GridBagConstraints();

        JLabel ipLabel = new JLabel("IP Address:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipady = 20;
        dataEntry.add(ipLabel, constraints);
        constraints = new GridBagConstraints();

        JTextField ipField = new JTextField("127.0.0.1");
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.ipady = 20;
        constraints.weightx = 0.5;
        constraints.insets = new Insets(0, 10, 0, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        dataEntry.add(ipField, constraints);
        constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        dataEntry.add(Box.createRigidArea(new Dimension(0, 20)), constraints);
        constraints = new GridBagConstraints();

        JLabel portLabel = new JLabel("Port:");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipady = 20;
        dataEntry.add(portLabel, constraints);
        constraints = new GridBagConstraints();

        JTextField portField = new JTextField("54621");
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.ipady = 20;
        constraints.weightx = 0.5;
        constraints.insets = new Insets(0, 10, 0, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        dataEntry.add(portField, constraints);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                changeScreen(MAIN_MENU);
            }
        });

        JButton startButton = new JButton("Host");
        startButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event) {
                if (_listener == null || !startNetworking(ipField.getText(), portField.getText(), true))
                    return;

                String title = "Hosting at " + ipField.getText() + ":" + portField.getText();

                _waitingScreen.update(title, "Waiting for client", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        changeScreen(HOST_MENU);
                        _waitingScreen.stopLoading();
                        if (_listener != null)
                        _listener.cancel();
                    }
                });

                _waitingScreen.startLoading();
                changeScreen(WAITING_SCREEN);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        ((GridLayout) buttons.getLayout()).setHgap(20);
        buttons.add(backButton);
        buttons.add(startButton);

        hostMenu.add(title, BorderLayout.PAGE_START);
        hostMenu.add(dataEntry, BorderLayout.CENTER);
        hostMenu.add(buttons, BorderLayout.PAGE_END);

        return hostMenu;
    }

    private JPanel createConnectMenu()
    {
        JPanel connectMenu = new JPanel();
        connectMenu.setLayout(new BorderLayout(0, 10));
        connectMenu.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        
        JLabel title = new JLabel("Connection Settings");
        title.setHorizontalAlignment(JLabel.CENTER);

        JPanel dataEntry = new JPanel(new GridBagLayout());
        dataEntry.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints constraints = new GridBagConstraints();

        JLabel ipLabel = new JLabel("IP Address:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipady = 20;
        dataEntry.add(ipLabel, constraints);
        constraints = new GridBagConstraints();

        JTextField ipField = new JTextField("127.0.0.1");
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.ipady = 20;
        constraints.weightx = 0.5;
        constraints.insets = new Insets(0, 10, 0, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        dataEntry.add(ipField, constraints);
        constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        dataEntry.add(Box.createRigidArea(new Dimension(0, 20)), constraints);
        constraints = new GridBagConstraints();

        JLabel portLabel = new JLabel("Port:");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.ipady = 20;
        dataEntry.add(portLabel, constraints);
        constraints = new GridBagConstraints();

        JTextField portField = new JTextField("54621");
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.ipady = 20;
        constraints.weightx = 0.5;
        constraints.insets = new Insets(0, 10, 0, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        dataEntry.add(portField, constraints);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                changeScreen(MAIN_MENU);
            }
        });

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event) {
                if (_listener == null || !startNetworking(ipField.getText(), portField.getText(), false))
                    return;

                String title = "Connecting to " + ipField.getText() + ":" + portField.getText();

                _waitingScreen.update(title, "Attempting to connect", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        changeScreen(CONNECT_MENU);
                        _waitingScreen.stopLoading();
                        if (_listener != null)
                            _listener.cancel();
                    }
                });

                _waitingScreen.startLoading();
                changeScreen(WAITING_SCREEN);
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        ((GridLayout) buttons.getLayout()).setHgap(20);
        buttons.add(backButton);
        buttons.add(connectButton);

        connectMenu.add(title, BorderLayout.PAGE_START);
        connectMenu.add(dataEntry, BorderLayout.CENTER);
        connectMenu.add(buttons, BorderLayout.PAGE_END);

        return connectMenu;
    }

    private boolean startNetworking(String ipAddress, String portText, boolean isHosting)
    {
        try {
            int port = Integer.parseInt(portText);

            if (port < 0 || port > 65353)
                throw new NumberFormatException();

            if (isHosting) 
                _listener.host(ipAddress, port);
            else 
                _listener.connect(ipAddress, port);

            return true;
        } catch (NumberFormatException error) {
            notification("Invalid Port", "The specified port is invalid. A valid port must be a whole integer between 0 and 65353.", true);
            return false;
        }
    }

    private void changeScreen(String screen)
    {
        CardLayout layout = (CardLayout) _mainPanel.getLayout();
        layout.show(_mainPanel, screen);
    }

    private class WaitingScreen extends JPanel
    {
        private JLabel _titleLabel, _loadingLabel;
        private JButton _cancelButton;
        private Timer _timer;
        private String _loadingMessage = "";
        private short _dots = 0;

        public WaitingScreen()
        {
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
    
            _titleLabel = new JLabel();
            _titleLabel.setHorizontalAlignment(JLabel.CENTER);

            _loadingLabel = new JLabel();
            _loadingLabel.setHorizontalAlignment(JLabel.CENTER);

            _cancelButton = new JButton("Cancel");

            JPanel padding = new JPanel(new GridLayout(1, 0));
            padding.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            padding.add(_cancelButton);

            add(_titleLabel, BorderLayout.PAGE_START);
            add(_loadingLabel, BorderLayout.CENTER);
            add(padding, BorderLayout.PAGE_END);

            _timer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String message = _loadingMessage;
                    
                    for (int i = 0; i < _dots; i++)
                        message += ".";

                    _loadingLabel.setText(message);

                    _dots++;
                    _dots %= 4;
                }
            });

            _timer.setRepeats(true);
            _timer.setInitialDelay(0);
        }

        public void update(String title, String loadingMesasge, ActionListener listener)
        {
            _titleLabel.setText(title);
            _loadingMessage = loadingMesasge;
            
            for (ActionListener existingListener : _cancelButton.getActionListeners())
                _cancelButton.removeActionListener(existingListener);

            _cancelButton.addActionListener(listener);
        }

        public void startLoading()
        {
            _dots = 0;
            _timer.start();
        }

        public void stopLoading()
        {
            _timer.stop();
        }
    }

    private class SendScreen extends JPanel
    {
        private JLabel _connectedLabel;

        public SendScreen()
        {
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

            _connectedLabel = new JLabel();
            _connectedLabel.setHorizontalAlignment(JLabel.CENTER);

            JFileChooser filePicker = new JFileChooser();
            filePicker.setFileSelectionMode(JFileChooser.FILES_ONLY);
            filePicker.setDialogTitle("Pick a file to send");
            filePicker.setMultiSelectionEnabled(false);

            JLabel _selectedFileLabel = new JLabel("Selected File: No file selected");
            _selectedFileLabel.setHorizontalAlignment(JLabel.CENTER);
            _selectedFileLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            JButton _browseButton = new JButton("Pick a file");
            _browseButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

            JPanel middlePanel = new JPanel();
            middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.PAGE_AXIS));
            middlePanel.add(Box.createVerticalGlue());
            middlePanel.add(_browseButton);
            middlePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            middlePanel.add(_selectedFileLabel);
            middlePanel.add(Box.createVerticalGlue());

            JButton disconnectButton = new JButton("Disconnect");
            disconnectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeScreen(MAIN_MENU);
                    if (_listener != null)
                        _listener.cancel();
                }
            });

            JButton sendButton = new JButton("Send");
            sendButton.setEnabled(false);
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        if (_listener != null)
                            _listener.sendFile(filePicker.getSelectedFile());
                    });

                    _popup.showFileSendDialog(filePicker.getSelectedFile().getName());
                }
            });

            _browseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (filePicker.showOpenDialog(_mainPanel) == JFileChooser.APPROVE_OPTION) {
                        _selectedFileLabel.setText("Selected File: " + filePicker.getSelectedFile().getName());
                        sendButton.setEnabled(true);
                    }
                }
            });

            JPanel buttons = new JPanel(new GridLayout(1, 0));
            buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            ((GridLayout) buttons.getLayout()).setHgap(20);
            buttons.add(disconnectButton);
            buttons.add(sendButton);

            add(_connectedLabel, BorderLayout.PAGE_START);
            add(middlePanel, BorderLayout.CENTER);
            add(buttons, BorderLayout.PAGE_END);
        }

        public void updateIp(String ipAddress)
        {
            _connectedLabel.setText("Connected to " + ipAddress);
        }
    }

    private class Popup extends JDialog
    {
        private JOptionPane _receiveFilePane;
        private JFileChooser _receiveFilePicker;
        
        private String _fileName;
        private JPanel _sendFilePane;
        private JLabel _sendFileLabel;
        private Component _sendFileCloseButtonSpacing;
        private JButton _sendFileCloseButton;

        private JPanel _transferPanel;
        private JLabel _transferDirectionLabel;
        private JProgressBar _transferProgressBar;
        private JLabel _transferSpeedLabel;
        private Component _transferCloseButtonSpacing;
        private JButton _transferCloseButton;

        private boolean _allowClosing = false;

        public Popup()
        {
            super(_frame, true);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            _receiveFilePicker = new JFileChooser();
            _receiveFilePicker.setDialogTitle("Choose where to save the file");

            // Setup the receive file dialog
            _receiveFilePane = new JOptionPane();
            _receiveFilePane.setMessageType(JOptionPane.QUESTION_MESSAGE);
            _receiveFilePane.setOptionType(JOptionPane.YES_NO_OPTION);
            _receiveFilePane.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    String prop = e.getPropertyName();
        
                    if (isVisible() && e.getSource() == _receiveFilePane
                                    && prop.equals(JOptionPane.VALUE_PROPERTY)
                                    && _receiveFilePane.getValue() != null)
                    {
                        // Check the selected option
                        final int choice = ((Integer) _receiveFilePane.getValue()).intValue();
                        if (choice != JOptionPane.YES_OPTION) {
                            rejectRequest();
                            return;
                        }

                        // Ask where to save the file
                        if (_receiveFilePicker.showSaveDialog(Popup.this) != JFileChooser.APPROVE_OPTION) {
                            rejectRequest();
                            return;
                        }

                        // Accept the transfer request
                        if (_listener != null)
                            _listener.acceptRequest(_receiveFilePicker.getSelectedFile());

                        showTransferPanel("Downloading '" + _receiveFilePicker.getSelectedFile().getName() + "'");
                    }
                }
            });

            // Construct the send message panel
            _sendFileLabel = new JLabel();
            _sendFileLabel.setHorizontalAlignment(JLabel.CENTER);
            _sendFileLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            _sendFileCloseButtonSpacing = Box.createRigidArea(new Dimension(0, 10));

            _sendFileCloseButton = new JButton("Close");
            _sendFileCloseButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
            _sendFileCloseButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            _sendFilePane = new JPanel();
            _sendFilePane.setLayout(new BoxLayout(_sendFilePane, BoxLayout.PAGE_AXIS));
            _sendFilePane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            _sendFilePane.add(Box.createVerticalGlue());
            _sendFilePane.add(_sendFileLabel);
            _sendFilePane.add(_sendFileCloseButtonSpacing);
            _sendFilePane.add(_sendFileCloseButton);
            _sendFilePane.add(Box.createVerticalGlue());

            // Construct the transfer panel
            _transferDirectionLabel = new JLabel();
            _transferDirectionLabel.setHorizontalAlignment(JLabel.CENTER);
            _transferDirectionLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            _transferProgressBar = new JProgressBar();
            _transferProgressBar.setStringPainted(true);

            _transferSpeedLabel = new JLabel();
            _transferSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
            _transferSpeedLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

            _transferCloseButtonSpacing = Box.createRigidArea(new Dimension(0, 10));
            _transferCloseButton = new JButton("Close");
            _transferCloseButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
            _transferCloseButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            _transferPanel = new JPanel();
            _transferPanel.setLayout(new BoxLayout(_transferPanel, BoxLayout.PAGE_AXIS));
            _transferPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
            _transferPanel.add(Box.createVerticalGlue());
            _transferPanel.add(_transferDirectionLabel);
            _transferPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            _transferPanel.add(_transferProgressBar);
            _transferPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            _transferPanel.add(_transferSpeedLabel);
            _transferPanel.add(_transferCloseButtonSpacing);
            _transferPanel.add(_transferCloseButton);
            _transferPanel.add(Box.createVerticalGlue());

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (getContentPane() == _receiveFilePane) {
                        // Set default option when the exit button is pressed
                        _receiveFilePane.setValue(JOptionPane.NO_OPTION);
                    }

                    // Close the window
                    if (_allowClosing)
                        setVisible(false);
                }
            });
        }

        public void showFileReceiveDialog(String fileName, long fileSize)
        {
            setTitle("File Transfer Request");
            setContentPane(_receiveFilePane);
            _allowClosing = false;
            _receiveFilePane.setValue(null);
            _receiveFilePane.setMessage("Receive file '" + fileName + "' with size " + bytesToReadable(fileSize, false) + "?");
            _receiveFilePicker.setSelectedFile(new File(fileName));

            pack();
            setLocationRelativeTo(getOwner());
            setVisible(true);
        }

        public void showFileSendDialog(String fileName)
        {
            _fileName = fileName;

            setTitle("Waiting for Peer");
            setContentPane(_sendFilePane);
            _allowClosing = false;

            _sendFileLabel.setText("Waiting for peer to accept the file...");
            _sendFileCloseButtonSpacing.setVisible(false);
            _sendFileCloseButton.setVisible(false);

            pack();
            setLocationRelativeTo(getOwner());
            setVisible(true);
        }

        public void requestAccepted()
        {
            showTransferPanel("Uploading '" + _fileName + "'");
        }

        public void requestRejected()
        {
            _allowClosing = true;
            _sendFileLabel.setText("The file transfer request was rejected!");
            _sendFileCloseButtonSpacing.setVisible(true);
            _sendFileCloseButton.setVisible(true);

            pack();
        }

        public void transferProgress(float percentComplete, long bytesPerSecond)
        {
            _transferProgressBar.setValue(Math.round(percentComplete * 100));
            _transferSpeedLabel.setText("Transfer Speed: " + bytesToReadable(bytesPerSecond, true));
        }

        public void transferDone()
        {
            _allowClosing = true;
            _transferProgressBar.setValue(100);
            _transferSpeedLabel.setText("Transfer Complete!");

            _transferCloseButtonSpacing.setVisible(true);
            _transferCloseButton.setVisible(true);
            pack();
        }

        public void close()
        {
            _receiveFilePicker.cancelSelection();
            setVisible(false);
        }

        private void showTransferPanel(String messasge)
        {
            setTitle(messasge);
            setContentPane(_transferPanel);

            _transferDirectionLabel.setText(messasge);
            _transferProgressBar.setValue(0);
            _transferSpeedLabel.setText("Transfer Speed: Unknown");

            _transferCloseButtonSpacing.setVisible(false);
            _transferCloseButton.setVisible(false);

            pack();
            setLocationRelativeTo(getOwner());
        }

        private void rejectRequest()
        {
            if (_listener != null)
                _listener.rejectRequest();

            setVisible(false);
        }
    }

    public void setListener(GuiListener listener)
    {
        _listener = listener;
    }

    public Gui()
    {
        _frame = new JFrame("SimpleFileTransfer");

        _frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        _frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
                e.getWindow().setVisible(false);
                if (_listener != null)
                    _listener.cancel();
                
                // Give the network code a small chance to actually exit
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
                System.exit(0);
            }
        });

        _waitingScreen = new WaitingScreen();
        _sendScreen = new SendScreen();
        _popup = new Popup();

        _mainPanel = new JPanel(new CardLayout());

        _mainPanel.add(createMainMenu(), MAIN_MENU);
        _mainPanel.add(createHostMenu(), HOST_MENU);
        _mainPanel.add(createConnectMenu(), CONNECT_MENU);
        _mainPanel.add(_waitingScreen, WAITING_SCREEN);
        _mainPanel.add(_sendScreen, SEND_SCREEN);

        _frame.setContentPane(_mainPanel);
        _frame.setSize(300, 300);
        _frame.setVisible(true);
    }

    public void connected(String ipAddress)
    {
        _sendScreen.updateIp(ipAddress);
        changeScreen(SEND_SCREEN);
    }

    public void disconnected()
    {
        changeScreen(MAIN_MENU);

        _popup.close();

        // Display the disconnected notification
        notification("Disconnected", "The conenction has been closed", false);
    }

    public void notification(String title, String message, boolean isError)
    {
        JOptionPane.showMessageDialog(_mainPanel, message, title, 
            isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    public void transferRequest(String fileName, long fileSize)
    {
        _popup.showFileReceiveDialog(fileName, fileSize);
    }

    public void requestAccepted()
    {
        _popup.requestAccepted();
    }

    public void requestRejected()
    {
        _popup.requestRejected();
    }

    public void transferProgress(float percentComplete, long bytesPerSecond)
    {
        _popup.transferProgress(percentComplete, bytesPerSecond);
    }

    public void transferDone()
    {
        _popup.transferDone();
    }

    private String bytesToReadable(long bytes, boolean isPerSecond)
    {
        double readbleBytes;
        String units;

        if (bytes < 1000) {
            // Use bytes as the unit
            readbleBytes = bytes;
            units = "B";
        } else if (bytes < 1000000) {
            // Use kilobytes as the unit
            readbleBytes = bytes / 1000.0;
            units = "KB";
        } else if (bytes < 1000000000) {
            // Use megabytes as the unit
            readbleBytes = bytes / 1000000.0;
            units = "MB";
        } else {
            // Use gigabytes as the unit
            readbleBytes = bytes / 1000000000.0;
            units = "GB";
        }

        if (isPerSecond)
            units += "/s";

        return String.format("%.2f %s", readbleBytes, units);
    }
}
