package pl.verCodeEventSwiateczny.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.verCodeEventSwiateczny.Main;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.utils.Chat;

import java.util.*;

public class RegionManager {

    private final Main plugin;
    private final PluginConfig cfg;
    private final GiftManager giftManager;

    private final Map<String, BukkitTask> regionTasks = new HashMap<>();
    private final Map<UUID, Location[]> regionCreating = new HashMap<>();

    public RegionManager(Main plugin, PluginConfig cfg, GiftManager giftManager) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.giftManager = giftManager;
        loadRegions();
    }

    public void cleanup() {
        regionTasks.values().forEach(BukkitTask::cancel);
        regionTasks.clear();
        regionCreating.clear();
    }

    public void loadRegions() {
        regionTasks.values().forEach(BukkitTask::cancel);
        regionTasks.clear();

        ConfigurationSection regionsSection = cfg.getSection("gift_regions");
        if (regionsSection == null) return;

        for (String regionName : regionsSection.getKeys(false)) {
            startRegionTask(regionName);
        }
    }

    private void startRegionTask(String regionName) {
        long ticks = parseRegionTime(regionName);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnGiftsInRegion(regionName);
            }
        }.runTaskTimer(plugin, 0L, ticks);

        regionTasks.put(regionName, task);
    }

    private long parseRegionTime(String regionName) {
        String timeStr = cfg.getString("gift_regions." + regionName + ".time", "1m");
        return giftManager.parseTimeToTicks(timeStr);
    }

    private void spawnGiftsInRegion(String regionName) {
        ConfigurationSection positionsSection =
                cfg.getSection("gift_regions." + regionName + ".positions");
        if (positionsSection == null) return;

        List<Location> validPositions = new ArrayList<>();
        List<String> allowedBlocks =
                cfg.getStringList("gift_regions." + regionName + ".blocks");

        for (String posName : positionsSection.getKeys(false)) {
            String coordsStr =
                    cfg.getString("gift_regions." + regionName + ".positions." + posName);
            if (coordsStr == null) continue;
            String[] coords = coordsStr.split(", ");
            if (coords.length != 3) continue;
            try {
                double x = Double.parseDouble(coords[0].trim());
                double y = Double.parseDouble(coords[1].trim());
                double z = Double.parseDouble(coords[2].trim());
                Location loc = new Location(Bukkit.getWorlds().get(0), x, y, z);
                Block block = loc.getBlock();
                if (allowedBlocks.contains(block.getType().name())
                        && !giftManager.getSpawnedGifts().containsKey(loc)) {
                    validPositions.add(loc);
                }
            } catch (NumberFormatException ignored) {}
        }

        int giftsOnce = cfg.getInt("gift_regions." + regionName + ".gifts_once", 1);
        for (int i = 0; i < Math.min(giftsOnce, validPositions.size()); i++) {
            spawnGiftAt(validPositions.get(i));
        }
    }

    private void spawnGiftAt(Location loc) {
        Block block = loc.getBlock();
        block.setType(giftManager.getGiftItem().getType());
        if (giftManager.getGiftItem().getType() == Material.PLAYER_HEAD && block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            SkullMeta meta = (SkullMeta) giftManager.getGiftItem().getItemMeta();
            if (meta != null) {
                skull.setOwnerProfile(meta.getOwnerProfile());
                skull.update();
            }
        }
        giftManager.getSpawnedGifts().put(loc, System.currentTimeMillis());
    }

    public Map<UUID, Location[]> getRegionCreating() {
        return regionCreating;
    }

    // komendy regionów:

    public void createRegion(Player player, String regionName) {
        Location[] positions = regionCreating.get(player.getUniqueId());
        if (positions == null || positions[0] == null || positions[1] == null) {
            player.sendMessage(Chat.color(cfg.getPrefix() + "&cNajpierw zaznacz 2 pozycje patykiem!"));
            return;
        }

        String pos1 = positions[0].getX() + ", " + positions[0].getY() + ", " + positions[0].getZ();
        String pos2 = positions[1].getX() + ", " + positions[1].getY() + ", " + positions[1].getZ();

        cfg.set("gift_regions." + regionName + ".positions.position1", pos1);
        cfg.set("gift_regions." + regionName + ".positions.position2", pos2);
        cfg.set("gift_regions." + regionName + ".time", "1m");
        cfg.set("gift_regions." + regionName + ".gifts_once", 2);
        cfg.set("gift_regions." + regionName + ".blocks", Arrays.asList("GRASS_BLOCK", "SNOW"));
        cfg.set("gift_regions." + regionName + ".region_name", regionName);

        cfg.save();
        startRegionTask(regionName);
        player.sendMessage(Chat.color(cfg.replaceBasic(
                cfg.getString("messages.regions.create")
        ).replace("{region}", regionName)));
        regionCreating.remove(player.getUniqueId());
    }

    public void deleteRegion(Player player, String regionName) {
        if (!cfg.contains("gift_regions." + regionName)) {
            player.sendMessage(Chat.color(cfg.getPrefix() + "&cRegion nie istnieje!"));
            return;
        }
        cfg.set("gift_regions." + regionName, null);
        cfg.save();
        if (regionTasks.containsKey(regionName)) {
            regionTasks.get(regionName).cancel();
            regionTasks.remove(regionName);
        }
        player.sendMessage(Chat.color(cfg.replaceBasic(
                cfg.getString("messages.regions.delete")
        ).replace("{region}", regionName)));
    }

    public void setRegionTime(Player player, String regionName, String time) {
        if (!cfg.contains("gift_regions." + regionName)) {
            player.sendMessage(Chat.color(cfg.getPrefix() + "&cRegion nie istnieje!"));
            return;
        }
        cfg.set("gift_regions." + regionName + ".time", time);
        cfg.save();
        startRegionTask(regionName);
        player.sendMessage(Chat.color(cfg.replaceBasic(
                cfg.getString("messages.regions.set_time")
        ).replace("{time}", time).replace("{region}", regionName)));
    }

    public void setRegionGiftsOnce(Player player, String regionName, int amount) {
        if (!cfg.contains("gift_regions." + regionName)) {
            player.sendMessage(Chat.color(cfg.getPrefix() + "&cRegion nie istnieje!"));
            return;
        }
        cfg.set("gift_regions." + regionName + ".gifts_once", amount);
        cfg.save();
        player.sendMessage(Chat.color(cfg.replaceBasic(
                cfg.getString("messages.regions.set_gift-once")
        ).replace("{gift_once}", String.valueOf(amount)).replace("{region}", regionName)));
    }
}
