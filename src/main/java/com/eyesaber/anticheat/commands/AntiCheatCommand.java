package com.eyesaber.anticheat.commands;

import com.eyesaber.anticheat.AntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AntiCheatCommand implements CommandExecutor {
    
    private final AntiCheatPlugin plugin;
    
    public AntiCheatCommand(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("anticheat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
                
            case "info":
                sendInfoMessage(sender);
                break;
                
            case "reload":
                plugin.getConfigManager().loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Sentinel configuration reloaded!");
                break;
                
            case "violations":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /anticheat violations <player>");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                
                sender.sendMessage(ChatColor.YELLOW + "Violations for " + target.getName() + ":");
                plugin.getViolationManager().getPlayerViolations(target).forEach((check, count) -> {
                    sender.sendMessage(ChatColor.GRAY + "- " + check + ": " + count);
                });
                break;
                
            case "clear":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /anticheat clear <player>");
                    return true;
                }
                
                Player clearTarget = Bukkit.getPlayer(args[1]);
                if (clearTarget == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                
                plugin.getViolationManager().clearViolations(clearTarget);
                sender.sendMessage(ChatColor.GREEN + "Cleared violations for " + clearTarget.getName());
                break;
                
            case "alerts":
                // Toggle alerts for the sender (if they're a player)
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    // This would need to be implemented in a separate alert toggle system
                    sender.sendMessage(ChatColor.YELLOW + "Alert toggle feature coming soon!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players can toggle alerts!");
                }
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand! Use /anticheat help");
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== Sentinel AntiCheat ==========");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat info" + ChatColor.WHITE + " - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat violations <player>" + ChatColor.WHITE + " - Show player violations");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat clear <player>" + ChatColor.WHITE + " - Clear player violations");
        sender.sendMessage(ChatColor.YELLOW + "/anticheat alerts" + ChatColor.WHITE + " - Toggle alerts");
        sender.sendMessage(ChatColor.GOLD + "======================================");
    }
    
    private void sendInfoMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== Sentinel AntiCheat ==========");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + plugin.getDescription().getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Server Version: " + ChatColor.WHITE + Bukkit.getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Players Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        sender.sendMessage(ChatColor.GOLD + "======================================");
    }
}