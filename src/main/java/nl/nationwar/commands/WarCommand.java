package nl.nationwar.commands;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import nl.nationwar.data.War;
import nl.nationwar.managers.NationManager;
import nl.nationwar.managers.WarManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WarCommand implements CommandExecutor {

    private final NationWar plugin;
    private final NationManager nationManager;
    private final WarManager warManager;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm");

    public WarCommand(NationWar plugin, NationManager nationManager, WarManager warManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.warManager = warManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/war declare <nation>" + ChatColor.WHITE + " - Declare war on a nation");
            player.sendMessage(ChatColor.YELLOW + "/war status" + ChatColor.WHITE + " - View active and upcoming wars");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "declare":
                if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /war declare <nation>"); return true; }
                Nation myNation = nationManager.getNationOf(player);
                if (myNation == null) { player.sendMessage(ChatColor.RED + "You are not in a nation."); return true; }
                if (!myNation.getLeader().equals(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Only the nation leader can declare war."); return true; }
                String declareError = warManager.declareWar(myNation.getName(), args[1]);
                if (declareError != null) {
                    player.sendMessage(ChatColor.RED + declareError);
                } else {
                    for (War w : warManager.getWars()) {
                        if (w.isAtWar(myNation.getName(), args[1]) && !w.isActive()) {
                            player.sendMessage(ChatColor.GREEN + "War declared! It will begin at " + ChatColor.YELLOW + timeFormat.format(new Date(w.getStartTime())) + ChatColor.GREEN + ".");
                            break;
                        }
                    }
                }
                break;

            case "status":
                List<War> wars = warManager.getWars();
                if (wars.isEmpty()) { player.sendMessage(ChatColor.GRAY + "There are no active or upcoming wars."); return true; }
                player.sendMessage(ChatColor.GOLD + "=== War Status ===");
                long now = System.currentTimeMillis();
                for (War w : wars) {
                    if (w.isActive()) {
                        long msLeft = w.getEndTime() - now;
                        long hoursLeft = msLeft / 1000 / 3600;
                        long minutesLeft = (msLeft / 1000 / 60) % 60;
                        player.sendMessage(ChatColor.RED + "[ACTIVE] " + ChatColor.YELLOW + w.getAttackerName() + ChatColor.RED + " vs " + ChatColor.YELLOW + w.getDefenderName() + ChatColor.GRAY + " - " + hoursLeft + "h " + minutesLeft + "m left (ends: " + dateTimeFormat.format(new Date(w.getEndTime())) + ")");
                    } else {
                        long msLeft = w.getStartTime() - now;
                        long hoursLeft = msLeft / 1000 / 3600;
                        long minutesLeft = (msLeft / 1000 / 60) % 60;
                        player.sendMessage(ChatColor.GOLD + "[PENDING] " + ChatColor.YELLOW + w.getAttackerName() + ChatColor.GOLD + " vs " + ChatColor.YELLOW + w.getDefenderName() + ChatColor.GRAY + " - starts in " + hoursLeft + "h " + minutesLeft + "m (at " + timeFormat.format(new Date(w.getStartTime())) + ")");
                    }
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown sub-command. Use /war declare or /war status.");
        }
        return true;
    }
}
