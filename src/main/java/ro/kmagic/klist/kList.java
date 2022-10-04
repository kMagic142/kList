package ro.kmagic.klist;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import ro.kmagic.klist.commands.ListCommand;
import ro.kmagic.klist.commands.ListReloadCommand;
import ro.kmagic.klist.commands.PVListCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class kList extends Plugin {

    private static Configuration config;
    private static File dataFolder;

    @Override
    public void onEnable() {

        if(!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");


        if(!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        dataFolder = getDataFolder();

        if(getProxy().getPluginManager().getPlugin("PremiumVanish") != null) {
            getProxy().getPluginManager().registerCommand(this, new PVListCommand());
            getProxy().getLogger().info("Hooked into PremiumVanish!");
        } else {
            getProxy().getPluginManager().registerCommand(this, new ListCommand());
            getProxy().getLogger().severe("Could not hook into PremiumVanish. kList will not hide hidden players!");
        }

        getProxy().getPluginManager().registerCommand(this, new ListReloadCommand());
    }

    public static Configuration getConfig() {
        return config;
    }

    public static void reloadConfig() {
        File file = new File(dataFolder, "config.yml");
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // bye?
    }
}
