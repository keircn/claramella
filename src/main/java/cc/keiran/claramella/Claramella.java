package cc.keiran.claramella;

import org.bukkit.plugin.java.JavaPlugin;
import cc.keiran.claramella.commands.ConfigCommand;
import cc.keiran.claramella.config.DatabaseManager;
import cc.keiran.claramella.features.sleep.SleepListener;
import cc.keiran.claramella.features.welcome.WelcomeListener;
import cc.keiran.claramella.test.ConfigTest;

public class Claramella extends JavaPlugin {

    private DatabaseManager databaseManager;
    private SleepListener sleepListener;
    private WelcomeListener welcomeListener;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        welcomeListener = new WelcomeListener(this, databaseManager);
        sleepListener = new SleepListener(this, databaseManager);
        
        getServer().getPluginManager().registerEvents(welcomeListener, this);
        getServer().getPluginManager().registerEvents(sleepListener, this);
        
        ConfigCommand configCommand = new ConfigCommand(this, databaseManager);
        getCommand("claramella").setExecutor(configCommand);
        getCommand("claramella").setTabCompleter(configCommand);

        if (databaseManager.getConfigValue("plugin.debug_mode", Boolean.class, false)) {
            getLogger().info("Debug mode enabled - running configuration system test");
            ConfigTest.runTest(this);
            ConfigTest.logDatabaseInfo(this, databaseManager);
        }

        getLogger().info("Claramella has been enabled with database configuration system.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        getLogger().info("Claramella has been disabled.");
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
