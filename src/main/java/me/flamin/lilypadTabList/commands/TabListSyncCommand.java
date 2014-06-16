package me.flamin.lilypadTabList.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import com.comphenix.protocol.reflect.FieldAccessException;
import me.flamin.lilypadOnlinePlayers.PlayerEntry;
import me.flamin.lilypadTabList.lilypadTabList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class TabListSyncCommand implements CommandExecutor {
    private final lilypadTabList plugin;
    private final PacketConstructor playerListConstructor;

    public TabListSyncCommand(lilypadTabList plugin) {
       this.plugin = plugin;
        playerListConstructor = plugin.protocolManager.createPacketConstructor(
                PacketType.Play.Server.PLAYER_INFO, "", false, (int) 0
        );
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args ){
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!player.hasPermission(command.getPermission())) {
                commandSender.sendMessage("Insufficient permissions to use " + command.getName());
                return true;
            }
        }
        clearTabLists();
        plugin.formattedNames.clear();
        repopulateTabLists();
        return true;
    }

    private void clearTabLists() {
        for (String tabEntry: plugin.formattedNames.values()) {
            try {
                PacketContainer packet = playerListConstructor.createPacket(
                        tabEntry, false, 0
                );

                if (lilypadTabList.DEBUG)
                    plugin.getLogger().info("Removing " + tabEntry + " from the tab list.");

                for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                    plugin.protocolManager.sendServerPacket(receiver, packet);
                }
            } catch (FieldAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void repopulateTabLists() {
        for (Map.Entry<String, PlayerEntry> entry : plugin.lilypadOnlinePlayersHandler.getPlayers().entrySet()) {
            PlayerEntry player = entry.getValue();
            boolean playerVisible = player.getVisible();
            String formattedName = plugin.formatPlayerName(player);

            try {
                PacketContainer packet = playerListConstructor.createPacket(
                        formattedName, true, 0
                );

                if (lilypadTabList.DEBUG)
                    plugin.getLogger().info("Adding " + formattedName + " to the tab list.");

                for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                    if (playerVisible || receiver.hasPermission("lilypadTabList.viewHidden"))
                        plugin.protocolManager.sendServerPacket(receiver, packet);
                }
            } catch (FieldAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            plugin.formattedNames.put(player.getName(), formattedName);
        }
    }
}
