package cc.keiran.claramella.commands;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import cc.keiran.claramella.features.warps.Warp;
import cc.keiran.claramella.features.warps.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {
    
    private final Claramella plugin;
    private final DatabaseManager databaseManager;
    private final WarpManager warpManager;
    
    public WarpCommand(Claramella plugin, DatabaseManager databaseManager, WarpManager warpManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.warpManager = warpManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("claramella.warp.list")) {
                handleList(sender);
            } else {
                sendHelp(sender);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create", "set" -> handleCreate(sender, args);
            case "delete", "remove", "del" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "help" -> sendHelp(sender);
            default -> handleWarp(sender, subCommand);
        }
        
        return true;
    }
    
    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("claramella.warp.create")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to create warps.");
            return;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create warps.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warp create <name>");
            return;
        }
        
        String warpName = args[1];
        
        if (!isValidWarpName(warpName)) {
            sender.sendMessage(ChatColor.RED + "Invalid warp name. Use only letters, numbers, and underscores.");
            return;
        }
        
        if (warpManager.warpExists(warpName)) {
            sender.sendMessage(ChatColor.RED + "A warp with that name already exists.");
            return;
        }
        
        warpManager.createWarp(warpName, player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' created successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to create warp. A warp with that name may already exist.");
            }
        });
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("claramella.warp.delete")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to delete warps.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warp delete <name>");
            return;
        }
        
        String warpName = args[1];
        
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage(ChatColor.RED + "No warp found with that name.");
            return;
        }
        
        warpManager.deleteWarp(warpName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' deleted successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to delete warp.");
            }
        });
    }
    
    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("claramella.warp.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to list warps.");
            return;
        }
        
        Set<String> warpNames = warpManager.getWarpNames();
        
        if (warpNames.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No warps have been created yet.");
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== Available Warps ===");
        List<String> sortedWarps = warpNames.stream().sorted().collect(Collectors.toList());
        
        for (String warpName : sortedWarps) {
            Warp warp = warpManager.getWarp(warpName);
            String status = warp.isWorldLoaded() ? 
                ChatColor.GREEN + "✓" : 
                ChatColor.RED + "✗ (world unloaded)";
            
            sender.sendMessage(ChatColor.AQUA + "  " + warpName + " " + status);
        }
        
        sender.sendMessage(ChatColor.GRAY + "Use '/warp <name>' to teleport to a warp");
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("claramella.warp.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view warp info.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warp info <name>");
            return;
        }
        
        String warpName = args[1];
        Warp warp = warpManager.getWarp(warpName);
        
        if (warp == null) {
            sender.sendMessage(ChatColor.RED + "No warp found with that name.");
            return;
        }
        
        sender.sendMessage(ChatColor.GREEN + "=== Warp Info: " + warp.getName() + " ===");
        sender.sendMessage(ChatColor.AQUA + "World: " + ChatColor.WHITE + 
            (warp.isWorldLoaded() ? 
                Bukkit.getWorld(warp.getWorldId()).getName() : 
                warp.getWorldId().toString() + " (unloaded)"));
        sender.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
            String.format("%.1f, %.1f, %.1f", warp.getX(), warp.getY(), warp.getZ()));
        
        OfflinePlayer creator = Bukkit.getOfflinePlayer(warp.getCreatedBy());
        String creatorName = creator.getName() != null ? creator.getName() : warp.getCreatedBy().toString();
        sender.sendMessage(ChatColor.AQUA + "Created by: " + ChatColor.WHITE + creatorName);
        sender.sendMessage(ChatColor.AQUA + "Created: " + ChatColor.WHITE + new Date(warp.getCreatedAt()));
    }
    
    private void handleWarp(CommandSender sender, String warpName) {
        if (!sender.hasPermission("claramella.warp.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use warps.");
            return;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can warp.");
            return;
        }
        
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage(ChatColor.RED + "No warp found with that name. Use '/warp list' to see available warps.");
            return;
        }
        
        Warp warp = warpManager.getWarp(warpName);
        if (!warp.isWorldLoaded()) {
            sender.sendMessage(ChatColor.RED + "Cannot warp to '" + warpName + "' - the world is not loaded.");
            return;
        }
        
        if (warpManager.teleportToWarp(player, warpName)) {
            sender.sendMessage(ChatColor.GREEN + "Warped to '" + warpName + "'!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to warp. The destination may be unsafe.");
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Warp Commands ===");
        
        if (sender.hasPermission("claramella.warp.use")) {
            sender.sendMessage(ChatColor.YELLOW + "/warp <name>" + ChatColor.WHITE + " - Teleport to a warp");
        }
        if (sender.hasPermission("claramella.warp.list")) {
            sender.sendMessage(ChatColor.YELLOW + "/warp list" + ChatColor.WHITE + " - List all warps");
        }
        if (sender.hasPermission("claramella.warp.info")) {
            sender.sendMessage(ChatColor.YELLOW + "/warp info <name>" + ChatColor.WHITE + " - Show warp information");
        }
        if (sender.hasPermission("claramella.warp.create")) {
            sender.sendMessage(ChatColor.YELLOW + "/warp create <name>" + ChatColor.WHITE + " - Create a new warp");
        }
        if (sender.hasPermission("claramella.warp.delete")) {
            sender.sendMessage(ChatColor.YELLOW + "/warp delete <name>" + ChatColor.WHITE + " - Delete a warp");
        }
    }
    
    private boolean isValidWarpName(String name) {
        return name.matches("^[a-zA-Z0-9_]+$") && name.length() <= 32;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("claramella.warp.create")) {
                completions.add("create");
            }
            if (sender.hasPermission("claramella.warp.delete")) {
                completions.add("delete");
            }
            if (sender.hasPermission("claramella.warp.list")) {
                completions.add("list");
            }
            if (sender.hasPermission("claramella.warp.info")) {
                completions.add("info");
            }
            completions.add("help");
            
            if (sender.hasPermission("claramella.warp.use")) {
                completions.addAll(warpManager.getWarpNames());
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (("delete".equals(subCommand) || "info".equals(subCommand)) && 
                sender.hasPermission("claramella.warp." + ("delete".equals(subCommand) ? "delete" : "info"))) {
                completions.addAll(warpManager.getWarpNames());
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .sorted()
            .collect(Collectors.toList());
    }
}
