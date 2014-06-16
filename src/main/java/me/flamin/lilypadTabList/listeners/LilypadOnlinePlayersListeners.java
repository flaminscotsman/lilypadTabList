package me.flamin.lilypadTabList.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import com.comphenix.protocol.reflect.FieldAccessException;
import me.flamin.lilypadOnlinePlayers.PlayerEntry;
import me.flamin.lilypadOnlinePlayers.events.HubPlayerJoinEvent;
import me.flamin.lilypadOnlinePlayers.events.HubPlayerQuitEvent;
import me.flamin.lilypadOnlinePlayers.events.HubPlayerVisibilityChangeEvent;
import me.flamin.lilypadOnlinePlayers.events.HubPlayerWorldChangeEvent;
import me.flamin.lilypadTabList.lilypadTabList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;

public class LilypadOnlinePlayersListeners implements Listener {
    private final lilypadTabList plugin;
    private final PacketConstructor playerListConstructor;

    public LilypadOnlinePlayersListeners(lilypadTabList plugin) {
        this.plugin = plugin;
        playerListConstructor = plugin.protocolManager.createPacketConstructor(
                PacketType.Play.Server.PLAYER_INFO, "", false, (int) 0
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHubPlayerLogin(final HubPlayerJoinEvent event) {
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;

        final String formattedName = plugin.formatPlayerName(event.getName(), event.getUUID(), event.getWorld(), event.getVisibile());
        plugin.formattedNames.put(event.getName(), formattedName);

        try {
            PacketContainer packet = playerListConstructor.createPacket(
                    formattedName, true, 0
            );
            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe(String.format(
                    "[%1$s] - Adding %2$s to the tab list.",
                    plugin.getDescription().getName(),
                    formattedName
                ));

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                if (!event.getVisibile() && !receiver.hasPermission("lilypadTabList.viewHidden"))
                    continue; // Only add vanished players if the player has permission to see them
                plugin.protocolManager.sendServerPacket(receiver, packet);
            }
        } catch (FieldAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHubPlayerQuit(final HubPlayerQuitEvent event) {
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;

        if (!plugin.formattedNames.containsKey(event.getName())) {
            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe(String.format(
                        "[%1$s] - Unable to find %2$s in the list of formatted names.",
                        plugin.getDescription().getName(),
                        event.getName()
                ));
            return;
        }

        String playerName = plugin.formattedNames.get(event.getName());
        plugin.formattedNames.remove(event.getName());

        try {
            PacketContainer packet = playerListConstructor.createPacket(
                    playerName, false, 0
            );

            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe(String.format(
                        "[%1$s] - Removing %2$s from the tab list.",
                        plugin.getDescription().getName(),
                        event.getName()
                ));

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                plugin.protocolManager.sendServerPacket(receiver, packet);
            }
        } catch (FieldAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHubPlayerWorldChange(final HubPlayerWorldChangeEvent event) {
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;

        boolean visible = true;
        if (plugin.lilypadOnlinePlayersHandler.containsPlayer(event.getName()))
            visible = plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getVisible();

        String oldFormattedName = plugin.formattedNames.get(event.getName());
        String formattedName = plugin.formatPlayerName(event.getName(), event.getUUID(), event.getWorld(), visible);

        try {
            PacketContainer oldNamePacket = playerListConstructor.createPacket(
                    oldFormattedName, false, 0
            );
            PacketContainer newNamePacket = playerListConstructor.createPacket(
                    formattedName, true, 0
            );

            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe(String.format(
                        "[%1$s] - Renaming %2$s to %3$s in the tab list.",
                        plugin.getDescription().getName(),
                        oldFormattedName,
                        formattedName
                ));

            plugin.formattedNames.put(event.getName(), formattedName);

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                plugin.protocolManager.sendServerPacket(receiver, oldNamePacket);
                if (!visible && !receiver.hasPermission("lilypadTabList.viewHidden"))
                    continue; // Only show vanished players if they have permission to see them
                plugin.protocolManager.sendServerPacket(receiver, newNamePacket);
            }
        } catch (FieldAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHubPlayerVisibilityChange(final HubPlayerVisibilityChangeEvent event) {
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;

        PlayerEntry entry = plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName());
        String oldFormattedName = plugin.formattedNames.get(event.getName());
        String formattedName = plugin.formatPlayerName(event.getName(), event.getUUID(), entry.getWorld(), event.isVisible());

        try {
            PacketContainer visiblePacket = playerListConstructor.createPacket(
                    event.isVisible() ? formattedName : oldFormattedName, event.isVisible(), 0
            );

            PacketContainer vanishedPacket = playerListConstructor.createPacket(
                    event.isVanishing() ? formattedName : oldFormattedName, event.isVanishing(), 0
            );

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                // All payers get the entry removed on vanish
                plugin.protocolManager.sendServerPacket(receiver, visiblePacket);
                if (receiver.hasPermission("lilypadTabList.viewHidden"))
                    plugin.protocolManager.sendServerPacket(receiver, vanishedPacket);
            }
        } catch (FieldAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
