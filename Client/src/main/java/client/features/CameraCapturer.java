package client.features;

import client.connection.ClientConnection;
import client.connection.ID;
import com.github.sarxos.webcam.Webcam;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraCapturer {
    private final ClientConnection connection;
    public boolean sharingCamera = false;
    public Webcam webcam;

    public CameraCapturer(ClientConnection connection) {
        this.connection = connection;
    }

    private static final ImageWriter writer;
    private static final ImageWriteParam param;

    static {
        writer = ImageIO.getImageWritersByFormatName("jpg").next();
        param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) 0.7);
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

    public void start() throws InterruptedException {
        new Thread(() -> {
            if(connection == null)
                return;
            sharingCamera = true;
            boolean isOpen = open();
            if(!isOpen)
                return;
            while (sharingCamera) {
                try {
                    Thread.sleep(300);
                    // 捕获屏幕截图
                    BufferedImage captureCameraImage = webcam.getImage();
                    // 压缩图像
                    byte[] compressedImage = compressImage(captureCameraImage);
                    // 将压缩后的屏幕截图发送到服务端

                    connection.sendHead(ID.CAMERA());
                    connection.sendData(compressedImage);
                    System.gc();
                } catch (Exception e) {
                    sharingCamera = false;
                }
            }

        }).start();
        System.gc();
    }

    public void stop(){
        sharingCamera = false;
    }

    public boolean open(){
        try {
            webcam = Webcam.getDefault();
            webcam.open();
        }catch (Exception e) {

            return false;
        }
        return true;
    }
}
