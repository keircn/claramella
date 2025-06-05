package cc.keiran.hellopaper;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class HelloPaper extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("HelloPaper has been enabled! Ready to greet players.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HelloPaper has been disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(
            ChatColor.AQUA + "Hello from PaperMC, " + 
            ChatColor.GOLD + player.getName() + 
            ChatColor.AQUA + "!"
        );
    }
}
