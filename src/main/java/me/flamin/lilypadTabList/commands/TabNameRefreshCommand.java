package me.flamin.lilypadTabList.commands;

import me.flamin.lilypadTabList.lilypadTabList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class TabNameRefreshCommand implements CommandExecutor {
    private final lilypadTabList plugin;

    public TabNameRefreshCommand(lilypadTabList plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args ){
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            return player.hasPermission("lilypadTabList.refresh") && tablist(commandSender, args);
        } else {
            return tablist(commandSender, args);
        }
    }

    private boolean tablist(CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            refreshAllPlayers();
            return true;
        } else if (args.length == 1) {
            refreshFilteredPlayers(args[0]);
            return true;
        } else {
            commandSender.sendMessage("Invalid number of arguments.");
            return false;
        }
    }

    private void refreshFilteredPlayers(String matcher) {
        for (Map.Entry<String, String> entry : plugin.formattedNames.entrySet()) {
            String player = entry.getKey();
            if (entry.getKey().startsWith(matcher)) {
                entry.setValue(
                        plugin.formatPlayerName(player, plugin.lilypadOnlinePlayersHandler.getPlayer(player).getWorld())
                );
            }
        }
    }

    private void refreshAllPlayers() {
        for (Map.Entry<String, String> entry : plugin.formattedNames.entrySet()) {
            String player = entry.getKey();
            entry.setValue(
                    plugin.formatPlayerName(player, plugin.lilypadOnlinePlayersHandler.getPlayer(player).getWorld())
            );
        }
    }
}
