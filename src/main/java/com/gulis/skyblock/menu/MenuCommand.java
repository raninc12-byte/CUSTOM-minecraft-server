package com.gulis.skyblock.menu;

import com.gulis.skyblock.core.Skyblock;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the {@link MainMenuGUI}. Registered to {@code /menu} and {@code /hub}.
 */
public class MenuCommand implements CommandExecutor {

    private final Skyblock plugin;

    public MenuCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("hub")) {
            // Teleport to the hub spawn
            if (plugin.getHubManager() != null && plugin.getHubManager().getSpawnLocation() != null) {
                player.teleport(plugin.getHubManager().getSpawnLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to the hub.");
            } else {
                player.sendMessage(ChatColor.RED + "The hub is not available.");
            }
            return true;
        }

        // /menu — open the main menu
        MainMenuGUI gui = new MainMenuGUI(plugin, player);
        gui.open();
        plugin.getGuiManager().register(player, gui);
        return true;
    }
}
