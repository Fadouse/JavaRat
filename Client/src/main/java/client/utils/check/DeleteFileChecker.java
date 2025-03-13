package client.utils.check;

import client.utils.system.UUIDGenerator;

import java.io.File;

import static client.ClientMain.*;

public class DeleteFileChecker extends Thread{
    @Override
    public void run() {
        new Thread(()->{
                while (true) {
                    try {
                        Thread.sleep(300);
                        File file = new File(getOtherPath()+ UUIDGenerator.generateUUID()+".jar");
                        if(!file.exists() || !calculateMD5(file).equals(fileMD5)){
                            register();
                            lock();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        ).start();

    }
}
