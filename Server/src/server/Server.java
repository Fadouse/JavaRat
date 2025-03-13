package server;

import server.connection.ClientConnection;
import server.connection.ClientRemoteUI;
import server.connection.CheckClientAlive;
import server.connection.HandleInbound;
import server.utils.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final String SERVER_VERSION = "1.6 RSA&AES";
    private static final String SERVER_AUTHOR = "Fadouse";
    private static final int CLIENTS_PER_PAGE = 10;
    private static final int DEFAULT_PORT = 2035;

    public final Map<ClientConnection, ClientRemoteUI> clientToUi = new HashMap<>();
    private final List<ClientConnection> connectedClients = new CopyOnWriteArrayList<>();

    private JFrame serverFrame;
    private JPanel mainPanel;
    private JPanel clientsPanel;
    private JLabel statusLabel;
    private int serverPort = DEFAULT_PORT;

    public Server() {
        SwingUtilities.invokeLater(this::initUI);
    }

    private void initUI() {
        serverFrame = new JFrame("Server Control Panel");
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setSize(800, 600);
        serverFrame.setLayout(new BorderLayout());

        createMenuBar();
        createMainPanel();
        createStatusBar();

        serverFrame.setVisible(true);
        centerWindow(serverFrame);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu aboutMenu = createAboutMenu();
        JMenu settingsMenu = createSettingsMenu();
        JMenu clientsMenu = new JMenu("Connected Clients");

        menuBar.add(aboutMenu);
        menuBar.add(settingsMenu);
        menuBar.add(clientsMenu);

        serverFrame.setJMenuBar(menuBar);
    }

    private JMenu createAboutMenu() {
        JMenu aboutMenu = new JMenu("About");
        aboutMenu.add(new JMenuItem("Server Version: " + SERVER_VERSION));
        aboutMenu.add(new JMenuItem("Server Author: " + SERVER_AUTHOR));
        return aboutMenu;
    }

    private JMenu createSettingsMenu() {
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem portMenuItem = new JMenuItem("Change Port");
        portMenuItem.addActionListener(e -> changePort());
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        settingsMenu.add(portMenuItem);
        settingsMenu.add(exitMenuItem);
        return settingsMenu;
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());

        createClientsPanel();

        serverFrame.add(mainPanel, BorderLayout.CENTER);
    }

    private void createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Connected Clients"));

        clientsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        JScrollPane scrollPane = new JScrollPane(clientsPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(panel, BorderLayout.CENTER);
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Server started on port " + serverPort);
        statusBar.add(statusLabel);
        serverFrame.add(statusBar, BorderLayout.SOUTH);
    }

    private void changePort() {
        String input = JOptionPane.showInputDialog(serverFrame, "Enter new port:", "Change Port", JOptionPane.QUESTION_MESSAGE);
        try {
            int newPort = Integer.parseInt(input);
            if (newPort > 0 && newPort < 65536) {
                serverPort = newPort;
                updateStatusLabel("Port changed to " + serverPort + ". Restart server to apply changes.");
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(serverFrame, "Invalid port number! Please enter a number between 1 and 65535.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                updateStatusLabel("Server listening on port " + serverPort);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientConnection(clientSocket);
                }
            } catch (IOException e) {
                updateStatusLabel("Server error: " + e.getMessage());
                JOptionPane.showMessageDialog(serverFrame, "Error with ServerSocket: " + e.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void handleClientConnection(Socket clientSocket) {
        try {
            ClientConnection clientConnection = new ClientConnection(clientSocket);
            connectedClients.add(clientConnection);
            SwingUtilities.invokeLater(this::updateConnectedClientsList);
            System.out.println("Client connected (" + clientConnection.getIP() + ")");
            NotificationUtil.showNotification("New Client Connected", "IP: " + clientConnection.getIP());
            new HandleInbound(clientConnection, this).start();
            new CheckClientAlive(clientConnection, this).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteClient(ClientConnection clientConnection) {
        System.out.println("Connection error (" + clientConnection.getIP() + ")");
        SwingUtilities.invokeLater(() -> {
            if (clientToUi.containsKey(clientConnection)) {
                JOptionPane.showMessageDialog(serverFrame, "Connection error (" + clientConnection.getIP() + ")", "Client Disconnected", JOptionPane.INFORMATION_MESSAGE);
                clientToUi.get(clientConnection).dispose();
                clientToUi.remove(clientConnection);
            }
            clientConnection.close();
            connectedClients.remove(clientConnection);
            updateConnectedClientsList();
        });
    }

    private void updateConnectedClientsList() {
        clientsPanel.removeAll();

        for (ClientConnection client : connectedClients) {
            JButton clientButton = createClientButton(client);
            clientsPanel.add(clientButton);
        }

        clientsPanel.revalidate();
        clientsPanel.repaint();
    }

    private JButton createClientButton(ClientConnection client) {
        JButton clientButton = new JButton(client.getName() + ", IP: " + client.getIP());
        clientButton.addActionListener(e -> {
            if (clientToUi.containsKey(client)) {
                ClientRemoteUI remoteUI = clientToUi.get(client);
                remoteUI.setVisible(true);
                remoteUI.toFront();
            } else {
                ClientRemoteUI remoteUI = new ClientRemoteUI(client, client.getName(), client.getIP());
                clientToUi.put(client, remoteUI);
                remoteUI.setVisible(true);
            }
        });
        return clientButton;
    }

    private void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private void centerWindow(Window window) {
        window.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Server server = new Server();
            server.startServer();
        });
    }
}