package client.utils.dll;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;

public interface LoadUser32 extends Library {
    LoadUser32 instance= (LoadUser32) Native.loadLibrary("User32.dll", LoadUser32.class);
    WinDef.SHORT GetKeyState(int i);
}