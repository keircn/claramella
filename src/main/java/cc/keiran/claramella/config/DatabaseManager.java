package cc.keiran.claramella.config;

import cc.keiran.claramella.Claramella;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final Claramella plugin;
    private Connection connection;
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private final String databasePath;
    private final Map<String, Object> defaultConfig = new HashMap<>();
    
    public DatabaseManager(Claramella plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "config.db";
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        defaultConfig.put("sleep.delay_ticks", 100L);
        defaultConfig.put("sleep.check_interval", 20L);
        defaultConfig.put("sleep.percentage_required", 0.5);
        defaultConfig.put("sleep.minimum_players_for_vote", 2);
        defaultConfig.put("sleep.single_player_skip", true);
        defaultConfig.put("sleep.show_progress_messages", true);
        defaultConfig.put("sleep.skip_message", "☀ The night has been skipped! Good morning!");
        defaultConfig.put("welcome.enabled", true);
        defaultConfig.put("welcome.message", "Welcome to the server, {player}!");
        defaultConfig.put("welcome.log_joins", true);
        defaultConfig.put("admin.default_fly_speed", 0.1f);
        defaultConfig.put("admin.default_walk_speed", 0.2f);
        defaultConfig.put("admin.max_fly_speed", 1.0f);
        defaultConfig.put("admin.max_walk_speed", 1.0f);
        defaultConfig.put("admin.invulnerability_timeout", 300000L);
        defaultConfig.put("admin.announce_god_mode", true);
        defaultConfig.put("admin.announce_invulnerability", true);
        defaultConfig.put("plugin.debug_mode", false);
        defaultConfig.put("plugin.language", "en");
    }
    
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                createConfigTable();
                loadConfigurationCache();
                plugin.getLogger().info("Database configuration system initialized successfully");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("SQLite JDBC driver not found. Falling back to file-based configuration.");
                initializeFallbackConfig();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database configuration", e);
            initializeFallbackConfig();
        }
    }
    
    private void createConfigTable() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS config (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
        String triggerSQL = """
            CREATE TRIGGER IF NOT EXISTS update_config_timestamp 
            AFTER UPDATE ON config
            BEGIN
                UPDATE config SET updated_at = CURRENT_TIMESTAMP WHERE key = NEW.key;
            END
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(triggerSQL);
        }
    }
    
    private void loadConfigurationCache() throws SQLException {
        String selectSQL = "SELECT key, value, type FROM config";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                String key = rs.getString("key");
                String value = rs.getString("value");
                String type = rs.getString("type");
                Object parsedValue = parseValue(value, type);
                configCache.put(key, parsedValue);
            }
        }
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            if (!configCache.containsKey(entry.getKey())) {
                configCache.put(entry.getKey(), entry.getValue());
                saveConfigValue(entry.getKey(), entry.getValue(), getDescription(entry.getKey()));
            }
        }
    }
    
    private Object parseValue(String value, String type) {
        return switch (type.toLowerCase()) {
            case "boolean" -> Boolean.parseBoolean(value);
            case "int", "integer" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "double" -> Double.parseDouble(value);
            case "float" -> Float.parseFloat(value);
            default -> value;
        };
    }
    
    private String getTypeString(Object value) {
        return switch (value) {
            case Boolean b -> "boolean";
            case Integer i -> "int";
            case Long l -> "long";
            case Double d -> "double";
            case Float f -> "float";
            default -> "string";
        };
    }
    
    private String getDescription(String key) {
        return switch (key) {
            case "sleep.delay_ticks" -> "Delay in ticks before checking sleep conditions";
            case "sleep.check_interval" -> "Interval in ticks between sleep progress checks";
            case "sleep.percentage_required" -> "Percentage of players required to sleep (0.0-1.0)";
            case "sleep.minimum_players_for_vote" -> "Minimum players online before requiring vote";
            case "sleep.single_player_skip" -> "Allow single player to skip night instantly";
            case "sleep.show_progress_messages" -> "Show sleep progress messages to players";
            case "sleep.skip_message" -> "Message shown when night is skipped";
            case "welcome.enabled" -> "Enable welcome messages for joining players";
            case "welcome.message" -> "Welcome message template ({player} for player name)";
            case "welcome.log_joins" -> "Log player joins to console";
            case "plugin.debug_mode" -> "Enable debug logging";
            case "plugin.language" -> "Plugin language (en, es, fr, etc.)";
            default -> "Configuration value for " + key;
        };
    }
    
    public CompletableFuture<Void> setConfigValue(String key, Object value) {
        return CompletableFuture.runAsync(() -> {
            configCache.put(key, value);
            try {
                saveConfigValue(key, value, getDescription(key));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save config value: " + key, e);
            }
        });
    }
    
    private void saveConfigValue(String key, Object value, String description) throws SQLException {
        String upsertSQL = """
            INSERT OR REPLACE INTO config (key, value, type, description) 
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
            pstmt.setString(1, key);
            pstmt.setString(2, String.valueOf(value));
            pstmt.setString(3, getTypeString(value));
            pstmt.setString(4, description);
            pstmt.executeUpdate();
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, Class<T> type) {
        Object value = configCache.get(key);
        if (value == null) {
            value = defaultConfig.get(key);
        }
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        if (value != null) {
            return convertValue(value, type);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> type) {
        String stringValue = String.valueOf(value);
        if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(Boolean.parseBoolean(stringValue));
        } else if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(Integer.parseInt(stringValue));
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(Long.parseLong(stringValue));
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(Double.parseDouble(stringValue));
        } else if (type == Float.class || type == float.class) {
            return (T) Float.valueOf(Float.parseFloat(stringValue));
        } else if (type == String.class) {
            return (T) stringValue;
        }
        return null;
    }
    
    public <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        T value = getConfigValue(key, type);
        return value != null ? value : defaultValue;
    }
    
    public Map<String, Object> getAllConfigValues() {
        return new HashMap<>(configCache);
    }
    
    public CompletableFuture<Void> resetToDefaults() {
        return CompletableFuture.runAsync(() -> {
            try {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("DELETE FROM config");
                }
                configCache.clear();
                loadConfigurationCache();
                plugin.getLogger().info("Configuration reset to defaults");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset configuration", e);
            }
        });
    }
    
    private void initializeFallbackConfig() {
        plugin.getLogger().warning("Falling back to file-based configuration");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            String key = entry.getKey();
            Object value = config.get(key.replace(".", "-"), entry.getValue());
            configCache.put(key, value);
        }
    }
    
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
