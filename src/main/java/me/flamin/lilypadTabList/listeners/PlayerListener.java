package me.flamin.lilypadTabList.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import com.comphenix.protocol.reflect.FieldAccessException;
import me.flamin.lilypadTabList.lilypadTabList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class PlayerListener implements Listener {
    private final lilypadTabList plugin;
    private final PacketConstructor playerListConstructor;

    public PlayerListener(lilypadTabList plugin) {
        this.plugin = plugin;
        playerListConstructor = plugin.protocolManager.createPacketConstructor(
                PacketType.Play.Server.PLAYER_INFO, "", false, (int) 0
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED))
            return; // We do not wish to track blocked logins

        Player playerEntity = event.getPlayer();
        String formattedPlayerName = plugin.formatPlayerName(playerEntity.getName(), playerEntity.getWorld().getName());
        playerEntity.setPlayerListName(formattedPlayerName);

        final String playerName = playerEntity.getName();

        if (lilypadTabList.DEBUG)
            plugin.getLogger().severe("Renaming " + playerEntity.getName() + " to " + formattedPlayerName + ".");

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (lilypadTabList.DEBUG)
                    plugin.getLogger().severe("Processing packets for " + playerName);

                Player player = plugin.getServer().getPlayerExact(playerName);
                if (player == null)
                    return;

                for (Iterator<Map.Entry<String, String>> iterator = plugin.formattedNames.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<String, String> entry = iterator.next();
                    String entryName = entry.getKey();
                    String formattedEntryName = entry.getValue();

                    if (!plugin.lilypadOnlinePlayers.containsPlayer(entryName)) {
                        try {
                            PacketContainer packet = playerListConstructor.createPacket(
                                    formattedEntryName, false, 0
                            );

                            if (lilypadTabList.DEBUG)
                                plugin.getLogger().severe("Removing expired player " + playerName
                                        + " from the tab list.");

                            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                                plugin.protocolManager.sendServerPacket(receiver, packet);
                            }
                        } catch (FieldAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }

                        iterator.remove();
                        continue;
                    }

                    if (plugin.lilypadOnlinePlayers.getPlayer(entryName).getServer().equals(plugin.servername))
                        continue; // We don't wish to handle players currently on this server

                    PacketContainer packet = playerListConstructor.createPacket(
                            formattedEntryName, true, 0
                    );

                    if (lilypadTabList.DEBUG)
                        plugin.getLogger().severe(
                                "Sending packet corresponding to " + entryName + " to " + playerName
                        );

                    try {
                        plugin.protocolManager.sendServerPacket(player, packet);
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}