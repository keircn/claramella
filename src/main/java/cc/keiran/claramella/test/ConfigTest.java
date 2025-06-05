package cc.keiran.claramella.test;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Simple test class to verify database configuration functionality
 * This is not meant for production use, just for testing the system
 */
public class ConfigTest {
    
    public static void runTest(Claramella plugin) {
        Logger logger = plugin.getLogger();
        logger.info("=== Starting Configuration System Test ===");
        
        try {
            DatabaseManager dbManager = plugin.getDatabaseManager();
            
            logger.info("Test 1: Basic Operations");
            dbManager.setConfigValue("test.string_value", "Hello World").join();
            dbManager.setConfigValue("test.boolean_value", true).join();
            dbManager.setConfigValue("test.integer_value", 42).join();
            dbManager.setConfigValue("test.double_value", 3.14159).join();
            
            String stringVal = dbManager.getConfigValue("test.string_value", String.class);
            Boolean boolVal = dbManager.getConfigValue("test.boolean_value", Boolean.class);
            Integer intVal = dbManager.getConfigValue("test.integer_value", Integer.class);
            Double doubleVal = dbManager.getConfigValue("test.double_value", Double.class);
            
            logger.info("String value: " + stringVal);
            logger.info("Boolean value: " + boolVal);
            logger.info("Integer value: " + intVal);
            logger.info("Double value: " + doubleVal);
            
            logger.info("Test 2: Default Values");
            String defaultStr = dbManager.getConfigValue("test.nonexistent", String.class, "default");
            Integer defaultInt = dbManager.getConfigValue("test.nonexistent_int", Integer.class, 999);
            
            logger.info("Default string: " + defaultStr);
            logger.info("Default integer: " + defaultInt);
            
            logger.info("Test 3: Sleep Configuration");
            Double sleepPercentage = dbManager.getConfigValue("sleep.percentage_required", Double.class, 0.5);
            Integer minPlayers = dbManager.getConfigValue("sleep.minimum_players_for_vote", Integer.class, 2);
            Boolean singlePlayerSkip = dbManager.getConfigValue("sleep.single_player_skip", Boolean.class, true);
            
            logger.info("Sleep percentage: " + sleepPercentage);
            logger.info("Min players for vote: " + minPlayers);
            logger.info("Single player skip: " + singlePlayerSkip);
            
            logger.info("Test 4: Connection Status");
            logger.info("Database connected: " + dbManager.isConnected());
            logger.info("Total config values: " + dbManager.getAllConfigValues().size());
            
            logger.info("Test 5: Update Operations");
            dbManager.setConfigValue("sleep.percentage_required", 0.75).join();
            Double updatedPercentage = dbManager.getConfigValue("sleep.percentage_required", Double.class);
            logger.info("Updated sleep percentage: " + updatedPercentage);
            
            dbManager.setConfigValue("test.string_value", null);
            dbManager.setConfigValue("test.boolean_value", null);
            dbManager.setConfigValue("test.integer_value", null);
            dbManager.setConfigValue("test.double_value", null);
            
            logger.info("=== Configuration System Test Completed Successfully ===");
            
        } catch (Exception e) {
            logger.severe("Configuration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void logDatabaseInfo(JavaPlugin plugin, DatabaseManager dbManager) {
        Logger logger = plugin.getLogger();
        logger.info("=== Database Configuration Info ===");
        
        File dbFile = new File(plugin.getDataFolder(), "config.db");
        logger.info("Database file: " + dbFile.getAbsolutePath());
        logger.info("Database exists: " + dbFile.exists());
        logger.info("Database size: " + (dbFile.exists() ? dbFile.length() + " bytes" : "N/A"));
        logger.info("Database connected: " + dbManager.isConnected());
        
        logger.info("Current configuration values:");
        dbManager.getAllConfigValues().forEach((key, value) -> {
            logger.info("  " + key + " = " + value + " (" + value.getClass().getSimpleName() + ")");
        });
    }
}
