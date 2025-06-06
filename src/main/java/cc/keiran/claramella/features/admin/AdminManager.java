package cc.keiran.claramella.features.admin;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminManager implements Listener {

    private final Claramella plugin;
    private final DatabaseManager databaseManager;

    private final Set<UUID> invulnerablePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> godModePlayers = ConcurrentHashMap.newKeySet();

    public AdminManager(Claramella plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        loadInvulnerablePlayersFromDatabase();
    }

    private void loadInvulnerablePlayersFromDatabase() {
        String invulnPlayers = databaseManager.getConfigValue("admin.invulnerable_players", String.class, "");
        String godModePlayers = databaseManager.getConfigValue("admin.godmode_players", String.class, "");
        
        if (!invulnPlayers.isEmpty()) {
            for (String uuidStr : invulnPlayers.split(",")) {
                try {
                    this.invulnerablePlayers.add(UUID.fromString(uuidStr.trim()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in invulnerable players: " + uuidStr);
                }
            }
        }
        
        if (!godModePlayers.isEmpty()) {
            for (String uuidStr : godModePlayers.split(",")) {
                try {
                    this.godModePlayers.add(UUID.fromString(uuidStr.trim()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in god mode players: " + uuidStr);
                }
            }
        }
    }

    public boolean isInvulnerable(UUID playerId) {
        return invulnerablePlayers.contains(playerId) || godModePlayers.contains(playerId);
    }

    public void setInvulnerable(UUID playerId, boolean invulnerable) {
        if (invulnerable) {
            invulnerablePlayers.add(playerId);
        } else {
            invulnerablePlayers.remove(playerId);
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.setInvulnerable(invulnerable);
        }
        saveInvulnerablePlayersToDatabase();
    }

    public void setGodMode(UUID playerId, boolean godMode) {
        if (godMode) {
            godModePlayers.add(playerId);
        } else {
            godModePlayers.remove(playerId);
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.setInvulnerable(godMode);
            if (godMode) {
                healPlayer(player);
                feedPlayer(player);
            }
        }
        saveGodModePlayersToDatabase();
    }

    public void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFireTicks(0);
        player.clearActivePotionEffects();
        if (databaseManager.getConfigValue("admin.heal_removes_exhaustion", Boolean.class, true)) {
            player.setExhaustion(0);
        }
    }

    public void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
    }

    public void maxOutPlayer(Player player) {
        healPlayer(player);
        feedPlayer(player);
        if (databaseManager.getConfigValue("admin.max_gives_experience", Boolean.class, false)) {
            player.giveExp(1000);
        }
    }

    public void killPlayer(Player player) {
        if (isInvulnerable(player.getUniqueId())) {
            return;
        }
        player.setHealth(0);
    }

    public void teleportToPlayer(Player teleporter, Player target) {
        teleporter.teleport(target.getLocation());
    }

    public void teleportPlayerTo(Player player, Player destination) {
        player.teleport(destination.getLocation());
    }

    public void freezePlayer(UUID playerId, boolean frozen) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            if (frozen) {
                player.setWalkSpeed(0);
                player.setFlySpeed(0);
            } else {
                float defaultWalkSpeed = databaseManager.getConfigValue("admin.default_walk_speed", Float.class, 0.2f);
                float defaultFlySpeed = databaseManager.getConfigValue("admin.default_fly_speed", Float.class, 0.1f);
                player.setWalkSpeed(defaultWalkSpeed);
                player.setFlySpeed(defaultFlySpeed);
            }
        }
    }

    public void setPlayerSpeed(Player player, float speed) {
        float maxWalkSpeed = databaseManager.getConfigValue("admin.max_walk_speed", Float.class, 1.0f);
        float maxFlySpeed = databaseManager.getConfigValue("admin.max_fly_speed", Float.class, 1.0f);
        
        float clampedWalkSpeed = Math.max(0, Math.min(maxWalkSpeed, speed));
        float clampedFlySpeed = Math.max(0, Math.min(maxFlySpeed, speed));
        
        player.setWalkSpeed(clampedWalkSpeed);
        player.setFlySpeed(clampedFlySpeed);
    }

    public void toggleFly(Player player) {
        boolean canFly = !player.getAllowFlight();
        player.setAllowFlight(canFly);
        player.setFlying(canFly);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isInvulnerable(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (invulnerablePlayers.contains(playerId) || godModePlayers.contains(playerId)) {
            player.setInvulnerable(true);
        }
        if (databaseManager.getConfigValue("admin.auto_heal_on_join", Boolean.class, false)) {
            if (player.hasPermission("claramella.admin.autoheal")) {
                healPlayer(player);
                feedPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
    }

    public Set<UUID> getInvulnerablePlayers() {
        return new HashSet<>(invulnerablePlayers);
    }

    public Set<UUID> getGodModePlayers() {
        return new HashSet<>(godModePlayers);
    }

    public void clearAllInvulnerablePlayers() {
        for (UUID playerId : invulnerablePlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setInvulnerable(false);
            }
        }
        invulnerablePlayers.clear();
        for (UUID playerId : godModePlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setInvulnerable(false);
            }
        }
        godModePlayers.clear();
        saveInvulnerablePlayersToDatabase();
        saveGodModePlayersToDatabase();
    }

    private void saveInvulnerablePlayersToDatabase() {
        String playerList = invulnerablePlayers.stream()
            .map(UUID::toString)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        databaseManager.setConfigValue("admin.invulnerable_players", playerList);
    }
    
    private void saveGodModePlayersToDatabase() {
        String playerList = godModePlayers.stream()
            .map(UUID::toString)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        databaseManager.setConfigValue("admin.godmode_players", playerList);
    }
}
