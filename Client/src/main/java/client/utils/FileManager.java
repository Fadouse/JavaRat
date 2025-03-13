package client.utils;

import client.connection.ClientConnection;
import client.connection.ID;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private final ClientConnection clientConnection;
    // 用于记录待决策的上传目标
    private String pendingUploadPath;
    private boolean isWaitingForDecision = false;

    public FileManager(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;

    }

    public void listDirectory(String path) throws Exception {
        Path dir = Paths.get(path);
        if (!Files.exists(dir)) {
            clientConnection.sendMessage(ID.MESSAGE(), "File not exists");
            return;
        }
        List<String> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (Files.isDirectory(file)) {
                    fileName += "/";
                }
                fileList.add(fileName);
            }
        }
        clientConnection.sendMessage(ID.FILE_DIR(), String.join("\n", fileList));
    }

    public void handleFileExists(String filePath) throws Exception {
        pendingUploadPath = filePath;
        isWaitingForDecision = true;
        clientConnection.sendMessage(ID.FILE_EXISTS(), filePath);
    }

    public void handleUploadDecision(String decision) throws Exception {
        // 当没有 pending 状态时，仅记录日志并忽略决策
        if (!isWaitingForDecision || pendingUploadPath == null) {
            System.out.println("No pending upload or not waiting for decision. Received decision: " + decision);
            return;
        }

        Path path = Paths.get(pendingUploadPath);
        if (decision.equals("REPLACE")) {
            Files.deleteIfExists(path);
        } else if (decision.equals("RENAME")) {
            path = findNextAvailableName(path);
        } else {
            // 如果选择 CANCEL，则取消上传
            pendingUploadPath = null;
            isWaitingForDecision = false;
            return;
        }
        pendingUploadPath = path.toString();
        isWaitingForDecision = false;
    }

    public void uploadFile(String filePath, DataInputStream in) throws Exception {
        Path path = Paths.get(filePath);

        if (isWaitingForDecision) {
            throw new Exception("Waiting for upload decision");
        }

        // 如果文件已存在且不在待决策状态，则发起文件存在处理流程
        if (Files.exists(path) && !filePath.equals(pendingUploadPath)) {
            handleFileExists(filePath);
            return;
        }

        // 首先接收文件大小包（未加密）
        int header = clientConnection.receiveHead();
        if (header != ID.FILE_SIZE()) {
            throw new IOException("Expected FILE_SIZE header, got: " + header);
        }
        long fileSize = in.readLong();

        try (OutputStream out = Files.newOutputStream(path)) {
            long remainingBytes = fileSize;
            while (remainingBytes > 0) {
                // 使用 receiveRawData() 接收文件数据块
                byte[] data = clientConnection.receiveRawData();
                out.write(data);
                remainingBytes -= data.length;
            }
        }

        pendingUploadPath = null;
        clientConnection.sendMessage(ID.FILE_UPLOAD_SUCCESS(), "File uploaded successfully");
    }

    private Path findNextAvailableName(Path original) {
        Path parent = original.getParent();
        String fileName = original.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        int counter = 1;
        Path newPath = original;
        while (Files.exists(newPath)) {
            newPath = parent.resolve(baseName + " (" + counter + ")" + extension);
            counter++;
        }
        return newPath;
    }

    public void downloadFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            clientConnection.sendMessage(ID.MESSAGE(), "File does not exist or is a directory");
            return;
        }
        clientConnection.sendHead(ID.FILE_OUT());
        clientConnection.sendFile(path.toFile());
    }

    public void deleteFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            clientConnection.sendMessage(ID.MESSAGE(), "File or directory does not exist");
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        clientConnection.sendMessage(ID.MESSAGE(), "File or directory deleted successfully");
    }
}
