package cc.keiran.claramella.features.welcome;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;

public class WelcomeListener implements Listener {

    private final Claramella plugin;
    private final DatabaseManager databaseManager;

    public WelcomeListener(Claramella plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!databaseManager.getConfigValue("welcome.enabled", Boolean.class, true)) {
            return;
        }
        
        Player player = event.getPlayer();
        String messageTemplate = databaseManager.getConfigValue("welcome.message", String.class, 
            "Welcome to the server, {player}!");
        
        String welcomeMessage = messageTemplate.replace("{player}", player.getName());
        
        player.sendMessage(ChatColor.AQUA + welcomeMessage);
        
        if (databaseManager.getConfigValue("welcome.log_joins", Boolean.class, true)) {
            plugin.getLogger().info("Sent welcome message to " + player.getName());
        }
    }
}
