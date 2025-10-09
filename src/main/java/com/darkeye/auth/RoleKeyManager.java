package com.darkeye.auth;
import java.util.HashMap;
import java.util.Map;

/**
 * RoleKeyManager keeps a small local registry of known key identifiers (hashes) for roles.
 * In production this would be more secure; here it's a simple local store in memory.
 */
public class RoleKeyManager {

    private final Map<String,String> roleToId = new HashMap<>();

    public void registerKeyForRole(String role, String keyId) {
        roleToId.put(role, keyId);
    }

    public boolean isKeyKnownForRole(String role, String keyId) {
        String known = roleToId.get(role);
        return known != null && known.equals(keyId);
    }

    // persistence loading/saving could be implemented here (file-based) if needed
}
