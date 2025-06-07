package cc.keiran.claramella;

import org.bukkit.plugin.java.JavaPlugin;
import cc.keiran.claramella.commands.ConfigCommand;
import cc.keiran.claramella.commands.AdminCommand;
import cc.keiran.claramella.commands.WarpCommand;
import cc.keiran.claramella.config.DatabaseManager;
import cc.keiran.claramella.features.admin.AdminManager;
import cc.keiran.claramella.features.sleep.SleepListener;
import cc.keiran.claramella.features.welcome.WelcomeListener;
import cc.keiran.claramella.features.warps.WarpManager;

public class Claramella extends JavaPlugin {

    private DatabaseManager databaseManager;
    private AdminManager adminManager;
    private WarpManager warpManager;
    private SleepListener sleepListener;
    private WelcomeListener welcomeListener;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        adminManager = new AdminManager(this, databaseManager);
        warpManager = new WarpManager(this, databaseManager);
        warpManager.initialize();
        
        welcomeListener = new WelcomeListener(this, databaseManager);
        sleepListener = new SleepListener(this, databaseManager);
        
        getServer().getPluginManager().registerEvents(welcomeListener, this);
        getServer().getPluginManager().registerEvents(sleepListener, this);
        getServer().getPluginManager().registerEvents(adminManager, this);
        
        ConfigCommand configCommand = new ConfigCommand(this, databaseManager);
        getCommand("claramella").setExecutor(configCommand);
        getCommand("claramella").setTabCompleter(configCommand);
        
        AdminCommand adminCommand = new AdminCommand(this, databaseManager, adminManager);
        getCommand("admin").setExecutor(adminCommand);
        getCommand("admin").setTabCompleter(adminCommand);
        
        WarpCommand warpCommand = new WarpCommand(this, databaseManager, warpManager);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("warp").setTabCompleter(warpCommand);

        if (databaseManager.getConfigValue("plugin.debug_mode", Boolean.class, false)) {
            getLogger().info("Debug mode enabled");
        }

        getLogger().info("Claramella has been enabled.");
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
    
    public AdminManager getAdminManager() {
        return adminManager;
    }
    
    public WarpManager getWarpManager() {
        return warpManager;
    }
}
