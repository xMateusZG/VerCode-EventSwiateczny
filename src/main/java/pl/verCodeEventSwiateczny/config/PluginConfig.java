package pl.verCodeEventSwiateczny.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pl.verCodeEventSwiateczny.Main;
import pl.verCodeEventSwiateczny.utils.Chat;

import java.util.List;

public class PluginConfig {

    private final Main plugin;
    private FileConfiguration config;
    private String prefix;
    private String guiTitle;

    public PluginConfig(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.prefix = Chat.color(config.getString("messages.prefix", ""));
        this.guiTitle = Chat.color(replaceBasic(config.getString("drop.gui_name", "")));
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public String replaceBasic(String msg) {
        if (msg == null) return "";
        return msg.replace("{prefix}", prefix);
    }

    // skróty
    public String getString(String path) {
        return config.getString(path);
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public void save() {
        plugin.saveConfig();
    }
}
