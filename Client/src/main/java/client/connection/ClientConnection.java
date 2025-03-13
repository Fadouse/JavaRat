package client.connection;

import client.features.CameraCapturer;
import client.features.ScreenSharer;
import client.utils.RSAUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public class ClientConnection {
    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;
    private final RSAUtil rsaUtil = new RSAUtil();
    private final String clientName;
    private final String clientSystemVersion;
    private String serverPublicKey;
    public ScreenSharer screenSharer;
    public CameraCapturer cameraSharingClient;
    private HandleOutbound handleOutbound;

    private static final String AES_ALGORITHM = "AES";
    private final Cipher enCipher = Cipher.getInstance(AES_ALGORITHM);
    private final Cipher deCipher = Cipher.getInstance(AES_ALGORITHM);

    private final String serverAddress;
    private final int serverPort;

    // 修改构造器，保存地址和端口
    public ClientConnection(String serverAddress, int serverPort, String clientSystemVersion, String clientName) throws Exception {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientSystemVersion = clientSystemVersion;
        this.clientName = clientName;
        socket = new Socket(serverAddress, serverPort);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        // Perform initial handshake
        rsaUtil.getKeyPair();
        performInitialHandshake();

        // Initialize HandleOutbound after handshake
        handleOutbound = new HandleOutbound(out);
        new Thread(handleOutbound).start();
        screenSharer = new ScreenSharer(this);
        cameraSharingClient = new CameraCapturer(this);


    }

    public synchronized void reconnect() {
        close();  // 先关闭旧连接
        while (true) {
            try {
                System.out.println("尝试重新连接服务器...");
                socket = new Socket(serverAddress, serverPort);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                // 重新执行握手
                performInitialHandshake();

                // 重置并启动发送队列线程
                handleOutbound = new HandleOutbound(out);
                new Thread(handleOutbound).start();

                // 若有接收线程，也需重新启动（依据实际项目结构调整）
                System.out.println("重新连接成功。");
                break;
            } catch (Exception ex) {
                System.err.println("重新连接失败，30秒后重试...");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void performInitialHandshake() throws Exception {
        receivePublicKey();
        sendPublicKey();
        receiveAesKey();
        sendSystemInfo();
    }

    private void sendSystemInfo() throws Exception {
        out.writeInt(-1);
        out.writeUTF(rsaUtil.encryptStr(clientSystemVersion, serverPublicKey));
        out.writeUTF(rsaUtil.encryptStr(clientName, serverPublicKey));
        out.flush();
    }

    private void sendPublicKey() throws IOException {
        out.writeInt(-10);
        out.writeUTF(rsaUtil.keyMap.get(0));
        out.flush();
    }

    private void receivePublicKey() throws IOException {
        if(in.readInt() == -10)
            serverPublicKey = in.readUTF();
    }

    private void receiveAesKey() throws Exception {
        if(in.readInt() == ID.AES()) {
            String encryptedAesKey = in.readUTF();
            String decryptedAesKey = rsaUtil.decryptStr(encryptedAesKey, rsaUtil.keyMap.get(1));
            byte[] aesKeyBytes = Base64.getDecoder().decode(decryptedAesKey);
            SecretKey serverAesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            deCipher.init(Cipher.DECRYPT_MODE, serverAesKey);
            enCipher.init(Cipher.ENCRYPT_MODE, serverAesKey);
        }
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


    public synchronized void sendHead(int head) throws Exception {
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
            out.writeLong(fileSize);
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

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
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


    public void close() {
        try {
            if(screenSharer != null)
                screenSharer.stop();
            screenSharer = null;
            if(cameraSharingClient != null)
                cameraSharingClient.stop();
            cameraSharingClient = null;
            handleOutbound.stop();
            in.close();
            out.close();
            socket.close();
        } catch (IOException ignored) {
        }
    }
}