package pl.verCodeEventSwiateczny.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.verCodeEventSwiateczny.Main;
import pl.verCodeEventSwiateczny.config.PluginConfig;
import pl.verCodeEventSwiateczny.managers.GiftManager;
import pl.verCodeEventSwiateczny.managers.RegionManager;
import pl.verCodeEventSwiateczny.utils.Chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PluginConfig cfg;
    private final GiftManager giftManager;
    private final RegionManager regionManager;

    public EventCommand(Main plugin, PluginConfig cfg, GiftManager giftManager, RegionManager regionManager) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.giftManager = giftManager;
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("eventswiateczny")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("Tylko gracz może użyć tej komendy.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage(Chat.color(cfg.getPrefix() +
                        "&cUżyj: /" + label + " <patyk|time|region|reload>"));
                return true;
            }

            String sub = args[0].toLowerCase();
            switch (sub) {
                case "patyk":
                    if (!player.hasPermission("vercode.eventswiateczny.patyk")) {
                        noPermission(player, "vercode.eventswiateczny.patyk");
                        return true;
                    }
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.STICK));
                    player.sendMessage(Chat.color(cfg.replaceBasic(
                            cfg.getString("messages.regions.give_stick"))));
                    return true;

                case "time":
                    if (!player.hasPermission("vercode.eventswiateczny.time")) {
                        noPermission(player, "vercode.eventswiateczny.time");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(Chat.color(cfg.getPrefix() + "&cUżyj: /" + label + " time <czas>"));
                        return true;
                    }
                    String timeStr = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    cfg.set("gift_settings.gift_delivery_time", timeStr);
                    cfg.save();
                    giftManager.reloadDeliveryTime();
                    player.sendMessage(Chat.color(cfg.getPrefix() + "&aUstawiono czas: " + timeStr));
                    return true;

                case "region":
                    return handleRegionCommand(player, label, args);

                case "reload":
                    if (!player.hasPermission("vercode.eventswiateczny.reload")) {
                        noPermission(player, "vercode.eventswiateczny.reload");
                        return true;
                    }
                    cfg.reload();
                    giftManager.reloadDeliveryTime();
                    regionManager.loadRegions();
                    player.sendMessage(Chat.color(cfg.replaceBasic(
                            cfg.getString("messages.plugin_reload"))));
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    private boolean handleRegionCommand(Player player, String label, String[] args) {
        if (!player.hasPermission("vercode.eventswiateczny.region")) {
            noPermission(player, "vercode.eventswiateczny.region");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Chat.color(cfg.getPrefix() +
                    "&cUżyj: /" + label + " region <stworz|usun|time|gift_once>"));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "stworz":
                if (args.length < 3) {
                    player.sendMessage(Chat.color(cfg.getPrefix() +
                            "&cUżyj: /" + label + " region stworz <nazwa>"));
                    return true;
                }
                regionManager.createRegion(player, args[2]);
                return true;

            case "usun":
                if (args.length < 3) {
                    player.sendMessage(Chat.color(cfg.getPrefix() +
                            "&cUżyj: /" + label + " region usun <nazwa>"));
                    return true;
                }
                regionManager.deleteRegion(player, args[2]);
                return true;

            case "time":
                if (args.length < 4) {
                    player.sendMessage(Chat.color(cfg.getPrefix() +
                            "&cUżyj: /" + label + " region time <nazwa> <czas>"));
                    return true;
                }
                String regionTime = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                regionManager.setRegionTime(player, args[2], regionTime);
                return true;

            case "gift_once":
                if (args.length < 4) {
                    player.sendMessage(Chat.color(cfg.getPrefix() +
                            "&cUżyj: /" + label + " region gift_once <nazwa> <ilość>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[3]);
                    regionManager.setRegionGiftsOnce(player, args[2], amount);
                } catch (NumberFormatException ex) {
                    player.sendMessage(Chat.color(cfg.getPrefix() + "&cIlość musi być liczbą!"));
                }
                return true;

            default:
                return true;
        }
    }

    private void noPermission(Player player, String permission) {
        player.sendMessage(Chat.color(
                cfg.replaceBasic(cfg.getString("messages.no_premission"))
                        .replace("{permission}", permission)
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        if (!command.getName().equalsIgnoreCase("eventswiateczny")) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("patyk", "time", "region", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("region")) {
            list.addAll(Arrays.asList("stworz", "usun", "time", "gift_once"));
        }
        return list;
    }
}
