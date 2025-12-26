package pl.verCodeEventSwiateczny;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import pl.verCodeEventSwiateczny.commands.EventCommand;
import pl.verCodeEventSwiateczny.commands.PrezentyCommand;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.gui.GiftGUI;
import pl.verCodeEventSwiateczny.listeners.EventListener;
import pl.verCodeEventSwiateczny.managers.GiftManager;
import pl.verCodeEventSwiateczny.managers.RegionManager;

import java.util.Objects;

public class Main extends JavaPlugin {

    private PluginConfig pluginConfig;
    private GiftManager giftManager;
    private RegionManager regionManager;
    private GiftGUI giftGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);

        giftManager = new GiftManager(this, pluginConfig);
        regionManager = new RegionManager(this, pluginConfig, giftManager);
        giftGUI = new GiftGUI(pluginConfig);

        EventListener listener = new EventListener(pluginConfig, giftManager, regionManager, giftGUI);
        getServer().getPluginManager().registerEvents(listener, this);

        EventCommand eventCommand = new EventCommand(this, pluginConfig, giftManager, regionManager);
        Objects.requireNonNull(getCommand("eventswiateczny")).setExecutor(eventCommand);
        Objects.requireNonNull(getCommand("eventswiateczny")).setTabCompleter(eventCommand);

        Objects.requireNonNull(getCommand("prezenty")).setExecutor(new PrezentyCommand(giftGUI));
    }

    @Override
    public void onDisable() {
        if (giftManager != null) giftManager.cleanup();
        if (regionManager != null) regionManager.cleanup();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
