package com.darkeye.auth;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * USBAuthManager
 * - Manages creation and validation of role keys stored on external USB drives.
 * - For this prototype we simulate by scanning mounted drives and looking for files named admin.key, analyst.key, viewer.key
 */
public class USBAuthManager {

    private static final String ADMIN_KEY = "admin.key";
    private static final String ANALYST_KEY = "analyst.key";
    private static final String VIEWER_KEY = "viewer.key";

    /** Called during first-boot setup to write keys to the provided drive path. */
    public static boolean provisionKeysToDrive(Path driveRoot) {
        try {
            // Create small random keys (base64 hash)
            String admin = hash("admin-" + System.currentTimeMillis());
            String analyst = hash("analyst-" + System.currentTimeMillis());
            String viewer = hash("viewer-" + System.currentTimeMillis());

            Files.writeString(driveRoot.resolve(ADMIN_KEY), admin, StandardOpenOption.CREATE);
            Files.writeString(driveRoot.resolve(ANALYST_KEY), analyst, StandardOpenOption.CREATE);
            Files.writeString(driveRoot.resolve(VIEWER_KEY), viewer, StandardOpenOption.CREATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean requestAdminKey() {
        return findAndValidateKey(ADMIN_KEY);
    }

    public static boolean requestUserKey() {
        // analyst or viewer may be accepted depending on action; here accept analyst or viewer
        return findAndValidateKey(ANALYST_KEY) || findAndValidateKey(VIEWER_KEY);
    }

    private static boolean findAndValidateKey(String keyFileName) {
        try {
            List<Path> roots = listFileSystemRoots();
            for (Path p : roots) {
                Path pf = p.resolve(keyFileName);
                if (Files.exists(pf)) {
                    String content = Files.readString(pf).trim();
                    // in a real implementation you'd validate a signature; here we just accept any non-empty key
                    if (!content.isEmpty()) {
                        System.out.println("[USBAuth] Found key " + keyFileName + " on " + p);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        System.out.println("[USBAuth] Key " + keyFileName + " not found on any mounted drive.");
        return false;
    }

    private static List<Path> listFileSystemRoots() {
        return File.listRoots() == null ? List.of() :
                java.util.Arrays.stream(File.listRoots()).map(File::toPath).collect(Collectors.toList());
    }

    private static String hash(String in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(in.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(d);
    }
}
