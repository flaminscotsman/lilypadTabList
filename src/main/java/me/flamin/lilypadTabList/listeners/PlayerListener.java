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
import java.util.UUID;

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
        String formattedPlayerName = plugin.formatPlayerName(playerEntity.getName(), playerEntity.getUniqueId(), playerEntity.getWorld().getName());

        if (lilypadTabList.DEBUG)
            plugin.getLogger().severe(String.format(
                    "[%1$s] - Setting %2$s's tab entry to %3$s.",
                    plugin.getDescription().getName(),
                    playerEntity.getName(),
                    formattedPlayerName
            ));

        try {
            playerEntity.setPlayerListName(formattedPlayerName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe(String.format(
                    "[%1$s] - Error colouring local player %2$s's name - duplicate entry found",
                    plugin.getDescription().getName(),
                    playerEntity.getName()
            ));
        }

        final UUID playerID = playerEntity.getUniqueId();
        final String playerName = playerEntity.getName();

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (lilypadTabList.DEBUG)
                    plugin.getLogger().severe(String.format(
                            "[%1$s] - Processing packets for %2$s.",
                            plugin.getDescription().getName(),
                            playerName
                    ));

                Player player = plugin.getServer().getPlayer(playerID);
                if (player == null)
                    return;

                for (Iterator<Map.Entry<String, String>> iterator = plugin.formattedNames.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<String, String> entry = iterator.next();
                    String entryName = entry.getKey();
                    String formattedEntryName = entry.getValue();

                    if (!plugin.lilypadOnlinePlayersHandler.containsPlayer(entryName)) {
                        try {
                            PacketContainer packet = playerListConstructor.createPacket(
                                    formattedEntryName, false, 0
                            );

                            if (lilypadTabList.DEBUG)
                                plugin.getLogger().severe(String.format(
                                        "[%1$s] - Removing expired player %2$s from the tab list.",
                                        plugin.getDescription().getName(),
                                        entryName
                                ));

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

                    if (plugin.lilypadOnlinePlayersHandler.getPlayer(entryName).getServer().equals(plugin.servername))
                        continue; // We don't wish to handle players currently on this server

                    if (!plugin.lilypadOnlinePlayersHandler.getPlayer(entryName).getVisible() && !player.hasPermission("lilypadTabList.viewHidden"))
                        continue; // Only show vanished players if they have permission to see them

                    PacketContainer packet = playerListConstructor.createPacket(
                            formattedEntryName, true, 0
                    );

                    if (lilypadTabList.DEBUG)
                        plugin.getLogger().severe(String.format(
                                "[%1$s] - Sending packet corresponding to %2$s to %3$s.",
                                plugin.getDescription().getName(),
                                entryName,
                                playerName
                        ));

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