package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the {@code /oneblock} (alias {@code /ob}) command.
 *
 * <p>Subcommands:</p>
 * <ul>
 *   <li>{@code /oneblock} or {@code /oneblock start} — create a new OneBlock</li>
 *   <li>{@code /oneblock home} — teleport to your OneBlock</li>
 *   <li>{@code /oneblock phase} — show your current phase and progress</li>
 * </ul>
 */
public class OneBlockCommand implements CommandExecutor {

    private final Skyblock plugin;

    public OneBlockCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("OneBlock commands can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        String sub = args.length == 0 ? "start" : args[0].toLowerCase();

        switch (sub) {
            case "start":
            case "create":
                return handleStart(player);
            case "home":
            case "go":
                return handleHome(player);
            case "phase":
            case "info":
                return handlePhase(player);
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleStart(Player player) {
        if (plugin.getOneBlockManager().getOneBlock(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You already have a OneBlock. Use /oneblock home.");
            return true;
        }
        if (plugin.getOneBlockManager().createOneBlock(player)) {
            player.sendMessage(ChatColor.GREEN + "Welcome to your OneBlock!");
            player.sendMessage(ChatColor.YELLOW + "Mine the block to get resources and progress through phases.");
        } else {
            player.sendMessage(ChatColor.RED + "Could not create your OneBlock.");
        }
        return true;
    }

    private boolean handleHome(Player player) {
        OneBlockManager.OneBlockData data = plugin.getOneBlockManager().getOneBlock(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "You do not have a OneBlock. Use /oneblock start.");
            return true;
        }
        player.teleport(data.getLocation().clone().add(0.5, 1, 0.5));
        player.sendMessage(ChatColor.GREEN + "Teleported to your OneBlock.");
        return true;
    }

    private boolean handlePhase(Player player) {
        OneBlockManager.OneBlockData data = plugin.getOneBlockManager().getOneBlock(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ChatColor.RED + "You do not have a OneBlock. Use /oneblock start.");
            return true;
        }
        player.sendMessage(ChatColor.GOLD + "=== OneBlock Status ===");
        player.sendMessage(ChatColor.YELLOW + "Phase: " + ChatColor.WHITE
                + OneBlockManager.getPhaseName(data.getPhase()) + " (" + data.getPhase() + ")");
        player.sendMessage(ChatColor.YELLOW + "Blocks mined: " + ChatColor.WHITE + data.getMined());
        int nextPhaseAt = (data.getPhase() + 1) * 200;
        player.sendMessage(ChatColor.YELLOW + "Next phase in: " + ChatColor.WHITE
                + (nextPhaseAt - data.getMined()) + " blocks");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== OneBlock Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/oneblock start" + ChatColor.GRAY + " - Create your OneBlock");
        player.sendMessage(ChatColor.YELLOW + "/oneblock home" + ChatColor.GRAY + " - Teleport to your OneBlock");
        player.sendMessage(ChatColor.YELLOW + "/oneblock phase" + ChatColor.GRAY + " - Show your progress");
    }
}
