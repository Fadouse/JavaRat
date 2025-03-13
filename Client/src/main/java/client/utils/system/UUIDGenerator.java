package client.utils.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UUIDGenerator {
    public static String generateUUID() {
        String computerInfo = String.valueOf(getCombinedHardwareID());

        String hash = sha256(computerInfo);

        String uuidString = null;
        if (hash != null) {
            uuidString = hash.substring(0, 32);
        }
        return uuidString;
    }

    private static String getSystemProperty(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        reader.close();

        // Ensure the line is not null and contains a valid ID
        if (line != null && !line.trim().isEmpty()) {
            return line.trim();
        } else {
            throw new RuntimeException("Unable to retrieve hardware ID");
        }
    }

    public static int getCombinedHardwareID() {
        try {
            // Get CPU ID
            String cpuID = getSystemProperty("wmic cpu get ProcessorId");

            // Get Motherboard ID
            String motherboardID = getSystemProperty("wmic baseboard get SerialNumber");

            // Combine and hash the IDs

            return (cpuID.hashCode() + motherboardID.hashCode());
        } catch (Exception e) {
            return 0; // Return 0 in case of an error
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
