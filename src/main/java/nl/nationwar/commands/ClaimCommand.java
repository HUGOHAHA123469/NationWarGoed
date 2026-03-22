package nl.nationwar.commands;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import nl.nationwar.managers.NationManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand implements CommandExecutor {

    private final NationWar plugin;
    private final NationManager nationManager;

    public ClaimCommand(NationWar plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();
        if (label.equalsIgnoreCase("claim")) {
            String error = nationManager.claimChunk(player, chunk);
            if (error != null) {
                player.sendMessage(ChatColor.RED + error);
            } else {
                Nation nation = nationManager.getNationOf(player);
                player.sendMessage(ChatColor.GREEN + "Chunk claimed! (" + nation.getClaimCount() + "/50)");
            }
        } else if (label.equalsIgnoreCase("unclaim")) {
            String error = nationManager.unclaimChunk(player, chunk);
            if (error != null) {
                player.sendMessage(ChatColor.RED + error);
            } else {
                Nation nation = nationManager.getNationOf(player);
                player.sendMessage(ChatColor.YELLOW + "Chunk unclaimed. (" + nation.getClaimCount() + "/50)");
            }
        }
        return true;
    }
}
