package client.utils.file;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class JarUtil {
    private final String jarName;
    private String jarPath;

    public JarUtil(Class<?> clazz) {
        String path = clazz.getProtectionDomain().getCodeSource()
                .getLocation().getFile();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {}

        File jarFile = new File(path);
        this.jarName = jarFile.getName();

        File parent = jarFile.getParentFile();
        if (parent != null) {
            this.jarPath = parent.getAbsolutePath();
        }
    }

    public String getJarName() {
        try {
            return URLDecoder.decode(this.jarName, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
    }

    public String getJarPath() {
        try {
            return URLDecoder.decode(this.jarPath, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return null;
    }
}
