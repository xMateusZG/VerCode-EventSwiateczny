package pl.verCodeEventSwiateczny.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.verCodeEventSwiateczny.gui.GiftGUI;

public class PrezentyCommand implements CommandExecutor {

    private final GiftGUI giftGUI;

    public PrezentyCommand(GiftGUI giftGUI) {
        this.giftGUI = giftGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracz może użyć tej komendy.");
            return true;
        }
        giftGUI.openGiftGUI((Player) sender);
        return true;
    }
}
