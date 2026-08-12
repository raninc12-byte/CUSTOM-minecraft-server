package com.gulis.skyblock.island;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the {@code /island} (alias {@code /is}) command and all subcommands.
 *
 * <p>Supported subcommands:</p>
 * <ul>
 *   <li>{@code /is create} — create a new island</li>
 *   <li>{@code /is home} — teleport to your island</li>
 *   <li>{@code /is invite <player>} — invite a player to your island</li>
 *   <li>{@code /is accept <player>} — accept a pending invite</li>
 *   <li>{@code /is leave} — leave your current island (members only)</li>
 *   <li>{@code /is level} — show your island level</li>
 *   <li>{@code /is top} — leaderboard of top islands</li>
 *   <li>{@code /is delete} — delete your island (owner only)</li>
 * </ul>
 */
public class IslandCommand implements CommandExecutor, TabCompleter {

    private final Skyblock plugin;
    // Pending invites: target UUID -> owner UUID
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    public IslandCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Island commands can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        String sub = args.length == 0 ? "help" : args[0].toLowerCase();

        switch (sub) {
            case "create":
                return handleCreate(player);
            case "home":
            case "go":
                return handleHome(player);
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player, args);
            case "leave":
                return handleLeave(player);
            case "level":
                return handleLevel(player);
            case "top":
                return handleTop(player);
            case "delete":
            case "reset":
                return handleDelete(player);
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player) {
        if (plugin.getIslandManager().getIsland(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You already have an island. Use /is delete first to make a new one.");
            return true;
        }
        Island island = plugin.getIslandManager().createIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Could not create your island. See console for details.");
            return true;
        }
        player.teleport(island.getHome());
        player.sendMessage(ChatColor.GREEN + "Welcome to your new island!");
        return true;
    }

    private boolean handleHome(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            // Also allow members to teleport home
            for (Island i : plugin.getIslandManager().getAllIslands().values()) {
                if (i.isMember(player.getUniqueId())) {
                    island = i;
                    break;
                }
            }
        }
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You do not have an island. Use /is create to make one.");
            return true;
        }
        player.teleport(island.getHome());
        player.sendMessage(ChatColor.GREEN + "Teleported to your island.");
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /is invite <player>");
            return true;
        }
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You do not own an island.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not online: " + args[1]);
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return true;
        }
        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        target.sendMessage(ChatColor.GREEN + player.getName() + " invited you to their island.");
        target.sendMessage(ChatColor.YELLOW + "Use /is accept " + player.getName() + " to join.");
        player.sendMessage(ChatColor.GREEN + "Invite sent to " + target.getName() + ".");
        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        UUID inviterUuid = pendingInvites.remove(player.getUniqueId());
        if (inviterUuid == null) {
            player.sendMessage(ChatColor.RED + "You have no pending invites.");
            return true;
        }
        Island island = plugin.getIslandManager().getIsland(inviterUuid);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "That island no longer exists.");
            return true;
        }
        island.addMember(player.getUniqueId());
        plugin.getIslandManager().saveIsland(island);
        player.sendMessage(ChatColor.GREEN + "You joined " + Bukkit.getOfflinePlayer(inviterUuid).getName() + "'s island.");
        OfflinePlayer inviter = Bukkit.getOfflinePlayer(inviterUuid);
        if (inviter.isOnline()) {
            ((Player) inviter).sendMessage(ChatColor.GREEN + player.getName() + " joined your island.");
        }
        return true;
    }

    private boolean handleLeave(Player player) {
        for (Island island : plugin.getIslandManager().getAllIslands().values()) {
            if (island.getMembers().contains(player.getUniqueId())) {
                island.removeMember(player.getUniqueId());
                plugin.getIslandManager().saveIsland(island);
                player.sendMessage(ChatColor.GREEN + "You left the island.");
                return true;
            }
        }
        player.sendMessage(ChatColor.RED + "You are not a member of any island.");
        return true;
    }

    private boolean handleLevel(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You do not own an island.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Your island level: " + ChatColor.GOLD + island.getLevel());
        return true;
    }

    private boolean handleTop(Player player) {
        List<Island> sorted = new ArrayList<>(plugin.getIslandManager().getAllIslands().values());
        sorted.sort((a, b) -> Double.compare(b.getLevel(), a.getLevel()));
        player.sendMessage(ChatColor.GOLD + "=== Island Top ===");
        int max = Math.min(10, sorted.size());
        for (int i = 0; i < max; i++) {
            Island island = sorted.get(i);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwnerUuid());
            player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " "
                    + ChatColor.WHITE + owner.getName()
                    + ChatColor.GRAY + " - Level " + ChatColor.GOLD + island.getLevel());
        }
        if (sorted.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No islands yet.");
        }
        return true;
    }

    private boolean handleDelete(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You do not own an island.");
            return true;
        }
        plugin.getIslandManager().deleteIsland(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Your island has been deleted.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Island Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/is create" + ChatColor.GRAY + " - Create a new island");
        player.sendMessage(ChatColor.YELLOW + "/is home" + ChatColor.GRAY + " - Teleport to your island");
        player.sendMessage(ChatColor.YELLOW + "/is invite <player>" + ChatColor.GRAY + " - Invite a player");
        player.sendMessage(ChatColor.YELLOW + "/is accept <player>" + ChatColor.GRAY + " - Accept an invite");
        player.sendMessage(ChatColor.YELLOW + "/is leave" + ChatColor.GRAY + " - Leave your current island");
        player.sendMessage(ChatColor.YELLOW + "/is level" + ChatColor.GRAY + " - Show your island level");
        player.sendMessage(ChatColor.YELLOW + "/is top" + ChatColor.GRAY + " - Island leaderboard");
        player.sendMessage(ChatColor.YELLOW + "/is delete" + ChatColor.GRAY + " - Delete your island");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create", "home", "invite", "accept", "leave", "level", "top", "delete", "help");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return new ArrayList<>();
    }
}
