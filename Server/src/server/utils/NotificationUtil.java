package server.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class NotificationUtil {
    static final SystemTray tray = SystemTray.getSystemTray();
    static final Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
    static final TrayIcon trayIcon = new TrayIcon(image, "Server Notification");

    public static void showNotification(String title, String message) {
        tray.remove(trayIcon);
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported on this platform");
            return;
        }

        try {
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        } catch (AWTException ignored) {
        }
    }
}
