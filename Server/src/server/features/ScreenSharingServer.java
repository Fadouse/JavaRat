package server.features;

import server.connection.ClientConnection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ScreenSharingServer {
    public boolean receivingScreens;

    public final JFrame popupFrame;
    private final JLabel imageLabel;

    public ScreenSharingServer(ClientConnection clientConnection) {
        popupFrame = new JFrame("Screen(" + clientConnection.getIP() + ")");
        popupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        imageLabel = new JLabel();
        popupFrame.getContentPane().add(imageLabel);
    }

    public void startReceivingScreens() {
        showScreenSharingPopup();
        receivingScreens = true;
    }

    private void showScreenSharingPopup() {
        popupFrame.setVisible(true);
        popupFrame.setSize(600, 400);
    }

    public void updateScreenSharingPopup(byte[] cameraData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(cameraData);
        BufferedImage cameraCapture = ImageIO.read(bais);

        // 计算宽度和高度的缩放比例
        double widthScale = (double) popupFrame.getWidth() / cameraCapture.getWidth();
        double heightScale = (double) popupFrame.getHeight() / cameraCapture.getHeight();
        double scale = Math.min(widthScale, heightScale);

        // 根据缩放比例进行缩放
        int scaledWidth = (int) (cameraCapture.getWidth() * scale);
        int scaledHeight = (int) (cameraCapture.getHeight() * scale);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, cameraCapture.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(cameraCapture, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        imageLabel.setSize(scaledWidth, scaledHeight);
        imageLabel.setIcon(new ImageIcon(scaledImage));
    }

    public void stopReceivingScreens() {
        popupFrame.setVisible(false);
        receivingScreens = false;
    }
}
