package cc.keiran.claramella.features.sleep;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import cc.keiran.claramella.Claramella;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SleepListener implements Listener {

    private final Claramella plugin;
    private final long SLEEP_DELAY_TICKS = 100L;
    private final long SLEEP_CHECK_INTERVAL = 20L;
    private final double SLEEP_PERCENTAGE_REQUIRED = 0.5;
    private final int MINIMUM_PLAYERS_FOR_VOTE = 2;
    
    private final Map<UUID, Set<UUID>> sleepingPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> activeSleepTasks = new HashMap<>();

    public SleepListener(Claramella plugin) {
        this.plugin = plugin;
    }

    private boolean isValidSleepTime(World world) {
        long time = world.getTime();
        boolean isNight = time >= 12542 && time <= 23459;
        boolean isStormy = world.hasStorm() || world.isThundering();
        return isNight || isStormy;
    }


    private int getRequiredSleepers(World world) {
        int onlinePlayers = world.getPlayers().size();
        if (onlinePlayers <= MINIMUM_PLAYERS_FOR_VOTE) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(onlinePlayers * SLEEP_PERCENTAGE_REQUIRED));
    }

    private int getCurrentSleepers(World world) {
        return sleepingPlayers.getOrDefault(world.getUID(), new HashSet<>()).size();
    }

    private void addSleepingPlayer(Player player) {
        UUID worldId = player.getWorld().getUID();
        sleepingPlayers.computeIfAbsent(worldId, k -> new HashSet<>()).add(player.getUniqueId());
    }

    private void removeSleepingPlayer(Player player) {
        UUID worldId = player.getWorld().getUID();
        Set<UUID> sleepers = sleepingPlayers.get(worldId);
        if (sleepers != null) {
            sleepers.remove(player.getUniqueId());
            if (sleepers.isEmpty()) {
                sleepingPlayers.remove(worldId);
                BukkitTask task = activeSleepTasks.remove(worldId);
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
        }
    }

    private void skipNight(World world) {
        world.setTime(0); // Set to dawn
        world.setStorm(false);
        world.setThundering(false);
        
        sleepingPlayers.remove(world.getUID());
        
        BukkitTask task = activeSleepTasks.remove(world.getUID());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        
        Bukkit.broadcastMessage(
            ChatColor.GREEN + "☀ " + ChatColor.YELLOW + "The night has been skipped! Good morning!"
        );
    }

    private void startSleepMonitoring(World world) {
        UUID worldId = world.getUID();
        
        if (activeSleepTasks.containsKey(worldId)) {
            return;
        }
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> sleepers = sleepingPlayers.get(worldId);
                if (sleepers == null || sleepers.isEmpty()) {
                    activeSleepTasks.remove(worldId);
                    this.cancel();
                    return;
                }
                
                sleepers.removeIf(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    return player == null || !player.isOnline() || !player.isSleeping();
                });
                
                if (sleepers.isEmpty()) {
                    sleepingPlayers.remove(worldId);
                    activeSleepTasks.remove(worldId);
                    this.cancel();
                    return;
                }
                
                int currentSleepers = sleepers.size();
                int requiredSleepers = getRequiredSleepers(world);
                
                if (currentSleepers >= requiredSleepers) {
                    skipNight(world);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, SLEEP_DELAY_TICKS, SLEEP_CHECK_INTERVAL);
        
        activeSleepTasks.put(worldId, task);
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!isValidSleepTime(world)) {
            player.sendMessage(
                ChatColor.YELLOW + "☽ You can only sleep at night or during a thunderstorm."
            );
            return;
        }
        
        addSleepingPlayer(player);
        
        int currentSleepers = getCurrentSleepers(world);
        int requiredSleepers = getRequiredSleepers(world);
        int totalPlayers = world.getPlayers().size();
        
        if (currentSleepers >= requiredSleepers) {
            Bukkit.broadcastMessage(
                ChatColor.GOLD + "☽ " + player.getName() + 
                ChatColor.YELLOW + " has gone to bed. " +
                ChatColor.GREEN + "Enough players are sleeping - night will be skipped soon!"
            );
        } else {
            Bukkit.broadcastMessage(
                ChatColor.GOLD + "☽ " + player.getName() + 
                ChatColor.YELLOW + " has gone to bed. " +
                ChatColor.AQUA + "(" + currentSleepers + "/" + requiredSleepers + 
                " players sleeping, " + totalPlayers + " online)"
            );
        }
        
        startSleepMonitoring(world);
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        removeSleepingPlayer(player);
        
        World world = player.getWorld();
        int currentSleepers = getCurrentSleepers(world);
        
        if (currentSleepers > 0) {
            int requiredSleepers = getRequiredSleepers(world);
            Bukkit.broadcastMessage(
                ChatColor.GOLD + player.getName() + 
                ChatColor.YELLOW + " left their bed. " +
                ChatColor.AQUA + "(" + currentSleepers + "/" + requiredSleepers + " players sleeping)"
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeSleepingPlayer(event.getPlayer());
    }
}
