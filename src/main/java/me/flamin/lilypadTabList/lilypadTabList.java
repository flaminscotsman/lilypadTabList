package me.flamin.lilypadTabList;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.flamin.lilypadOnlinePlayers.PlayerEntry;
import me.flamin.lilypadTabList.commands.ListFormatTabMembersCommand;
import me.flamin.lilypadTabList.commands.TabListSyncCommand;
import me.flamin.lilypadTabList.commands.TabNameRefreshCommand;
import me.flamin.lilypadTabList.commands.ListTabMembersCommand;
import me.flamin.lilypadTabList.listeners.LilypadOnlinePlayersListeners;
import me.flamin.lilypadTabList.listeners.PlayerListener;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import me.flamin.lilypadOnlinePlayers.LilypadOnlinePlayersHandler;
import net.milkbowl.vault.chat.Chat;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class lilypadTabList extends JavaPlugin {
    public static final boolean DEBUG = false;
    private static final Matcher matcher = Pattern.compile("(&[0-9a-fk-orA-FK-OR])").matcher("");
    public final Map<String, String> formattedNames = new HashMap<String, String>();
    public LilypadOnlinePlayersHandler lilypadOnlinePlayersHandler;
    public ProtocolManager protocolManager;
    public String servername;
    private ZPermissionsService zPerms;
    private Chat chat;

    @Override
    public void onEnable(){
        if (!hookOnlinePlayers() ) {
            getLogger().severe(String.format("[%s] - Unable to find LilypadOnlinePlayers, disabling!",
                    getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!hookzPerms() ) {
            getLogger().severe(String.format("[%s] - Unable to find zPermissions - falling back to vault colouring.",
                    getDescription().getName()));
        }
        if (!hookVault() ) {
            getLogger().severe(String.format("[%s] - No chat formatting plugin found. Defaulting to white names",
                    getDescription().getName()));
        }

        protocolManager = ProtocolLibrary.getProtocolManager();

        servername = lilypadOnlinePlayersHandler.getServerName();
        getServer().getPluginManager().registerEvents(new LilypadOnlinePlayersListeners(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getCommand("listtabmembers").setExecutor(new ListTabMembersCommand(this));
        getCommand("listformattedtabmembers").setExecutor(new ListFormatTabMembersCommand(this));
        getCommand("tabnamerefresh").setExecutor(new TabNameRefreshCommand(this));
        getCommand("tablistsync").setExecutor(new TabListSyncCommand(this));
    }

    @Override
    public void onDisable() {
    }

    private boolean hookVault() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean hookzPerms() {
        RegisteredServiceProvider<ZPermissionsService> rsp = getServer().getServicesManager().getRegistration(ZPermissionsService.class);
        zPerms = rsp.getProvider();
        return zPerms != null;
    }

    private boolean hookOnlinePlayers() {
        RegisteredServiceProvider<LilypadOnlinePlayersHandler> rsp =
                getServer().getServicesManager().getRegistration(LilypadOnlinePlayersHandler.class);
        lilypadOnlinePlayersHandler = rsp.getProvider();
        return lilypadOnlinePlayersHandler != null;
    }

    @Deprecated
    public String formatPlayerName(String player, String world) { return formatPlayerName(player, world, true); }

    public String formatPlayerName(String playerName, UUID playerID, String world) {
        return formatPlayerName(playerName, playerID, world, true);
    }

    @Deprecated
    public String formatPlayerName(String player, String world, boolean visible) {
        String prefix = (chat != null) ? chat.getPlayerPrefix(world, player) : "&r";

        matcher.reset(prefix);
        String colourCode = "&r";
        while(matcher.find())
            colourCode = matcher.group(1);
        player = ChatColor.translateAlternateColorCodes('&', (colourCode + player));

        boolean first = true;
        StringBuilder prefixBuilder = new StringBuilder();
        for (String prefixType: getConfig().getStringList("prefixPrecedence")) {
            boolean prefixTest = false;
            if (prefixType.equalsIgnoreCase("invisible")) {
                prefixTest = !visible;
            } else if (prefixType.equalsIgnoreCase("op")) {
                prefixTest = false;
            } else if (prefixType.equalsIgnoreCase("creative")) {
                prefixTest = false;
            }

            if (first || getConfig().getBoolean("mergePrefixes", false)) {
                String itemPrefix = getConfig().getString(prefixType + "Prefix");
                if (itemPrefix != null)
                    prefixBuilder.append(prefixTest ? "" : itemPrefix);
            }
            first = false;
        }

        if (first && !visible)
            prefixBuilder.append(getConfig().getString("invisiblePrefix", "?"));

        player = prefixBuilder.toString() + ((prefixBuilder.length() == 0)?"":getConfig().getString("prefixDelimiter", ":")) + player;

        player = player.substring(0, Math.min(player.length(), 16));
        return player;
    }

    public String formatPlayerName(PlayerEntry entry) {
        return formatPlayerName(entry.getName(), entry.getUUID(), entry.getWorld(), entry.getVisible());
    }

    public String formatPlayerName(String playerName, UUID playerID, String world, boolean visible) {
        String prefix = zPermsGetPrefix(playerID, world);
        prefix = (prefix == null) ? vaultGetPrefix(playerID, world) : prefix;
        prefix = (prefix == null) ? "&r" : prefix;

        matcher.reset(prefix);
        String colourCode = "&r";
        while(matcher.find())
            colourCode = matcher.group(1);

        playerName = ChatColor.translateAlternateColorCodes('&', (colourCode + playerName));

        boolean first = true;
        StringBuilder prefixBuilder = new StringBuilder();

        for (String prefixType: getConfig().getStringList("prefixPrecedence")) {
            boolean prefixNeeded = false;

            if (prefixType.equalsIgnoreCase("invisible")) {
                prefixNeeded = !visible;
            } else if (prefixType.equalsIgnoreCase("op")) {
                prefixNeeded = false;
            } else if (prefixType.equalsIgnoreCase("creative")) {
                prefixNeeded = false;
            } else {
                continue;
            }

            if (!prefixNeeded)
                continue;

            if (first || getConfig().getBoolean("mergePrefixes", false)) {
                String itemPrefix = getConfig().getString(prefixType + "Prefix");
                if (itemPrefix != null)
                    prefixBuilder.append(itemPrefix);
            }
            first = false;
        }

        if (first && !visible)
            prefixBuilder.append(getConfig().getString("invisiblePrefix", "?"));

        playerName = prefixBuilder.toString() + ((prefixBuilder.length() == 0)?"":getConfig().getString("prefixDelimiter", ":")) + playerName;

        playerName = playerName.substring(0, Math.min(playerName.length(), 16));
        return playerName;
    }

    private String zPermsGetPrefix(UUID uuid, String world) {
        return (zPerms == null) ? null : zPerms.getPlayerPrefix(uuid);
    }

    private String vaultGetPrefix(UUID uuid, String world) {
        OfflinePlayer player = this.getServer().getOfflinePlayer(uuid);
        return (chat == null) ? null : chat.getPlayerPrefix(world, player);
    }
}
