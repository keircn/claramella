package cc.keiran.claramella.commands;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import cc.keiran.claramella.features.admin.AdminManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final Claramella plugin;
    private final DatabaseManager databaseManager;
    private final AdminManager adminManager;
    
    public AdminCommand(Claramella plugin, DatabaseManager databaseManager, AdminManager adminManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.adminManager = adminManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("claramella.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use admin commands.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "heal" -> handleHeal(sender, args);
            case "feed" -> handleFeed(sender, args);
            case "max" -> handleMax(sender, args);
            case "kill" -> handleKill(sender, args);
            case "invulnerable", "invuln" -> handleInvulnerable(sender, args);
            case "god", "godmode" -> handleGodMode(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "tphere" -> handleTeleportHere(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "speed" -> handleSpeed(sender, args);
            case "fly" -> handleFly(sender, args);
            case "list" -> handleList(sender);
            case "clear" -> handleClear(sender);
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void handleHeal(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot heal itself. Specify a player.");
                return;
            }
            adminManager.healPlayer(player);
            sender.sendMessage(ChatColor.GREEN + "You have been healed.");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            
            adminManager.healPlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been healed by " + sender.getName());
        }
    }
    
    private void handleFeed(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot feed itself. Specify a player.");
                return;
            }
            adminManager.feedPlayer(player);
            sender.sendMessage(ChatColor.GREEN + "You have been fed.");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            
            adminManager.feedPlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Fed " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been fed by " + sender.getName());
        }
    }
    
    private void handleMax(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot max itself. Specify a player.");
                return;
            }
            adminManager.maxOutPlayer(player);
            sender.sendMessage(ChatColor.GREEN + "You have been maxed out (healed + fed).");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            
            adminManager.maxOutPlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Maxed out " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You have been maxed out by " + sender.getName());
        }
    }
    
    private void handleKill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin kill <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        if (adminManager.isInvulnerable(target.getUniqueId())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is invulnerable and cannot be killed.");
            return;
        }
        
        adminManager.killPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Killed " + target.getName());
        target.sendMessage(ChatColor.RED + "You have been killed by " + sender.getName());
    }
    
    private void handleInvulnerable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin invulnerable <player> [true/false]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        boolean invulnerable;
        if (args.length >= 3) {
            invulnerable = Boolean.parseBoolean(args[2]);
        } else {
            invulnerable = !adminManager.isInvulnerable(target.getUniqueId());
        }
        
        adminManager.setInvulnerable(target.getUniqueId(), invulnerable);
        
        String status = invulnerable ? "enabled" : "disabled";
        sender.sendMessage(ChatColor.GREEN + "Invulnerability " + status + " for " + target.getName());
        target.sendMessage(ChatColor.AQUA + "Invulnerability has been " + status + " by " + sender.getName());
    }
    
    private void handleGodMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin godmode <player> [true/false]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        boolean godMode;
        if (args.length >= 3) {
            godMode = Boolean.parseBoolean(args[2]);
        } else {
            godMode = !adminManager.getGodModePlayers().contains(target.getUniqueId());
        }
        
        adminManager.setGodMode(target.getUniqueId(), godMode);
        
        String status = godMode ? "enabled" : "disabled";
        sender.sendMessage(ChatColor.GREEN + "God mode " + status + " for " + target.getName());
        target.sendMessage(ChatColor.GOLD + "God mode has been " + status + " by " + sender.getName());
    }
    
    private void handleTeleport(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin tp <player> <target>");
            return;
        }
        
        Player player = Bukkit.getPlayer(args[1]);
        Player target = Bukkit.getPlayer(args[2]);
        
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Target not found: " + args[2]);
            return;
        }
        
        adminManager.teleportToPlayer(player, target);
        sender.sendMessage(ChatColor.GREEN + "Teleported " + player.getName() + " to " + target.getName());
        player.sendMessage(ChatColor.AQUA + "You have been teleported to " + target.getName());
    }
    
    private void handleTeleportHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(ChatColor.RED + "Only players can use tphere command.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin tphere <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        adminManager.teleportPlayerTo(target, senderPlayer);
        sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to you");
        target.sendMessage(ChatColor.AQUA + "You have been teleported to " + sender.getName());
    }
    
    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin freeze <player> [true/false]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        boolean freeze;
        if (args.length >= 3) {
            freeze = Boolean.parseBoolean(args[2]);
        } else {
            freeze = target.getWalkSpeed() > 0;
        }
        
        adminManager.freezePlayer(target.getUniqueId(), freeze);
        
        String status = freeze ? "frozen" : "unfrozen";
        sender.sendMessage(ChatColor.GREEN + target.getName() + " has been " + status);
        target.sendMessage(ChatColor.AQUA + "You have been " + status + " by " + sender.getName());
    }
    
    private void handleSpeed(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin speed <player> <speed>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        try {
            float speed = Float.parseFloat(args[2]);
            adminManager.setPlayerSpeed(target, speed);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s speed to " + speed);
            target.sendMessage(ChatColor.AQUA + "Your speed has been set to " + speed + " by " + sender.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid speed value: " + args[2]);
        }
    }
    
    private void handleFly(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot toggle fly. Specify a player.");
                return;
            }
            adminManager.toggleFly(player);
            String status = player.getAllowFlight() ? "enabled" : "disabled";
            sender.sendMessage(ChatColor.GREEN + "Flight " + status);
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            
            adminManager.toggleFly(target);
            String status = target.getAllowFlight() ? "enabled" : "disabled";
            sender.sendMessage(ChatColor.GREEN + "Flight " + status + " for " + target.getName());
            target.sendMessage(ChatColor.AQUA + "Flight has been " + status + " by " + sender.getName());
        }
    }
    
    private void handleList(CommandSender sender) {
        Set<UUID> invulnerable = adminManager.getInvulnerablePlayers();
        Set<UUID> godMode = adminManager.getGodModePlayers();
        
        sender.sendMessage(ChatColor.GREEN + "=== Admin Status ===");
        
        if (!invulnerable.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "Invulnerable Players:");
            for (UUID uuid : invulnerable) {
                Player player = Bukkit.getPlayer(uuid);
                String name = player != null ? player.getName() : uuid.toString();
                sender.sendMessage(ChatColor.WHITE + "  - " + name);
            }
        }
        
        if (!godMode.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "God Mode Players:");
            for (UUID uuid : godMode) {
                Player player = Bukkit.getPlayer(uuid);
                String name = player != null ? player.getName() : uuid.toString();
                sender.sendMessage(ChatColor.WHITE + "  - " + name);
            }
        }
        
        if (invulnerable.isEmpty() && godMode.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No players have special admin status.");
        }
    }
    
    private void handleClear(CommandSender sender) {
        adminManager.clearAllInvulnerablePlayers();
        sender.sendMessage(ChatColor.GREEN + "Cleared all invulnerability and god mode states.");
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/admin heal [player]" + ChatColor.WHITE + " - Heal player to full health");
        sender.sendMessage(ChatColor.YELLOW + "/admin feed [player]" + ChatColor.WHITE + " - Fill player's hunger");
        sender.sendMessage(ChatColor.YELLOW + "/admin max [player]" + ChatColor.WHITE + " - Heal and feed player");
        sender.sendMessage(ChatColor.YELLOW + "/admin kill <player>" + ChatColor.WHITE + " - Kill target player");
        sender.sendMessage(ChatColor.YELLOW + "/admin invuln <player> [true/false]" + ChatColor.WHITE + " - Toggle invulnerability");
        sender.sendMessage(ChatColor.YELLOW + "/admin godmode <player> [true/false]" + ChatColor.WHITE + " - Toggle god mode");
        sender.sendMessage(ChatColor.YELLOW + "/admin tp <player> <target>" + ChatColor.WHITE + " - Teleport player to target");
        sender.sendMessage(ChatColor.YELLOW + "/admin tphere <player>" + ChatColor.WHITE + " - Teleport player to you");
        sender.sendMessage(ChatColor.YELLOW + "/admin freeze <player> [true/false]" + ChatColor.WHITE + " - Freeze/unfreeze player");
        sender.sendMessage(ChatColor.YELLOW + "/admin speed <player> <speed>" + ChatColor.WHITE + " - Set player speed (0-1)");
        sender.sendMessage(ChatColor.YELLOW + "/admin fly [player]" + ChatColor.WHITE + " - Toggle flight");
        sender.sendMessage(ChatColor.YELLOW + "/admin list" + ChatColor.WHITE + " - List players with admin status");
        sender.sendMessage(ChatColor.YELLOW + "/admin clear" + ChatColor.WHITE + " - Clear all admin statuses");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("claramella.admin")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("heal", "feed", "max", "kill", "invulnerable", "godmode", 
                "tp", "tphere", "freeze", "speed", "fly", "list", "clear"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("heal", "feed", "max", "kill", "invulnerable", "godmode", 
                "tphere", "freeze", "speed", "fly").contains(subCommand)) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            } else if ("tp".equals(subCommand)) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ("tp".equals(subCommand)) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            } else if (Arrays.asList("invulnerable", "godmode", "freeze").contains(subCommand)) {
                completions.addAll(Arrays.asList("true", "false"));
            } else if ("speed".equals(subCommand)) {
                completions.addAll(Arrays.asList("0.1", "0.2", "0.5", "1.0"));
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .sorted()
            .collect(Collectors.toList());
    }
}
