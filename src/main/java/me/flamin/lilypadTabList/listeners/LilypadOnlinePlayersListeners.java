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
        if (lilypadTabList.DEBUG)
            plugin.getLogger().severe("Player " + event.getName() + " has joined " + event.getWorld());
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;
        String formattedName = plugin.formatPlayerName(event.getName(), event.getWorld(), event.getVisibility());
        plugin.formattedNames.put(event.getName(), formattedName);
        try {
            PacketContainer packet = playerListConstructor.createPacket(
                    plugin.formatPlayerName(event.getName(), event.getWorld()), true, 0
            );
            if (lilypadTabList.DEBUG) {
                plugin.getLogger().severe("Adding " + formattedName + " to the tab list.");
            }

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
    public void onHubPlayerQuit(final HubPlayerQuitEvent event) {
        if (plugin.lilypadOnlinePlayersHandler.getPlayer(event.getName()).getServer().equals(plugin.servername))
            return;
        if (!plugin.formattedNames.containsKey(event.getName())) {
            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe("Unable to find " + event.getName() + " in the list of"
                        + " formatted names.");
            return;
        }
        String playerName = plugin.formattedNames.get(event.getName());
        plugin.formattedNames.remove(event.getName());

        try {
            PacketContainer packet = playerListConstructor.createPacket(
                    playerName, false, 0
            );

            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe("Removing " + playerName
                        + " from the tab list.");

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
        String oldFormattedName = plugin.formattedNames.get(event.getName());
        String formattedName = plugin.formatPlayerName(event.getName(), event.getWorld());
        try {
            PacketContainer oldNamePacket = playerListConstructor.createPacket(
                    oldFormattedName, false, 0
            );
            PacketContainer newNamePacket = playerListConstructor.createPacket(
                    formattedName, true, 0
            );

            if (lilypadTabList.DEBUG)
                plugin.getLogger().severe("Renaming " + oldFormattedName +
                        " to " + formattedName + " in the tab list.");

            plugin.formattedNames.put(event.getName(), formattedName);

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                plugin.protocolManager.sendServerPacket(receiver, oldNamePacket);
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
        String formattedName = plugin.formatPlayerName(event.getName(), entry.getWorld(), !event.isVanishing());
        try {
            PacketContainer packet = playerListConstructor.createPacket(
                    event.isVanishing() ? oldFormattedName : formattedName, !event.isVanishing(), 0
            );

            PacketContainer hiddenPacket = playerListConstructor.createPacket(
                    event.isVanishing() ? formattedName : oldFormattedName, event.isVanishing(), 0
            );

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                // All payers get the entry removed on vanish
                plugin.protocolManager.sendServerPacket(receiver, packet);
                if (receiver.hasPermission("lilypadTabList.viewHidden"))
                    plugin.protocolManager.sendServerPacket(receiver, hiddenPacket);
            }
        } catch (FieldAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
