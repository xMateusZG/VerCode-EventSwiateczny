package pl.verCodeEventSwiateczny.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.gui.GiftGUI;
import pl.verCodeEventSwiateczny.managers.GiftManager;
import pl.verCodeEventSwiateczny.managers.RegionManager;
import pl.verCodeEventSwiateczny.utils.Chat;

public class EventListener implements Listener {

    private final PluginConfig cfg;
    private final GiftManager giftManager;
    private final RegionManager regionManager;
    private final GiftGUI giftGUI;

    public EventListener(PluginConfig cfg, GiftManager giftManager, RegionManager regionManager, GiftGUI giftGUI) {
        this.cfg = cfg;
        this.giftManager = giftManager;
        this.regionManager = regionManager;
        this.giftGUI = giftGUI;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!giftManager.getSpawnedGifts().containsKey(loc)) return;

        e.setCancelled(true);
        e.getBlock().setType(Material.AIR);
        giftManager.getSpawnedGifts().remove(loc);
        giftManager.giveGift(e.getPlayer());

        Player p = e.getPlayer();
        p.sendTitle(
                Chat.color(giftManager.replaceBasic(cfg.getString("messages.player.extracting_gift.title", ""))),
                Chat.color(giftManager.replaceBasic(cfg.getString("messages.player.extracting_gift.subtitle", ""))),
                10, 70, 20
        );
        p.sendMessage(Chat.color(giftManager.replaceBasic(
                cfg.getString("messages.player.extracting_gift.chat", "")
        )));
    }

    // kliknięcie prezentu w ręce = losowanie dropu, NIE GUI
    @EventHandler
    public void onGiftOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR
                && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack inHand = e.getItem();
        if (inHand == null) return;
        if (!giftManager.isSimilarItem(inHand, giftManager.getGiftItem())) return;

        e.setCancelled(true);
        Player player = e.getPlayer();

        // usuwamy jeden prezent z ręki
        inHand.setAmount(inHand.getAmount() - 1);

        // losowanie nagrody
        giftManager.giveRandomDrop(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof GiftGUI.GiftGUIHolder)) return;
        e.setCancelled(true); // GUI jest tylko podglądem, nic nie robi
    }

    // zaznaczanie regionu patykiem
    @EventHandler
    public void onStickSelect(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.STICK) return;
        if (!player.hasPermission("vercode.eventswiateczny.region")) return;
        if (e.getClickedBlock() == null) return;

        Location loc = e.getClickedBlock().getLocation();
        Location[] positions = regionManager.getRegionCreating()
                .getOrDefault(player.getUniqueId(), new Location[2]);

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            positions[0] = loc;
            player.sendMessage(Chat.color(cfg.getPrefix() + "&aZaznaczono 1 pozycję!"));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            positions[1] = loc;
            player.sendMessage(Chat.color(cfg.getPrefix() + "&aZaznaczono 2 pozycję!"));
        }
        regionManager.getRegionCreating().put(player.getUniqueId(), positions);
    }
}
