package pl.verCodeEventSwiateczny.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.utils.Chat;

import java.util.List;

public class GiftGUI {

    private final PluginConfig cfg;
    private final GiftGUIHolder holder = new GiftGUIHolder();

    public GiftGUI(PluginConfig cfg) {
        this.cfg = cfg;
    }

    public void openGiftGUI(Player player) {
        int slots = cfg.getInt("drop.slots_gui", 54);
        Inventory gui = Bukkit.createInventory(holder, slots, cfg.getGuiTitle());

        ConfigurationSection items = cfg.getSection("drop.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key) - 1;
                } catch (NumberFormatException e) {
                    continue;
                }
                if (slot < 0 || slot >= slots) continue;
                ItemStack item = createDropItem(items.getConfigurationSection(key));
                if (item != null) gui.setItem(slot, item);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createDropItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("item");
        if (materialName == null) return null;

        Material material = Material.matchMaterial(materialName);
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color(section.getString("name", "")));
            List<String> lore = section.getStringList("lore");
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(Chat.colorLore(lore));
            }
            if (section.getBoolean("glow", false)) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public GiftGUIHolder getHolder() {
        return holder;
    }

    public static class GiftGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
