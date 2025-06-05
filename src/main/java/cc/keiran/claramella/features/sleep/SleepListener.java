package cc.keiran.claramella.features.sleep;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.scheduler.BukkitRunnable;
import cc.keiran.claramella.Claramella;

public class SleepListener implements Listener {

    private final Claramella plugin;
    private final long SLEEP_DELAY_TICKS = 100L;

    public SleepListener(Claramella plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!world.isThundering() && (world.getTime() < 12541 || world.getTime() > 23458)) {
            player.sendMessage(ChatColor.YELLOW + "You can only sleep at night or during a thunderstorm.");
            return;
        }
        
        Bukkit.broadcastMessage(
            ChatColor.GOLD + player.getName() +
            ChatColor.YELLOW + " has gone to bed. Sweet dreams!"
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isSleeping()) {
                    world.setTime(0);
                    world.setStorm(false);
                    world.setThundering(false);
                    Bukkit.broadcastMessage(
                        ChatColor.GREEN + "The night has been skipped."
                    );
                }
            }
        }.runTaskLater(plugin, SLEEP_DELAY_TICKS);
    }
}
