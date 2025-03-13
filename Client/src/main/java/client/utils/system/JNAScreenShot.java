package client.utils.system;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;

import java.awt.image.*;

public class JNAScreenShot {
    public static BufferedImage getScreenshot() {
        WinDef.HDC windowDC = User32.INSTANCE.GetDC(null);
        WinDef.HDC memoryDC = GDI32.INSTANCE.CreateCompatibleDC(windowDC);

        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(User32.INSTANCE.GetDesktopWindow(), rect);
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDC, width, height);
        WinNT.HANDLE oldBitmap = GDI32.INSTANCE.SelectObject(memoryDC, hBitmap);

        GDI32.INSTANCE.BitBlt(memoryDC, 0, 0, width, height, windowDC, 0, 0, GDI32.SRCCOPY);
        GDI32.INSTANCE.SelectObject(memoryDC, oldBitmap);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        Memory memory = new Memory((long) width * height * 4);
        GDI32.INSTANCE.GetDIBits(windowDC, hBitmap, 0, height, memory, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];
        memory.read(0, pixels, 0, pixels.length);
        image.setRGB(0, 0, width, height, pixels, 0, width);

        GDI32.INSTANCE.DeleteObject(hBitmap);
        GDI32.INSTANCE.DeleteDC(memoryDC);
        User32.INSTANCE.ReleaseDC(null, windowDC);

        return image;
    }
}