package com.darkeye.security;

import org.mindrot.jbcrypt.BCrypt;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BCrypt-based authentication service with role-based access control
 */
public class AuthenticationService {
    
    public enum Role {
        ADMIN("Administrator - Full system access"),
        ANALYST("Security Analyst - View and analyze logs"),
        VIEWER("Viewer - Read-only access");
        
        private final String description;
        
        Role(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public static class User {
        private final String username;
        private final String hashedPassword;
        private final Role role;
        private final boolean active;
        
        public User(String username, String hashedPassword, Role role, boolean active) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.role = role;
            this.active = active;
        }
        
        public String getUsername() { return username; }
        public String getHashedPassword() { return hashedPassword; }
        public Role getRole() { return role; }
        public boolean isActive() { return active; }
    }
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    
    public AuthenticationService() {
        initializeDefaultUsers();
    }
    
    /**
     * Initialize default users for the system
     */
    private void initializeDefaultUsers() {
    // Default admin user
    addUser("admin", "Codex", Role.ADMIN);
        
        // Default analyst user (demo password)
        addUser("analyst", "Codex", Role.ANALYST);
        
        // Default viewer user (demo password)
        addUser("viewer", "Codex", Role.VIEWER);
    }
    
    /**
     * Add a new user to the system
     * @param username the username
     * @param password the plain text password
     * @param role the user role
     * @return true if user was added successfully
     */
    public boolean addUser(String username, String password, Role role) {
        if (username == null || password == null || role == null) {
            return false;
        }
        
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(username, hashedPassword, role, true);
        users.put(username, user);
        
        return true;
    }
    
    /**
     * Authenticate a user with username and password
     * @param username the username
     * @param password the plain text password
     * @return authentication token if successful, null otherwise
     */
    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        User user = users.get(username);
        if (user == null || !user.isActive()) {
            return null;
        }
        
        if (BCrypt.checkpw(password, user.getHashedPassword())) {
            // Generate session token
            String token = generateSessionToken();
            activeSessions.put(token, username);
            return token;
        }
        
        return null;
    }
    
    /**
     * Validate a session token
     * @param token the session token
     * @return the username if token is valid, null otherwise
     */
    public String validateSession(String token) {
        return activeSessions.get(token);
    }
    
    /**
     * Get user information by username
     * @param username the username
     * @return user object or null if not found
     */
    public User getUser(String username) {
        return users.get(username);
    }
    
    /**
     * Get user information by session token
     * @param token the session token
     * @return user object or null if not found
     */
    public User getUserByToken(String token) {
        String username = validateSession(token);
        return username != null ? users.get(username) : null;
    }
    
    /**
     * Check if user has required role
     * @param token the session token
     * @param requiredRole the required role
     * @return true if user has the required role or higher
     */
    public boolean hasRole(String token, Role requiredRole) {
        User user = getUserByToken(token);
        if (user == null) {
            return false;
        }
        
        // Role hierarchy: ADMIN > ANALYST > VIEWER
        switch (requiredRole) {
            case VIEWER:
                return true; // All roles can view
            case ANALYST:
                return user.getRole() == Role.ANALYST || user.getRole() == Role.ADMIN;
            case ADMIN:
                return user.getRole() == Role.ADMIN;
            default:
                return false;
        }
    }
    
    /**
     * Logout a user (invalidate session)
     * @param token the session token
     * @return true if logout was successful
     */
    public boolean logout(String token) {
        return activeSessions.remove(token) != null;
    }
    
    /**
     * Change user password
     * @param token the session token
     * @param oldPassword the current password
     * @param newPassword the new password
     * @return true if password was changed successfully
     */
    public boolean changePassword(String token, String oldPassword, String newPassword) {
        User user = getUserByToken(token);
        if (user == null) {
            return false;
        }
        
        if (!BCrypt.checkpw(oldPassword, user.getHashedPassword())) {
            return false; // Old password incorrect
        }
        
        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        User updatedUser = new User(user.getUsername(), newHashedPassword, user.getRole(), user.isActive());
        users.put(user.getUsername(), updatedUser);
        
        return true;
    }
    
    /**
     * Deactivate a user
     * @param adminToken the admin session token
     * @param username the username to deactivate
     * @return true if user was deactivated
     */
    public boolean deactivateUser(String adminToken, String username) {
        if (!hasRole(adminToken, Role.ADMIN)) {
            return false;
        }
        
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        
        User deactivatedUser = new User(user.getUsername(), user.getHashedPassword(), user.getRole(), false);
        users.put(username, deactivatedUser);
        
        // Invalidate all sessions for this user
        activeSessions.entrySet().removeIf(entry -> entry.getValue().equals(username));
        
        return true;
    }
    
    /**
     * Get all users (admin only)
     * @param adminToken the admin session token
     * @return map of all users
     */
    public Map<String, User> getAllUsers(String adminToken) {
        if (!hasRole(adminToken, Role.ADMIN)) {
            return new HashMap<>();
        }
        
        return new HashMap<>(users);
    }
    
    /**
     * Generate a secure session token
     */
    private String generateSessionToken() {
        return BCrypt.gensalt(10) + System.currentTimeMillis();
    }
}
