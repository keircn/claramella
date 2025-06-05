package cc.keiran.claramella.features.welcome;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import cc.keiran.claramella.Claramella;

public class WelcomeListener implements Listener {

    private final Claramella plugin;

    public WelcomeListener(Claramella plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(
            ChatColor.AQUA + "Welcome to the server, " +
            ChatColor.GOLD + player.getName() +
            ChatColor.AQUA + "!"
        );
        plugin.getLogger().info("Sent welcome message to " + player.getName());
    }
}
