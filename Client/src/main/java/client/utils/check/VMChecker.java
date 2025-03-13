package client.utils.check;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Scanner;

import static client.utils.system.NotificationUtil.showButtonNotificationNoNewThread;

public class VMChecker {
    // 定义一些常量，用于存储特定的进程名，服务名，文件名，注册表键名，MAC地址前缀等
    private static final String[] VM_SERVICE_NAMES = {"VMTools", "VBoxService", "vmicguestinterface", "vmicheartbeat"};
    private static final String[] VM_FILE_NAMES = {"vmware.sys", "vboxmouse.sys", "vmmouse.sys", "vmhgfs.sys"};
    private static final String[] VM_REGISTRY_KEYS = {"SOFTWARE\\VMware, Inc.\\VMware Tools", "SOFTWARE\\Oracle\\VirtualBox Guest Additions", "SYSTEM\\ControlSet001\\Services\\Disk\\Enum\\0"};

    // 定义一个静态方法，返回一个布尔值，表示是否在虚拟机环境中运行
    public static boolean isRunningInVM() throws IOException {
        // 尝试使用不同的检测方法，如果任何一个方法返回true，就认为是在虚拟机中运行
        return checkServiceNames() || checkFileNames() || checkRegistryKeys() || checkCPUId();
    }

    // 定义一个私有的静态方法，使用特定服务检测
    private static boolean checkServiceNames() {
        // 获取当前运行的服务列表
        Process process;
        try {
            process = Runtime.getRuntime().exec("sc query");
        } catch (Exception e) {
            return false;
        }
        // 读取服务列表的输出
        Scanner scanner = new Scanner(process.getInputStream());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // 遍历特定的服务名，如果发现匹配，就返回true
            for (String name : VM_SERVICE_NAMES) {
                if (line.contains(name)) {
                    scanner.close();
                    return true;
                }
            }
        }
        scanner.close();
        return false;
    }

    // 定义一个私有的静态方法，使用特定文件检测
    private static boolean checkFileNames() {
        // 获取系统驱动器的根目录
        File root = new File(System.getenv("SystemDrive") + "\\");
        // 遍历特定的文件名，如果发现存在，就返回true
        for (String name : VM_FILE_NAMES) {
            File file = new File(root, name);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    // 定义一个私有的静态方法，使用注册表检测
    private static boolean checkRegistryKeys() {
        // 遍历特定的注册表键名，如果发现存在，就返回true
        for (String key : VM_REGISTRY_KEYS) {
            Process process = null;
            try {
                // 使用reg命令查询注册表键值
                process = Runtime.getRuntime().exec("reg query \"" + key + "\"");
                // 读取查询结果的输出
                Scanner scanner = new Scanner(process.getInputStream());
                if (scanner.hasNextLine()) {
                    // 如果有输出，说明注册表键存在
                    scanner.close();
                    return true;
                }
                scanner.close();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // 定义一个私有的静态方法，使用CPUId检测
    private static boolean checkCPUId() throws IOException {
        String cpuId = getCpuId();
        if (cpuId != null)
            // 检查CPUId是否包含特定的字符串，如果是，就返回true
            return cpuId.contains("VMware") || cpuId.contains("VirtualPC");
        return false;
    }

    // 定义一个本地方法，用于获取CPUId的信息
    public static String getCpuId() throws IOException {
        // linux，windows命令
        String[] linux = {"dmidecode", "-t", "processor", "|", "grep", "'ID'"};
        String[] windows = {"wmic", "cpu", "get", "ProcessorId"};
        // 获取系统信息
        String property = System.getProperty("os.name");
        Process process = Runtime.getRuntime().exec(property.contains("Window") ? windows : linux);
        process.getOutputStream().close();
        Scanner sc = new Scanner(process.getInputStream(), "utf-8");
        sc.next();
        return sc.next();
    }

    public static boolean isRunningInSandbox() {
        boolean isSandbox = false;
        long runtime = ManagementFactory.getRuntimeMXBean().getUptime();
        long systemUptime = System.currentTimeMillis() - runtime;
        if (systemUptime < 60000)
            isSandbox = true;
        long totalPhysicalMemory = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        if (totalPhysicalMemory < 4294967296L)
            isSandbox = true;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors < 4)
            isSandbox = true;
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempDirFile = new File(tempDir);
        if (tempDirFile.isDirectory()) {
            File[] tempFiles = tempDirFile.listFiles();
            if (tempFiles != null && tempFiles.length < 10) {
                isSandbox = true;
            }
        }
        return isSandbox;
    }
    public static void check() throws IOException {
        if(isRunningInVM()){
            showButtonNotificationNoNewThread("Error", "This program didn't support Virtual Machine");
            System.exit(0);
        }
        if(isRunningInSandbox()){
            showButtonNotificationNoNewThread("Error", "This program didn't support SandBox");
            System.exit(0);
        }
    }
}
