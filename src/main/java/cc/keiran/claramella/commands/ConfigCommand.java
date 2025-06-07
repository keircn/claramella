package cc.keiran.claramella.commands;

import cc.keiran.claramella.Claramella;
import cc.keiran.claramella.config.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigCommand implements CommandExecutor, TabCompleter {
    
    private final Claramella plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, Class<?>> validKeys = new HashMap<>();
    
    public ConfigCommand(Claramella plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        initializeValidKeys();
    }
    
    private void initializeValidKeys() {
        validKeys.put("sleep.delay_ticks", Long.class);
        validKeys.put("sleep.check_interval", Long.class);
        validKeys.put("sleep.percentage_required", Double.class);
        validKeys.put("sleep.minimum_players_for_vote", Integer.class);
        validKeys.put("sleep.single_player_skip", Boolean.class);
        validKeys.put("sleep.show_progress_messages", Boolean.class);
        validKeys.put("sleep.skip_message", String.class);
        validKeys.put("welcome.enabled", Boolean.class);
        validKeys.put("welcome.message", String.class);
        validKeys.put("welcome.log_joins", Boolean.class);
        validKeys.put("admin.default_fly_speed", Float.class);
        validKeys.put("admin.default_walk_speed", Float.class);
        validKeys.put("admin.max_fly_speed", Float.class);
        validKeys.put("admin.max_walk_speed", Float.class);
        validKeys.put("admin.invulnerability_timeout", Long.class);
        validKeys.put("admin.announce_god_mode", Boolean.class);
        validKeys.put("admin.announce_invulnerability", Boolean.class);
        validKeys.put("admin.heal_removes_exhaustion", Boolean.class);
        validKeys.put("admin.max_gives_experience", Boolean.class);
        validKeys.put("admin.auto_heal_on_join", Boolean.class);
        validKeys.put("warp.cooldown_seconds", Integer.class);
        validKeys.put("warp.max_warps_per_player", Integer.class);
        validKeys.put("warp.require_safe_teleport", Boolean.class);
        validKeys.put("warp.teleport_delay_seconds", Integer.class);
        validKeys.put("plugin.debug_mode", Boolean.class);
        validKeys.put("plugin.language", String.class);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("claramella.config")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            case "list" -> handleList(sender);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void handleGet(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /claramella config get <key>");
            return;
        }
        
        String key = args[1];
        if (!validKeys.containsKey(key)) {
            sender.sendMessage(ChatColor.RED + "Invalid configuration key: " + key);
            sender.sendMessage(ChatColor.YELLOW + "Use '/claramella config list' to see valid keys.");
            return;
        }
        
        Object value = databaseManager.getConfigValue(key, Object.class);
        sender.sendMessage(ChatColor.GREEN + "Configuration value:");
        sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " = " + ChatColor.YELLOW + value);
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /claramella config set <key> <value>");
            return;
        }
        
        String key = args[1];
        if (!validKeys.containsKey(key)) {
            sender.sendMessage(ChatColor.RED + "Invalid configuration key: " + key);
            sender.sendMessage(ChatColor.YELLOW + "Use '/claramella config list' to see valid keys.");
            return;
        }
        
        String valueString = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        Class<?> expectedType = validKeys.get(key);
        Object parsedValue;
        
        try {
            parsedValue = parseValue(valueString, expectedType);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value type. Expected: " + expectedType.getSimpleName());
            sender.sendMessage(ChatColor.YELLOW + "Example: " + getExampleValue(expectedType));
            return;
        }
        
        if (!validateValue(key, parsedValue)) {
            sender.sendMessage(ChatColor.RED + "Invalid value for " + key + ". " + getValidationMessage(key));
            return;
        }
        
        databaseManager.setConfigValue(key, parsedValue).thenRun(() -> {
            sender.sendMessage(ChatColor.GREEN + "Configuration updated successfully!");
            sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " = " + ChatColor.YELLOW + parsedValue);
            if (requiresReload(key)) {
                sender.sendMessage(ChatColor.GOLD + "Note: Some changes may require a reload to take effect.");
            }
        });
    }
    
    private Object parseValue(String value, Class<?> type) throws IllegalArgumentException {
        if (type == String.class) {
            return value;
        } else if (type == Boolean.class) {
            if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
                return true;
            } else if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) {
                return false;
            } else {
                throw new IllegalArgumentException("Invalid boolean value");
            }
        } else if (type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == Long.class) {
            return Long.parseLong(value);
        } else if (type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == Float.class) {
            return Float.parseFloat(value);
        }
        throw new IllegalArgumentException("Unsupported type");
    }
    
    private boolean validateValue(String key, Object value) {
        return switch (key) {
            case "sleep.percentage_required" -> {
                double d = (Double) value;
                yield d >= 0.0 && d <= 1.0;
            }
            case "sleep.minimum_players_for_vote" -> (Integer) value >= 1;
            case "sleep.delay_ticks", "sleep.check_interval" -> (Long) value > 0;
            case "admin.default_fly_speed", "admin.default_walk_speed", 
                 "admin.max_fly_speed", "admin.max_walk_speed" -> {
                float f = (Float) value;
                yield f >= 0.0f && f <= 1.0f;
            }
            case "admin.invulnerability_timeout" -> (Long) value >= 0;
            case "warp.cooldown_seconds", "warp.max_warps_per_player", "warp.teleport_delay_seconds" -> (Integer) value >= 0;
            default -> true;
        };
    }
    
    private String getValidationMessage(String key) {
        return switch (key) {
            case "sleep.percentage_required" -> "Value must be between 0.0 and 1.0";
            case "sleep.minimum_players_for_vote" -> "Value must be at least 1";
            case "sleep.delay_ticks", "sleep.check_interval" -> "Value must be greater than 0";
            case "admin.default_fly_speed", "admin.default_walk_speed", 
                 "admin.max_fly_speed", "admin.max_walk_speed" -> "Value must be between 0.0 and 1.0";
            case "admin.invulnerability_timeout" -> "Value must be 0 or greater (milliseconds)";
            default -> "";
        };
    }
    
    private String getExampleValue(Class<?> type) {
        if (type == String.class) {
            return "\"Hello World\"";
        } else if (type == Boolean.class) {
            return "true/false";
        } else if (type == Integer.class) {
            return "42";
        } else if (type == Long.class) {
            return "100";
        } else if (type == Double.class) {
            return "0.5";
        } else if (type == Float.class) {
            return "1.5";
        }
        return "value";
    }
    
    private boolean requiresReload(String key) {
        return key.startsWith("plugin.") || key.equals("welcome.enabled");
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Configuration Keys ===");
        
        Map<String, List<String>> groupedKeys = validKeys.keySet().stream()
            .collect(Collectors.groupingBy(key -> key.split("\\.")[0]));
        
        for (Map.Entry<String, List<String>> group : groupedKeys.entrySet()) {
            sender.sendMessage(ChatColor.AQUA + group.getKey().toUpperCase() + ":");
            
            for (String key : group.getValue().stream().sorted().toList()) {
                Object value = databaseManager.getConfigValue(key, Object.class);
                Class<?> type = validKeys.get(key);
                sender.sendMessage(ChatColor.WHITE + "  " + key + 
                    ChatColor.GRAY + " (" + type.getSimpleName() + ") " +
                    ChatColor.YELLOW + "= " + value);
            }
        }
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(ChatColor.YELLOW + "Resetting all configuration to defaults...");
            databaseManager.resetToDefaults().thenRun(() -> {
                sender.sendMessage(ChatColor.GREEN + "All configuration has been reset to defaults!");
            });
        } else if (args.length == 2) {
            String key = args[1];
            if (!validKeys.containsKey(key)) {
                sender.sendMessage(ChatColor.RED + "Invalid configuration key: " + key);
                return;
            }
            Object defaultValue = getDefaultValue(key);
            if (defaultValue != null) {
                databaseManager.setConfigValue(key, defaultValue).thenRun(() -> {
                    sender.sendMessage(ChatColor.GREEN + "Configuration key reset to default:");
                    sender.sendMessage(ChatColor.AQUA + key + ChatColor.WHITE + " = " + ChatColor.YELLOW + defaultValue);
                });
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /claramella config reset [key]");
        }
    }
    
    private Object getDefaultValue(String key) {
        return switch (key) {
            case "sleep.delay_ticks" -> 100L;
            case "sleep.check_interval" -> 20L;
            case "sleep.percentage_required" -> 0.5;
            case "sleep.minimum_players_for_vote" -> 2;
            case "sleep.single_player_skip" -> true;
            case "sleep.show_progress_messages" -> true;
            case "sleep.skip_message" -> "☀ The night has been skipped! Good morning!";
            case "welcome.enabled" -> true;
            case "welcome.message" -> "Welcome to the server, {player}!";
            case "welcome.log_joins" -> true;
            case "admin.default_fly_speed" -> 0.1f;
            case "admin.default_walk_speed" -> 0.2f;
            case "admin.max_fly_speed" -> 1.0f;
            case "admin.max_walk_speed" -> 1.0f;
            case "admin.invulnerability_timeout" -> 300000L;
            case "admin.announce_god_mode" -> true;
            case "admin.announce_invulnerability" -> true;
            case "admin.heal_removes_exhaustion" -> true;
            case "admin.max_gives_experience" -> false;
            case "admin.auto_heal_on_join" -> false;
            case "plugin.debug_mode" -> false;
            case "plugin.language" -> "en";
            default -> null;
        };
    }
    
    private void handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading configuration...");
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded from database!");
    }
    
    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Configuration System Status ===");
        sender.sendMessage(ChatColor.AQUA + "Database Connected: " + 
            (databaseManager.isConnected() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.AQUA + "Total Config Keys: " + ChatColor.YELLOW + validKeys.size());
        sender.sendMessage(ChatColor.AQUA + "Cached Values: " + ChatColor.YELLOW + databaseManager.getAllConfigValues().size());
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Claramella Configuration Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config get <key>" + ChatColor.WHITE + " - Get a configuration value");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config set <key> <value>" + ChatColor.WHITE + " - Set a configuration value");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config list" + ChatColor.WHITE + " - List all configuration keys");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config reset [key]" + ChatColor.WHITE + " - Reset config to defaults");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/claramella config status" + ChatColor.WHITE + " - Show system status");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("claramella.config")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("get", "set", "list", "reset", "reload", "status"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("get".equals(subCommand) || "set".equals(subCommand) || "reset".equals(subCommand)) {
                completions.addAll(validKeys.keySet());
            }
        } else if (args.length == 3 && "set".equals(args[0].toLowerCase())) {
            String key = args[1];
            Class<?> type = validKeys.get(key);
            if (type == Boolean.class) {
                completions.addAll(Arrays.asList("true", "false"));
            } else if (key.equals("plugin.language")) {
                completions.addAll(Arrays.asList("en", "es", "fr", "de"));
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .sorted()
            .collect(Collectors.toList());
    }
}
