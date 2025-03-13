package client.utils.system;

import client.features.CmdExecutor;
import client.features.ShellCommandExecutor;
import com.sun.jna.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class NotificationUtil {

    public static void showButtonNotification(String title, String msg) {
        new Thread(() -> {
            if (Platform.isWindows()) {
                CmdExecutor.executeCmd("mshta vbscript:msgbox(\"" + msg +"\",64,\"" +title+"\")(window.close)");
                try {
                    Thread.sleep(100);
                    bringWindowToFront();
                } catch (Exception ignored) {}
            } else {
                JOptionPane.showMessageDialog(null, title, msg, JOptionPane.INFORMATION_MESSAGE);
            }
        }).start();
    }

    public static void showButtonNotificationNoNewThread(String title, String msg) {
        if (Platform.isWindows()) {
            CmdExecutor.executeCmd("mshta vbscript:msgbox(\"" + msg +"\",64,\"" +title+"\")(window.close)");
            try {
                Thread.sleep(1000);
                bringWindowToFront();
            } catch (Exception ignored) {}
        } else {
            JOptionPane.showMessageDialog(null, title, msg, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void bringWindowToFront() throws AWTException {
        Robot robot = new Robot();

        // 模拟按下 Alt+Tab 快捷键
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_ALT);
    }
    public static void showNotification(String title, String message) {
        String powerShellScript = String.format("\"$ErrorActionPreference = 'Stop';" +
                "$notificationTitle = '%s';" +
                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null;" +
                "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText01);" +
                "$toastXml = [xml] $template.GetXml();" +
                "$toastXml.GetElementsByTagName('text').AppendChild($toastXml.CreateTextNode($notificationTitle)) > $null;" +
                "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument;" +
                "$xml.LoadXml($toastXml.OuterXml);" +
                "$toast = [Windows.UI.Notifications.ToastNotification]::new($xml);" +
                "$toast.Tag = 'Rat';" +
                "$toast.Group = ':D';" +
                "$toast.ExpirationTime = [DateTimeOffset]::Now.AddSeconds(16);" +
                "$notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('%s');" +
                "$notifier.Show($toast);\"", message,title);

        ShellCommandExecutor.executeShellCommand(powerShellScript);
    }
}