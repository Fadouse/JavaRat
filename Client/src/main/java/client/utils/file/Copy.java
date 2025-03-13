package client.utils.file;

import client.ClientMain;
import client.utils.system.UUIDGenerator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static client.ClientMain.otherPath;

public class Copy {
    String path;
    public Copy(String path) {
        this.path = path;
    }
    public void copy() {
        try(FileInputStream fileInputStream = new FileInputStream(path); FileOutputStream fileOutputStream = new FileOutputStream(otherPath + "\\" + UUIDGenerator.generateUUID()+ ".jar")) {
            aVoid(fileInputStream,fileOutputStream);
        }catch (Exception ignored) {
        }
    }

    public void aVoid(FileInputStream fileInputStream, FileOutputStream fileOutputStream) throws IOException {
        byte[] bytes = new byte[1024];
        int len;
        while ((len = fileInputStream.read(bytes))!=-1) {
            fileOutputStream.write(bytes,0,len);
        }
    }

}
