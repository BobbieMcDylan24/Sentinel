package com.sentinelac.commands;

import com.sentinelac.SentinelAC;
import com.sentinelac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SentinelCommand implements CommandExecutor, TabCompleter {

    private final SentinelAC plugin;
    private final String prefix = ChatColor.RED + "SentinelAC " + ChatColor.GRAY + "» " + ChatColor.WHITE;
    private final String noPermission = prefix + ChatColor.RED + "You don't have permission to execute this command.";

    public SentinelCommand(SentinelAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "alerts":
                handleAlerts(sender, args);
                break;
            case "violations":
                handleViolations(sender, args);
                break;
            case "stats":
                handleStats(sender);
                break;
            case "debug":
                handleDebug(sender, args);
                break;
            case "webhook":
                handleWebhook(sender, args);
                break;
            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown subcommand: " + subCommand);
                sender.sendMessage(prefix + "Type " + ChatColor.YELLOW + "/sentinel help" + ChatColor.WHITE + " for available commands.");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.RED + "                    SentinelAC Commands");
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel help" + ChatColor.GRAY + " - Show this help menu");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel info [player]" + ChatColor.GRAY + " - Show player information");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel alerts <toggle|test>" + ChatColor.GRAY + " - Manage alerts");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel violations <player> [check|reset]" + ChatColor.GRAY + " - Manage violations");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel stats" + ChatColor.GRAY + " - Show plugin statistics");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel debug <on|off|player>" + ChatColor.GRAY + " - Debug utilities");
        sender.sendMessage(ChatColor.YELLOW + "/sentinel webhook <test|url>" + ChatColor.GRAY + " - Webhook management");
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinelac.command.info")) {
            sender.sendMessage(noPermission);
            return;
        }

        if (args.length < 2) {
            if (sender instanceof Player) {
                showPlayerInfo(sender, (Player) sender);
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Please specify a player name.");
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        showPlayerInfo(sender, target);
    }

    private void showPlayerInfo(CommandSender sender, Player target) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(prefix + ChatColor.RED + "No data found for " + target.getName());
            return;
        }

        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.RED + "                Player Info: " + target.getName());
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + data.getUUID());
        sender.sendMessage(ChatColor.YELLOW + "Ping: " + ChatColor.WHITE + data.getPing() + "ms");
        sender.sendMessage(ChatColor.YELLOW + "TPS: " + ChatColor.WHITE + String.format("%.1f", data.getServerTPS()));
        sender.sendMessage(ChatColor.YELLOW + "Total Violations: " + ChatColor.WHITE + data.getTotalViolations());
        sender.sendMessage(ChatColor.YELLOW + "Exempt: " + ChatColor.WHITE + (data.isExempt() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Lagging: " + ChatColor.WHITE + (data.isLagging() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Session Duration: " + ChatColor.WHITE + formatDuration(data.getSessionDuration()));
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("sentinelac.command.reload")) {
            sender.sendMessage(noPermission);
            return;
        }

        sender.sendMessage(prefix + "Reloading configuration...");
        
        try {
            plugin.getConfigManager().reload();
            plugin.getCheckManager().reloadChecks();
            plugin.getViolationManager().reload();
            plugin.getAlertManager().reload();
            
            sender.sendMessage(prefix + ChatColor.GREEN + "Configuration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage(prefix + ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
        }
    }

    private void handleAlerts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinelac.command.alerts")) {
            sender.sendMessage(noPermission);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /sentinel alerts <toggle|test>");
            return;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "toggle":
                boolean currentState = plugin.getAlertManager().areAlertsEnabled();
                plugin.getAlertManager().setAlertsEnabled(!currentState);
                sender.sendMessage(prefix + "Alerts " + (currentState ? ChatColor.RED + "disabled" : ChatColor.GREEN + "enabled"));
                break;
            case "test":
                sender.sendMessage(prefix + "Sending test alert...");
                plugin.getAlertManager().sendStaffMessage("&eSentinelAC &7» &fTest alert sent by " + sender.getName());
                break;
            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown action: " + action);
                break;
        }
    }

    private void handleViolations(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinelac.command.violations")) {
            sender.sendMessage(noPermission);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /sentinel violations <player> [check|reset]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        if (args.length == 2) {
            // Show violation info
            showViolationInfo(sender, target);
            return;
        }

        String action = args[2].toLowerCase();
        if (action.equals("reset")) {
            String checkName = args.length > 3 ? args[3] : "all";
            plugin.getViolationManager().resetPlayerViolations(target, checkName);
            sender.sendMessage(prefix + ChatColor.GREEN + "Reset violations for " + target.getName() + 
                (checkName.equals("all") ? "" : " (" + checkName + ")"));
        } else {
            // Show specific check violations
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
            if (data != null) {
                int violations = data.getViolations(action);
                sender.sendMessage(prefix + target.getName() + " has " + violations + " violations for " + action);
            }
        }
    }

    private void showViolationInfo(CommandSender sender, Player target) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(prefix + ChatColor.RED + "No data found for " + target.getName());
            return;
        }

        sender.sendMessage(prefix + "Violation info for " + ChatColor.YELLOW + target.getName());
        sender.sendMessage(prefix + "Total violations: " + ChatColor.RED + data.getTotalViolations());
        // In a full implementation, you'd iterate through all check violations here
    }

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("sentinelac.command.stats")) {
            sender.sendMessage(noPermission);
            return;
        }

        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.RED + "                   SentinelAC Statistics");
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.YELLOW + "Players tracked: " + ChatColor.WHITE + plugin.getPlayerDataManager().getPlayerCount());
        sender.sendMessage(ChatColor.YELLOW + "Registered checks: " + ChatColor.WHITE + plugin.getCheckManager().getRegisteredCheckCount());
        sender.sendMessage(ChatColor.YELLOW + "Alerts enabled: " + ChatColor.WHITE + (plugin.getAlertManager().areAlertsEnabled() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Webhook enabled: " + ChatColor.WHITE + (plugin.getAlertManager().isWebhookEnabled() ? "Yes" : "No"));
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        sender.sendMessage(ChatColor.YELLOW + "Memory usage: " + ChatColor.WHITE + (usedMemory / 1024 / 1024) + "MB");
        
        sender.sendMessage(ChatColor.RED + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinelac.command.debug")) {
            sender.sendMessage(noPermission);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /sentinel debug <on|off|player>");
            return;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "on":
                // Enable debug mode (would modify config)
                sender.sendMessage(prefix + ChatColor.GREEN + "Debug mode enabled");
                break;
            case "off":
                // Disable debug mode
                sender.sendMessage(prefix + ChatColor.RED + "Debug mode disabled");
                break;
            default:
                // Show debug info for player
                Player target = Bukkit.getPlayer(action);
                if (target == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Player not found: " + action);
                    return;
                }
                showPlayerInfo(sender, target);
                break;
        }
    }

    private void handleWebhook(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinelac.command.webhook")) {
            sender.sendMessage(noPermission);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /sentinel webhook <test|url>");
            return;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "test":
                sender.sendMessage(prefix + "Testing webhook...");
                plugin.getAlertManager().testWebhook();
                break;
            case "url":
                if (args.length < 3) {
                    sender.sendMessage(prefix + "Current webhook URL: " + 
                        (plugin.getAlertManager().getWebhookUrl() != null ? plugin.getAlertManager().getWebhookUrl() : "Not set"));
                } else {
                    plugin.getAlertManager().setWebhookUrl(args[2]);
                    sender.sendMessage(prefix + ChatColor.GREEN + "Webhook URL updated");
                }
                break;
            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown webhook action: " + action);
                break;
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "info", "reload", "alerts", "violations", "stats", "debug", "webhook");
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "info":
                case "violations":
                case "debug":
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "alerts":
                    return Arrays.asList("toggle", "test").stream()
                        .filter(action -> action.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "webhook":
                    return Arrays.asList("test", "url").stream()
                        .filter(action -> action.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("violations")) {
            return Arrays.asList("reset").stream()
                .filter(action -> action.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
