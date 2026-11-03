package dev.frozenworld.command;

import dev.frozenworld.FrozenWorldPlugin;
import dev.frozenworld.menu.WinterMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class FrozenWorldCommand implements CommandExecutor, TabCompleter {
    private final FrozenWorldPlugin plugin;
    private final WinterMenu menu;

    public FrozenWorldCommand(FrozenWorldPlugin plugin, WinterMenu menu) {
        this.plugin = plugin;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frozenworld.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав: frozenworld.admin");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }
        menu.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
