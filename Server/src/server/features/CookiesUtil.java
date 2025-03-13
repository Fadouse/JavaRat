package server.features;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CookiesUtil {
    public static void saveCookiesData(byte[] chromeData,byte[] chromeKey, byte[] edgeData, byte[] edgeKey) {
        // 使用Swing弹窗选择保存位置
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose the path to save");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File saveLocation = fileChooser.getSelectedFile();

            // 保存Chrome Cookies数据
            File chromeFile = new File(saveLocation, "chrome_cookies");
            saveToFile(chromeData, chromeFile);

            File chromeKeyFile = new File(saveLocation, "chrome_cookies_key.json");
            saveToFile(chromeKey, chromeKeyFile);

            // 保存Edge Cookies数据
            File edgeFile = new File(saveLocation, "edge_cookies");
            saveToFile(edgeData, edgeFile);

            File edgeKeyFile = new File(saveLocation, "edge_cookies_key.json");
            saveToFile(edgeKey, edgeKeyFile);

            JOptionPane.showMessageDialog(null, "CookiesUtil data saved！");
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
