package me.flamin.lilypadTabList;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.flamin.lilypadTabList.commands.ListFormatTabMembersCommand;
import me.flamin.lilypadTabList.commands.TabListSyncCommand;
import me.flamin.lilypadTabList.commands.TabNameRefreshCommand;
import me.flamin.lilypadTabList.commands.ListTabMembersCommand;
import me.flamin.lilypadTabList.listeners.LilypadOnlinePlayersListeners;
import me.flamin.lilypadTabList.listeners.PlayerListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import me.flamin.lilypadOnlinePlayers.LilypadOnlinePlayersHandler;
import net.milkbowl.vault.chat.Chat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class lilypadTabList extends JavaPlugin {
    public static final boolean DEBUG = false;
    private static final Matcher matcher = Pattern.compile("(&[0-9a-fk-orA-FK-OR])").matcher("");
    public final Map<String, String> formattedNames = new HashMap<String, String>();
    public LilypadOnlinePlayersHandler lilypadOnlinePlayersHandler;
    public ProtocolManager protocolManager;
    public String servername;
    private Chat chat;

    @Override
    public void onEnable(){
        if (!hookOnlinePlayers() ) {
            getLogger().severe(String.format("[%s] - Unable to find LilypadOnlinePlayers, disabling!",
                    getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
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

    private boolean hookOnlinePlayers() {
        RegisteredServiceProvider<LilypadOnlinePlayersHandler> rsp =
                getServer().getServicesManager().getRegistration(LilypadOnlinePlayersHandler.class);
        lilypadOnlinePlayersHandler = rsp.getProvider();
        return lilypadOnlinePlayersHandler != null;
    }

    public String formatPlayerName(String player, String world) {
        return formatPlayerName(player, world, true);
    }

    public String formatPlayerName(String player, String world, boolean visible) {
        String prefix;
        if (chat != null) {
            prefix = chat.getPlayerPrefix(world, player);
        } else {
            prefix = "&r";
        }

        matcher.reset(prefix);
        String colourcode = "&r";
        while(matcher.find())
            colourcode = matcher.group(1);
        player = ChatColor.translateAlternateColorCodes('&', (colourcode + player));

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
}
