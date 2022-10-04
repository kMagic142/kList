package ro.kmagic.klist.commands;

import com.google.common.base.Strings;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import ro.kmagic.klist.kList;

import java.util.*;
import java.util.concurrent.*;

public class ListCommand extends Command {

    public ListCommand() {
        super("glist", "bungeecord.command.list");
    }

    private final HashMap<String, Integer> maxPlayersCache = new HashMap<>();


    @Override
    public void execute(CommandSender sender, String[] args) {
        HashMap<String, String> serverRows = new HashMap<>();
        List<ServerInfo> servers = new ArrayList<>();
        Configuration config = kList.getConfig();

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (!server.canAccess(sender))
                continue;

            if (server.getPlayers().size() < 1)
                if (config.getBoolean("hide-empty-servers"))
                    continue;

            if (config.getStringList("blacklisted-servers").contains(server.getName()))
                continue;

            int[] playercount = new int[]{getMaxPlayers(sender, config, server)};

            if(playercount[0] <= 0)
                continue;

            int totalPlayers = ProxyServer.getInstance().getOnlineCount();
            float percent = totalPlayers == 0 ? 0.0F : (server.getPlayers().size() * 100.0F / totalPlayers);

            String progressbar = getProgressBar(server.getPlayers().size(), playercount[0], config.getString("format.bar-symbol"), config);
            String serverFormat = config.getString("format.server-row-format");

            serverFormat = serverFormat.replace("{PLAYER_AMOUNT}", "" + server.getPlayers().size());
            serverFormat = serverFormat.replace("{GRAPHIC_BAR}", progressbar);
            serverFormat = serverFormat.replace("{PERCENT}", (percent - ((int) percent) == 0 ? String.format("%.0f", percent) : String.format("%.02f", percent)) + "%");

            serverRows.put(server.getName(), serverFormat);
            servers.add(server);
        }

        for (String message : config.getStringList("format.full-message-format")) {
            if (message.contains("{SERVERS_ROWS}")) {
                sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', format(servers, serverRows, config))).create());
                continue;
            }

            if (message.contains("{TOTAL_PLAYER_AMOUNT}"))
                message = message.replace("{TOTAL_PLAYER_AMOUNT}", "" + ProxyServer.getInstance().getOnlineCount());


            sender.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', message)).create());
        }

    }

    private String getProgressBar(int current, int max, String symbol, Configuration config) {
        if(current < (max / 20)) current = max / 20;

        float percent = (float) current / max;
        int progressBars = (int) (20 * percent);
        int comparablePercent = (int) (percent * 100);

        if (comparablePercent <= 25 && comparablePercent >= 0) {
            return Strings.repeat(config.getString("format.progress-colors.25") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        } else if (comparablePercent <= 50 && comparablePercent > 25) {
            return Strings.repeat(config.getString("format.progress-colors.50") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        } else if (comparablePercent <= 75 && comparablePercent > 50) {
            return Strings.repeat(config.getString("format.progress-colors.75") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        } else if (comparablePercent <= 99 && comparablePercent > 75) {
            return Strings.repeat(config.getString("format.progress-colors.99") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        } else if (comparablePercent >= 100) {
            return Strings.repeat(config.getString("format.progress-colors.100") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        } else {
            return Strings.repeat(config.getString("format.progress-colors.default") + symbol, progressBars)
                    + Strings.repeat(config.getString("format.bar-background-color") + symbol, 20 - progressBars);
        }
    }

    private String format(List<ServerInfo> servers, HashMap<String, String> formattedRows, Configuration config) {
        StringBuilder message = new StringBuilder();
        servers.sort(Comparator.comparingInt(a -> a.getPlayers().size()));
        Collections.reverse(servers);

        for (ServerInfo server : servers) {
            if (config.getBoolean("uppercase-server-names")) {
                message.append(formattedRows.get(server.getName()).replace("{SERVER_NAME}", server.getName().toUpperCase())).append("\n");
            } else {
                message.append(formattedRows.get(server.getName()).replace("{SERVER_NAME}", server.getName())).append("\n");
            }
        }

        return (message.toString().length() <= 0 ? config.getString("no-servers-format") : message.toString());

    }


    private int getMaxPlayers(CommandSender sender, Configuration config, ServerInfo server) {
        ExecutorService executor = Executors.newCachedThreadPool();

        Future<?> future = executor.submit(() -> {
            if(!server.canAccess(sender))
                return;

            if(server.getPlayers().size() < 1)
                if(config.getBoolean("hide-empty-servers"))
                    return;

            if(config.getStringList("blacklisted-servers").contains(server.getName()))
                return;

            CompletableFuture.supplyAsync(() -> {
                server.ping((result, error) -> {
                    if (error != null) {
                        maxPlayersCache.put(server.getName(), 0);
                        return;
                    }

                    if (result == null) return;

                    ServerPing.Players players = result.getPlayers();
                    if (players != null)
                        maxPlayersCache.put(server.getName(), players.getMax());
                    else {
                        maxPlayersCache.put(server.getName(), 0);
                    }
                });
                return maxPlayersCache.get(server.getName());
            });
        });

        try {
            synchronized(this) {
                future.get();
                wait(25L);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return (maxPlayersCache.get(server.getName()) != null ? maxPlayersCache.get(server.getName()) : 0);
    }

}
