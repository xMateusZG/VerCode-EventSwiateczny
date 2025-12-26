package pl.verCodeEventSwiateczny.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.profile.PlayerTextures.SkinModel;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.verCodeEventSwiateczny.Main;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.utils.Chat;

import java.net.URL;
import java.util.*;

public class GiftManager {

    private final Main plugin;
    private final PluginConfig cfg;

    private final Map<UUID, Long> lastGiftTime = new HashMap<>();
    private final Map<Location, Long> spawnedGifts = new HashMap<>();
    private ItemStack giftItem;
    private long giftDeliveryTicks;
    private BukkitTask giftDeliveryTask;

    public GiftManager(Main plugin, PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        createGiftItem();
        parseGiftDeliveryTime();
        startGiftDeliveryTask();
    }

    public void cleanup() {
        lastGiftTime.clear();
        spawnedGifts.clear();
        if (giftDeliveryTask != null) giftDeliveryTask.cancel();
    }

    private void createGiftItem() {
        String blockName = cfg.getString("gift_settings.gift_block.block", "PLAYER_HEAD");
        Material material;
        try {
            material = Material.valueOf(blockName);
        } catch (IllegalArgumentException e) {
            material = Material.PLAYER_HEAD;
        }
        giftItem = new ItemStack(material);

        ItemMeta meta = giftItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Chat.color("&6&lPREZENT"));
            meta.setLore(Arrays.asList(
                    Chat.color("&7Kliknij PPM trzymając w ręce,"),
                    Chat.color("&7aby otworzyć prezent!")
            ));
            giftItem.setItemMeta(meta);
        }

        if (material == Material.PLAYER_HEAD) {
            String headUrl = cfg.getString("gift_settings.gift_block.head-url");
            if (headUrl != null && !headUrl.isEmpty()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    ItemStack head = setHeadTexture(giftItem.clone(), headUrl);
                    if (head != null) giftItem = head;
                });
            }
        }
    }

    private ItemStack setHeadTexture(ItemStack head, String textureUrl) {
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "Gift");
            PlayerTextures textures = profile.getTextures();

            String cleanUrl = textureUrl
                    .replace("https://www.minecraftskins.com/uploads/skins/", "http://textures.minecraft.net/texture/")
                    .replace(".png", "");

            textures.setSkin(new URL(cleanUrl), SkinModel.CLASSIC);
            profile.setTextures(textures);
            profile.update().join();

            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
            return head;
        } catch (Exception e) {
            plugin.getLogger().warning("Nie udało się ustawić tekstury głowy: " + e.getMessage());
            return head;
        }
    }

    private void parseGiftDeliveryTime() {
        String timeStr = cfg.getString("gift_settings.gift_delivery_time", "5m");
        giftDeliveryTicks = parseTimeToTicks(timeStr);
    }

    public long parseTimeToTicks(String timeStr) {
        long ticks = 0;
        String[] parts = timeStr.split(",");
        for (String part : parts) {
            part = part.trim().toLowerCase();
            if (!part.matches("\\d+[smhd]")) continue;
            char unit = part.charAt(part.length() - 1);
            int value = Integer.parseInt(part.substring(0, part.length() - 1));
            switch (unit) {
                case 's': ticks += value * 20L; break;
                case 'm': ticks += value * 1200L; break;
                case 'h': ticks += value * 72000L; break;
                case 'd': ticks += value * 1728000L; break;
            }
        }
        return ticks;
    }

    private void startGiftDeliveryTask() {
        if (giftDeliveryTask != null) giftDeliveryTask.cancel();
        giftDeliveryTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    long last = lastGiftTime.getOrDefault(player.getUniqueId(), 0L);
                    long nowTicks = player.getWorld().getFullTime();
                    if (nowTicks - last >= giftDeliveryTicks) {
                        giveGift(player);
                        lastGiftTime.put(player.getUniqueId(), nowTicks);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void reloadDeliveryTime() {
        parseGiftDeliveryTime();
        startGiftDeliveryTask();
    }

    public void giveGift(Player player) {
        player.getInventory().addItem(giftItem.clone());
        player.sendTitle(
                Chat.color(replaceBasic(cfg.getString("messages.player.give_gift_time.title", ""))),
                Chat.color(replaceBasic(cfg.getString("messages.player.give_gift_time.subtitle", ""))),
                10, 70, 20
        );
        player.sendMessage(Chat.color(replaceBasic(
                cfg.getString("messages.player.give_gift_time.chat", "")
        )));
    }

    public void giveRandomDrop(Player player) {
        org.bukkit.configuration.ConfigurationSection items =
                cfg.getSection("drop.items");
        if (items == null || items.getKeys(false).isEmpty()) return;

        List<String> ids = new ArrayList<>(items.getKeys(false));
        String chosenId = ids.get(new Random().nextInt(ids.size()));
        org.bukkit.configuration.ConfigurationSection section = items.getConfigurationSection(chosenId);
        if (section == null) return;

        String nameMsg = Chat.color(section.getString("name_messages",
                Chat.color(section.getString("name", "Item"))));

        player.sendMessage(Chat.color(replaceBasic(
                cfg.getString("messages.player.opening_gift", "")
        ).replace("{item}", nameMsg)));

        List<String> commands = section.getStringList("commands");
        if (commands != null) {
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName()));
            }
        }
    }

    public boolean isSimilarItem(ItemStack one, ItemStack two) {
        if (one == null || two == null) return false;
        return one.isSimilar(two);
    }

    public ItemStack getGiftItem() {
        return giftItem;
    }

    public Map<Location, Long> getSpawnedGifts() {
        return spawnedGifts;
    }

    public String replaceBasic(String msg) {
        return cfg.replaceBasic(msg);
    }
}
