import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class FileTransfer 
{
    private Thread _readThread;
    private Thread _writeThread;

    private volatile ServerSocket _server = null;
    private volatile Socket _socket = null;
    private volatile InputStream _is = null;
    private volatile OutputStream _os = null;

    private volatile State _state = State.Idle;

    private volatile File _file;
    private volatile long _fileSize = 0;
    
    private volatile FileInputStream _fis = null;
    private volatile FileOutputStream _fos = null;
    private long _remainingFileSize = 0;

    private long _remainingFileSizeOld = -1;
    private long _timestamp = -1;

    private FileTransferListener _listener = null;

    private enum State {
        Idle, Replying, WaitingForResponse, Sending, Receiving
    }

    public void setListener(FileTransferListener listener)
    {
        _listener = listener;
    }

    public void host(String ipAddress, int port)
    {
        // Start the server in the read thread
        _readThread = new Thread(() -> {
            InetSocketAddress address = new InetSocketAddress(ipAddress, port);
            try {
                _server = new ServerSocket();
                _server.bind(address);

                Socket socket = _server.accept();
                gotSocket(socket);
            } catch (SocketException e) {
                // Server was closed before a connection was established
            } catch (Exception e) {
                // Deal with the errors
                e.printStackTrace();
            }
        });
        _readThread.start();
    }

    public void connect(String ipAddress, int port)
    {
        _readThread = new Thread(() -> {
            try {
                InetSocketAddress address = new InetSocketAddress(ipAddress, port);

                // Keep trying to connect over and over
                Socket socket = null;
                while (socket == null) {
                    try {
                        socket = new Socket();
                        socket.connect(address);
                    }  catch (ConnectException e) {
                        // Try try again :)
                        Thread.sleep(3000);
                        socket.close();
                        socket = null;
                    }
                }

                gotSocket(socket);

            } catch (Exception e) {
                // Deal with errors
                e.printStackTrace();
            }
        });
        _readThread.start();
    }

    private void gotSocket(Socket socket)
    {
        try {
            _socket = socket;
            _is = _socket.getInputStream();
            _os = _socket.getOutputStream();

            // Callback for socket connected
            if (_listener != null)
                _listener.connected(_socket.getInetAddress().getHostAddress());

            // Establish the buffer for network reads
            byte[] buffer = new byte[4096];
            int fillMark = 0;
            int processMark = 0;
            int fileNameLength = -1;
            while (true) {
                //Thread.sleep(1);
                int bytesRead = _is.read(buffer, fillMark, buffer.length - fillMark);

                // Check for End of File
                if (bytesRead == -1)
                    throw new EOFException();

                fillMark += bytesRead;

                // Loop while there is data to process
                while (fillMark > 0) {
                    if (_state == State.Idle && buffer[0] == 0) {
                        // Keep reading if we have not received at least the first 5 bytes of the message
                        if (fillMark < 5)
                            break;
    
                        // Extract the file name length from the bytes 1-4
                        if (fileNameLength == -1)
                            fileNameLength = ByteBuffer.wrap(buffer, 1, 4).getInt();
                    
                        // Keep reading if we have not received the entire rest of the message
                        if (fillMark < 5 + fileNameLength + 8)
                            break;
    
                        // Extract the file name
                        String fileName = new String(buffer, 5, fileNameLength, "UTF-8");
    
                        // Extract the size of the file
                        _fileSize = ByteBuffer.wrap(buffer, 5 + fileNameLength, 8).getLong();
    
                        if (_listener != null)
                            _listener.transferRequest(fileName, _fileSize);
    
                        // Wait for reply before doing stuff with new data
                        _state = State.Replying;
    
                        // Mark the message as finished and reset variables
                        processMark += 5 + fileNameLength + 8;
                        fileNameLength = -1;
                    } else if (_state == State.WaitingForResponse) {
                        if (buffer[0] == 1) {
                            // Our request was accepted
    
                            if (_listener != null)
                                _listener.requestAccepted();
    
                            // Change to the sending state and start sending
                            _state = State.Sending;
                            sendFile();

                            // Mark the message as finished by setting the process mark 1 byte forward
                            processMark++;
                        } else if (buffer[0] == 2) {
                            // Our request was rejected
    
                            if (_listener != null)
                                _listener.requestRejected();
    
                            // Go back to the idle state, since the request was rejected
                            _state = State.Idle;
    
                            // Mark the message as finished by setting the process mark 1 byte forward
                            processMark++;
                        }
                    } else if (_state == State.Replying || _state == State.Sending) {
                        // No messages shall be processed while in the replying state
                        // But we shall continue reading so that we may detect EOF
                        processMark = fillMark;
                    } else if (_state == State.Receiving) {
                        // Open the file output stream if it was not already open
                        if (_fos == null) {
                            _fos = new FileOutputStream(_file);
                            _remainingFileSize = _fileSize;
                            _remainingFileSizeOld = -1;
                            _timestamp = -1;
                        }
    
                        // Write the data from the buffer to the disk
                        final int length = Math.toIntExact(Math.min(_remainingFileSize, fillMark));
                        _fos.write(buffer, 0, length);
                        _remainingFileSize -= length;

                        runStats();

                        // If we have written enough bytes, finish the file
                        if (_remainingFileSize <= 0) {
                            _fos.close();
                            _fos = null;

                            // Go back to the idle state
                            _state = State.Idle;

                            if (_listener != null)
                                _listener.transferDone();
                        }

                        processMark = length;
                    }

                    // Reset the position since we are done with this message
                    if (processMark > 0) {
                        if (fillMark > processMark)
                            System.arraycopy(buffer, processMark, buffer, 0, fillMark - processMark);
                        fillMark -= processMark;
                        processMark = 0;
                    }
                }
            }
        } catch (EOFException e) {
            // Deal with end of file
            if (_listener != null)
                _listener.disconnected();
            disconnect();
            
        } catch (SocketException e) {
            // Socket closed 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile()
    {
        _writeThread = new Thread(() -> {
            try {
                if (_fis == null)
                    _fis = new FileInputStream(_file);

                _remainingFileSize = _fileSize;
                _remainingFileSizeOld = -1;
                _timestamp = -1;

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = _fis.read(buffer)) != -1) {
                    _remainingFileSize -= bytesRead;
                    
                    _os.write(buffer, 0, bytesRead);
                    _os.flush();

                    runStats();
                }

                _fis.close();
                _fis = null;

                if (_listener != null)
                    _listener.transferDone();

                // Go back to the idle state
                _state = State.Idle;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        _writeThread.start();
    }

    private void runStats()
    {
        // Use the past timestamp and file size to compute transfer speed and percent completion
        boolean updateOldInfo = false;
        if (_timestamp != -1 && _remainingFileSizeOld != -1 && _listener != null) {
            double timeDelta = (System.nanoTime() - _timestamp) / (double) 1000000000;

            if (timeDelta > 0.1) {
                long fileSizeDelta = _remainingFileSizeOld - _remainingFileSize;
    
                long bytesPerSecond = Math.round(fileSizeDelta / timeDelta);
                float percentComplete = (_fileSize - _remainingFileSize) / (float)_fileSize;
    
                _listener.transferProgress(percentComplete, bytesPerSecond);
    
                updateOldInfo = true;
            }
        } else {
            updateOldInfo = true;
        }

        if (updateOldInfo) {
            // Update the time and file size
            _timestamp = System.nanoTime();
            _remainingFileSizeOld = _remainingFileSize;
        }
    }

    public void disconnect()
    {
        try {
            if (_server != null)
                _server.close();
        } catch (Exception e) {

        }
        _server = null;

        try {
            if (_socket != null)
                _socket.close();
        } catch (Exception e) {
            
        }
        _socket = null;
        _is = null;
        _os = null;
        _fis = null;
        _fos = null;

        // Reset to idle state
        _state = State.Idle;
    }

    public void startTransfer(File file)
    {
        _file = file;
        _state = State.WaitingForResponse;
        _writeThread = new Thread(() -> {
            // Grab file information
            byte[] fileName;
            try {
                fileName = file.getName().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }
            _fileSize = file.length();

            // Pack into a nice format before we send it out on the network
            ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.BYTES + fileName.length + Long.BYTES);
            buffer.put((byte)0); // First byte is 0, indicating transfer
            buffer.putInt(fileName.length); // The length of the file name
            buffer.put(fileName); // The file name data
            buffer.putLong(_fileSize); // The size of the file
            byte[] data = buffer.array();

            // Send out the data
            try {
                _os.write(data);
                _os.flush();
            } catch (SocketException e) {
                disconnect();
            } catch (IOException e) {
                // Deal with errors
                e.printStackTrace();
            }
        });
        _writeThread.start();
    }

    public void replyToTransferRequest(boolean accept)
    {
        _writeThread = new Thread(() -> {
            _state = accept ? State.Receiving : State.Idle;

            // Send out the data
            try {
                _os.write(accept ? (byte)1 : (byte)2);
                _os.flush();
            } catch (IOException e) {
                // Deal with errors
                e.printStackTrace();
            }
        });
        _writeThread.start();
    }

    public void setSaveLocation(File saveLocation)
    {
        _file = saveLocation;
    }
}
