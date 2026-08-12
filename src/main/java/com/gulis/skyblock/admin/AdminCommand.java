package com.gulis.skyblock.admin;

import com.gulis.skyblock.core.Skyblock;
import com.gulis.skyblock.gui.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the {@link AdminGUI} for the player. Registered to both {@code /admin}
 * and {@code /panel} in {@link Skyblock#onEnable()}.
 */
public class AdminCommand implements CommandExecutor {

    private final Skyblock plugin;

    public AdminCommand(Skyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Admin commands can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("skyblock.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use the admin panel.");
            return true;
        }
        Player player = (Player) sender;
        AdminGUI gui = new AdminGUI(plugin, player);
        gui.open();
        plugin.getGuiManager().register(player, gui);
        return true;
    }
}
