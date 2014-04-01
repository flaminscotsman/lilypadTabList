package me.flamin.lilypadTabList.commands;

import me.flamin.lilypadTabList.lilypadTabList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;

public class ListFormatTabMembersCommand implements CommandExecutor {
    private final lilypadTabList plugin;

    public ListFormatTabMembersCommand(lilypadTabList plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args ){
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            return player.hasPermission("lilypadTabList.list") && tablist(commandSender, args);
        } else {
            return tablist(commandSender, args);
        }
    }

    private boolean tablist(CommandSender commandSender, String[] args) {
        if (args.length == 0) {
            sendAllPlayers(commandSender);
            return true;
        } else if (args.length == 1) {
            sendFilteredPlayers(commandSender, args[0]);
            return true;
        } else {
            commandSender.sendMessage("Invalid number of arguments.");
            return false;
        }
    }

    private void sendFilteredPlayers(CommandSender commandSender, String matcher) {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> iter = plugin.formattedNames.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getKey().startsWith(matcher)) {
                sb.append(entry.getKey());
                sb.append(" => ");
                sb.append(entry.getValue());
                if (iter.hasNext()) {
                    sb.append(',').append(' ');
                }
            }
        }
        if (sb.length() > 0) {
            commandSender.sendMessage(sb.toString());
        } else {
            commandSender.sendMessage("Failed to find any formatted players beginning with " + matcher + ".");
        }
    }

    private void sendAllPlayers(CommandSender commandSender) {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> iter = plugin.formattedNames.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            sb.append(entry.getKey());
            sb.append(" => ");
            sb.append(entry.getValue()).append(ChatColor.WHITE.toString());
            if (iter.hasNext()) {
                sb.append(',').append(' ');
            }
        }

        if (plugin.formattedNames.size() > 0)
            commandSender.sendMessage(sb.toString());
        else
            commandSender.sendMessage("There appear to be no formatted names.");
    }
}
