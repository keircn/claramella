package cc.keiran.claramella.features.warps;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WarpManager {
    
    private final Claramella plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, Warp> warpCache = new HashMap<>();
    
    public WarpManager(Claramella plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    public void initialize() {
        try {
            createWarpsTable();
            loadWarpsFromDatabase();
            plugin.getLogger().info("Warp system initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize warp system", e);
        }
    }
    
    private void createWarpsTable() throws SQLException {
        if (!databaseManager.isConnected()) {
            return;
        }
        
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS warps (
                name TEXT PRIMARY KEY,
                world_id TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL,
                created_by TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + 
            plugin.getDataFolder().getAbsolutePath() + "/config.db");
    }
    
    private void loadWarpsFromDatabase() throws SQLException {
        if (!databaseManager.isConnected()) {
            return;
        }
        
        String selectSQL = "SELECT * FROM warps";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                UUID worldId = UUID.fromString(rs.getString("world_id"));
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                long createdAt = rs.getLong("created_at");
                
                Warp warp = new Warp(name, worldId, x, y, z, yaw, pitch, createdBy, createdAt);
                warpCache.put(name.toLowerCase(), warp);
            }
        }
    }
    
    public CompletableFuture<Boolean> createWarp(String name, Location location, UUID createdBy) {
        return CompletableFuture.supplyAsync(() -> {
            if (warpExists(name)) {
                return false;
            }
            
            Warp warp = new Warp(name, location, createdBy);
            warpCache.put(name.toLowerCase(), warp);
            
            try {
                saveWarpToDatabase(warp);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save warp to database: " + name, e);
                warpCache.remove(name.toLowerCase());
                return false;
            }
        });
    }
    
    private void saveWarpToDatabase(Warp warp) throws SQLException {
        if (!databaseManager.isConnected()) {
            return;
        }
        
        String insertSQL = """
            INSERT INTO warps (name, world_id, x, y, z, yaw, pitch, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, warp.getName());
            pstmt.setString(2, warp.getWorldId().toString());
            pstmt.setDouble(3, warp.getX());
            pstmt.setDouble(4, warp.getY());
            pstmt.setDouble(5, warp.getZ());
            pstmt.setFloat(6, warp.getYaw());
            pstmt.setFloat(7, warp.getPitch());
            pstmt.setString(8, warp.getCreatedBy().toString());
            pstmt.setLong(9, warp.getCreatedAt());
            pstmt.executeUpdate();
        }
    }
    
    public CompletableFuture<Boolean> deleteWarp(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (!warpExists(name)) {
                return false;
            }
            
            warpCache.remove(name.toLowerCase());
            
            try {
                deleteWarpFromDatabase(name);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete warp from database: " + name, e);
                return false;
            }
        });
    }
    
    private void deleteWarpFromDatabase(String name) throws SQLException {
        if (!databaseManager.isConnected()) {
            return;
        }
        
        String deleteSQL = "DELETE FROM warps WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }
    
    public boolean warpExists(String name) {
        return warpCache.containsKey(name.toLowerCase());
    }
    
    public Warp getWarp(String name) {
        return warpCache.get(name.toLowerCase());
    }
    
    public Set<String> getWarpNames() {
        return new HashSet<>(warpCache.keySet());
    }
    
    public Collection<Warp> getAllWarps() {
        return new ArrayList<>(warpCache.values());
    }
    
    public boolean teleportToWarp(Player player, String warpName) {
        Warp warp = getWarp(warpName);
        if (warp == null) {
            return false;
        }
        
        Location location = warp.getLocation();
        if (location == null) {
            return false;
        }
        
        player.teleport(location);
        return true;
    }
}
