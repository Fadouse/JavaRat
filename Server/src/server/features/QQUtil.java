package server.features;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class QQUtil {
    public static void saveQQLoginData(byte[] qqData, String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose the path to save");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File saveLocation = fileChooser.getSelectedFile();

            File chromeFile = new File(saveLocation, fileName);
            saveToFile(qqData, chromeFile);
        }
    }

    private static void saveToFile(byte[] data, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Save failed：" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
