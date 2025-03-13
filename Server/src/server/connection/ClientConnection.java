package server.connection;

import server.features.CameraServer;
import server.features.FileManagerUI;
import server.features.KeyboardServer;
import server.features.ScreenSharingServer;
import server.utils.RSAUtil;
import server.utils.ID;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import static client.ClientMain.clientConnection;

public class ClientConnection {
    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;
    private String clientName;
    private String clientIP;
    private String clientSystemVersion;
    private String clientPublicKey;
    private final RSAUtil rsaUtil = new RSAUtil();
    public ScreenSharingServer screenSharingServer;
    public CameraServer cameraServer;
    public KeyboardServer keyboardServer;
    private static final String AES_ALGORITHM = "AES";
    private SecretKey aesKey;
    private final Cipher deCipher = Cipher.getInstance(AES_ALGORITHM);
    private final Cipher enCipher = Cipher.getInstance(AES_ALGORITHM);
    public FileManagerUI fileManagerUI;
    private HandleOutbound handleOutbound;

    public ClientConnection(Socket socket) throws Exception {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        // Perform initial handshake
        rsaUtil.getKeyPair();
        generateAESKey();
        performInitialHandshake();

        // Initialize HandleOutbound after handshake
        handleOutbound = new HandleOutbound(out);
        new Thread(handleOutbound).start();
        screenSharingServer = new ScreenSharingServer(this);
        cameraServer = new CameraServer(this);
        keyboardServer = new KeyboardServer(this);
        fileManagerUI = new FileManagerUI(this);  // 取消注释：初始化 FileManagerUI
    }

    // 发送原始数据，不进行加密
    public void sendRawData(byte[] data) throws Exception {
        Packet packet = new Packet();
        packet.sendData(data);
        handleOutbound.addPacket(packet);
    }

    // 接收原始数据，不进行解密
    public byte[] receiveRawData() throws Exception {
        int dataSize = receiveHead();
        if (dataSize <= 0) {
            throw new IOException("Invalid data size received: " + dataSize);
        }
        byte[] rawData = new byte[dataSize];
        int bytesRead = 0;
        while (bytesRead < dataSize) {
            int count = in.read(rawData, bytesRead, dataSize - bytesRead);
            if (count < 0) {
                throw new EOFException("Unexpected end of stream");
            }
            bytesRead += count;
        }
        return rawData;
    }


    private void performInitialHandshake() throws Exception {
        sendPublicKey();
        receivePublicKey();
        sendAesKey();
        receiveSystemInfo();
    }

    private void receiveSystemInfo() throws Exception {
        if(in.readInt() == -1){
            clientSystemVersion = rsaUtil.decryptStr(in.readUTF(), rsaUtil.keyMap.get(1));
            clientName = rsaUtil.decryptStr(in.readUTF(), rsaUtil.keyMap.get(1));
            clientIP = socket.getInetAddress().getHostAddress();
        }
    }

    private void sendPublicKey() throws IOException {
        out.writeInt(-10);
        out.writeUTF(rsaUtil.keyMap.get(0));
        out.flush();
    }

    private void receivePublicKey() throws IOException {
        if(in.readInt() == -10)
            clientPublicKey = in.readUTF();
    }

    private void sendAesKey() throws Exception {
        enCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        deCipher.init(Cipher.DECRYPT_MODE, aesKey);

        out.writeInt(ID.AES());
        String encodedAesKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        String encryptedAesKey = rsaUtil.encryptStr(encodedAesKey, clientPublicKey);
        out.writeUTF(encryptedAesKey);
        out.flush();
    }

    public void generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        aesKey = keyGenerator.generateKey();
    }

    public String receiveMessage() throws Exception {
        return new String(receiveData());
    }

    public synchronized void sendMessage(int head, String message) throws Exception {
        Packet packet = new Packet();
        // 写入头部
        packet.sendHead(head);
        // 对消息进行加密，并写入数据体（头部与数据体在同一数据包中）
        byte[] encryptedData = enCipher.doFinal((message.isEmpty() ? "None" : message).getBytes());
        packet.sendData(encryptedData);
        // 将完整数据包加入发送队列
        handleOutbound.addPacket(packet);
    }


    public void sendHead(int head) throws Exception {
        Packet packet = new Packet();
        packet.sendHead(head);
        handleOutbound.addPacket(packet);
    }

    public int receiveHead() throws Exception {
        return in.readInt();
    }

    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    public void sendFile(File file) throws Exception {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File does not exist or is not a regular file");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            Packet sizePacket = new Packet();
            sizePacket.sendHead(ID.FILE_SIZE());
            sizePacket.sendLong(fileSize);
            handleOutbound.addPacket(sizePacket);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                Packet dataPacket = new Packet();
                dataPacket.sendData(Arrays.copyOf(buffer, bytesRead));
                handleOutbound.addPacket(dataPacket);
            }
        }
    }

    public void receiveFile(File file) throws Exception {
        if (receiveHead() != ID.FILE_SIZE()) {
            throw new IOException("Expected file size header not received");
        }
        long fileSize = in.readLong();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            long remainingBytes = fileSize;
            while (remainingBytes > 0) {
                byte[] data = receiveRawData();  // 使用原始数据传输
                fos.write(data);
                remainingBytes -= data.length;
            }
        }
    }


    public void sendData(byte[] data) throws Exception {
        byte[] encryptedData = enCipher.doFinal(data);
        Packet packet = new Packet();
        packet.sendData(encryptedData);
        handleOutbound.addPacket(packet);
    }

    public byte[] receiveData() throws Exception {
        int dataSize = receiveHead();
        if (dataSize <= 0) {
            throw new IOException("Invalid data size received: " + dataSize);
        }
        byte[] encryptedData = new byte[dataSize];
        int bytesRead = 0;
        while (bytesRead < dataSize) {
            int count = in.read(encryptedData, bytesRead, dataSize - bytesRead);
            if (count < 0) {
                throw new EOFException("Unexpected end of stream");
            }
            bytesRead += count;
        }
        return deCipher.doFinal(encryptedData);
    }

    public void sendCommand(String command) throws Exception {
        sendMessage(ID.COMMAND(), "command:" + command);
    }

    public void close() {
        try {
            handleOutbound.stop();
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter methods remain unchanged
    public String getName() {
        return clientName;
    }

    public String getSystemVersion() {
        return clientSystemVersion;
    }

    public Socket getSocket(){
        return socket;
    }

    public String getIP() {
        return clientIP;
    }
}