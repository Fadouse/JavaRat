package server.features;

import server.connection.ClientConnection;
import server.utils.ID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class FileManagerUI extends JFrame {
    private final ClientConnection clientConnection;
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private JTextField currentPathField;
    private JButton upButton;
    private JButton goButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    // 默认打开到 jar 运行目录
    private String currentPath = System.getProperty("user.dir") + File.separator;

    public FileManagerUI(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        initComponents();
        setupLayout();
        setTitle("File Manager");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        try {
            clientConnection.sendMessage(client.connection.ID.FILE_DIR(), System.getProperty("user.dir"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 显示默认目录
        currentPathField.setText(currentPath);
    }

    private void initComponents() {
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedItem = fileList.getSelectedValue();
                    if (selectedItem != null && selectedItem.endsWith("/")) {
                        navigateTo(currentPath + selectedItem);
                    }
                }
            }
        });

        currentPathField = new JTextField();
        upButton = new JButton("Up");
        goButton = new JButton("Go");
        uploadButton = new JButton("Upload");
        downloadButton = new JButton("Download");
        deleteButton = new JButton("Delete");

        upButton.addActionListener(e -> navigateUp());
        goButton.addActionListener(e -> navigateTo(currentPathField.getText()));
        uploadButton.addActionListener(e -> uploadFile());
        downloadButton.addActionListener(e -> downloadFile());
        deleteButton.addActionListener(e -> deleteFile());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(currentPathField, BorderLayout.CENTER);
        topPanel.add(upButton, BorderLayout.WEST);
        topPanel.add(goButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(deleteButton);

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(580, 300));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void updateFileList(List<String> files) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String file : files) {
                listModel.addElement(file);
            }
            System.out.println("File list updated with " + files.size() + " items");
            fileList.setModel(listModel);
            fileList.revalidate();
            fileList.repaint();
            if (fileList.getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) fileList.getParent();
                if (viewport.getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) viewport.getParent();
                    scrollPane.revalidate();
                    scrollPane.repaint();
                }
            }
        });
    }

    private void navigateUp() {
        File current = new File(currentPath);
        if (current.getParent() != null) {
            navigateTo(current.getParent());
        }
    }

    private void navigateTo(String path) {
        currentPath = path;
        currentPathField.setText(currentPath);
        try {
            clientConnection.sendMessage(ID.FILE_DIR(), path);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error navigating to directory: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                clientConnection.sendMessage(ID.FILE_IN(), currentPath + File.separator + selectedFile.getName());
                clientConnection.sendFile(selectedFile);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error uploading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void downloadFile() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null || selectedFile.endsWith("/")) {
            JOptionPane.showMessageDialog(this, "Please select a file to download.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(selectedFile));
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try {
                clientConnection.sendMessage(ID.FILE_OUT(), currentPath + File.separator + selectedFile);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error initiating file download: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteFile() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a file or directory to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selectedFile + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                clientConnection.sendMessage(ID.COMMAND(), "delete:" + currentPath + File.separator + selectedFile);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
