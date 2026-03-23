package nl.nationwar.commands;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import nl.nationwar.managers.NationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NationCommand implements CommandExecutor {

    private final NationWar plugin;
    private final NationManager nationManager;

    public NationCommand(NationWar plugin, NationManager nationManager) {
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
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {

            case "create":
                if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /nation create <name>"); return true; }
                String error = nationManager.createNation(player, args[1]);
                if (error != null) {
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Nation '" + args[1] + "' created! You are the leader.");
                    Bukkit.broadcastMessage(ChatColor.GOLD + "A new nation has been founded: " + ChatColor.YELLOW + args[1] + ChatColor.GOLD + " by " + player.getName() + "!");
                }
                break;

            case "invite":
                if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /nation invite <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage(ChatColor.RED + "Player not found or offline."); return true; }
                String inviteError = nationManager.invitePlayer(player, target);
                if (inviteError != null) {
                    player.sendMessage(ChatColor.RED + inviteError);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Invite sent to " + target.getName() + "!");
                    target.sendMessage(ChatColor.GOLD + "You have been invited to join nation '"
                        + ChatColor.YELLOW + nationManager.getNationOf(player).getName()
                        + ChatColor.GOLD + "'! Type " + ChatColor.WHITE + "/nation accept "
                        + nationManager.getNationOf(player).getName() + ChatColor.GOLD + " to accept.");
                }
                break;

            case "accept":
                if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /nation accept <nation>"); return true; }
                String acceptError = nationManager.acceptInvite(player, args[1]);
                if (acceptError != null) {
                    player.sendMessage(ChatColor.RED + acceptError);
                } else {
                    player.sendMessage(ChatColor.GREEN + "You have joined nation '" + args[1] + "'!");
                    Nation joined = nationManager.getNation(args[1]);
                    if (joined != null) {
                        for (java.util.UUID uuid : joined.getMembers()) {
                            Player member = Bukkit.getPlayer(uuid);
                            if (member != null && !member.equals(player))
                                member.sendMessage(ChatColor.YELLOW + player.getName() + " has joined your nation!");
                        }
                    }
                }
                break;

            case "leave":
                String leaveError = nationManager.leaveNation(player);
                if (leaveError != null) {
                    player.sendMessage(ChatColor.RED + leaveError);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You have left your nation.");
                }
                break;

            case "disband":
                Nation myNation = nationManager.getNationOf(player);
                if (myNation == null) { player.sendMessage(ChatColor.RED + "You are not in a nation."); return true; }
                String disbandName = myNation.getName();
                String disbandError = nationManager.disbandNation(player);
                if (disbandError != null) {
                    player.sendMessage(ChatColor.RED + disbandError);
                } else {
                    Bukkit.broadcastMessage(ChatColor.GRAY + "The nation '" + disbandName + "' has been disbanded.");
                }
                break;

            case "info":
                String infoName = args.length >= 2 ? args[1] : (nationManager.getNationOf(player) != null ? nationManager.getNationOf(player).getName() : null);
                if (infoName == null) { player.sendMessage(ChatColor.RED + "Usage: /nation info <name>"); return true; }
                Nation infoNation = nationManager.getNation(infoName);
                if (infoNation == null) { player.sendMessage(ChatColor.RED + "Nation not found."); return true; }
                OfflinePlayer leaderPlayer = Bukkit.getOfflinePlayer(infoNation.getLeader());
                player.sendMessage(ChatColor.GOLD + "=== " + infoNation.getName() + " ===");
                player.sendMessage(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE + (leaderPlayer.getName() != null ? leaderPlayer.getName() : "Unknown"));
                player.sendMessage(ChatColor.YELLOW + "Members: " + ChatColor.WHITE + infoNation.getMembers().size() + "/" + Nation.MAX_MEMBERS);
                player.sendMessage(ChatColor.YELLOW + "Claimed chunks: " + ChatColor.WHITE + infoNation.getClaimCount() + "/" + Nation.MAX_CLAIMS);
                break;

            case "list":
                player.sendMessage(ChatColor.GOLD + "=== All Nations ===");
                if (nationManager.getAllNations().isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No nations have been created yet.");
                } else {
                    for (Nation n : nationManager.getAllNations()) {
                        player.sendMessage(ChatColor.YELLOW + "- " + n.getName()
                                + ChatColor.GRAY + " (" + n.getMembers().size() + "/" + Nation.MAX_MEMBERS + " members, " + n.getClaimCount() + " chunks)");
                    }
                }
                break;

            default:
                sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== NationWar Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/nation create <name>" + ChatColor.WHITE + " - Create a new nation");
        player.sendMessage(ChatColor.YELLOW + "/nation invite <player>" + ChatColor.WHITE + " - Invite a player (leader only)");
        player.sendMessage(ChatColor.YELLOW + "/nation accept <nation>" + ChatColor.WHITE + " - Accept an invite");
        player.sendMessage(ChatColor.YELLOW + "/nation leave" + ChatColor.WHITE + " - Leave your nation");
        player.sendMessage(ChatColor.YELLOW + "/nation disband" + ChatColor.WHITE + " - Disband your nation (leader only)");
        player.sendMessage(ChatColor.YELLOW + "/nation info [name]" + ChatColor.WHITE + " - View nation info");
        player.sendMessage(ChatColor.YELLOW + "/nation list" + ChatColor.WHITE + " - List all nations");
        player.sendMessage(ChatColor.YELLOW + "/claim" + ChatColor.WHITE + " - Claim the chunk you are standing in");
        player.sendMessage(ChatColor.YELLOW + "/unclaim" + ChatColor.WHITE + " - Unclaim the chunk you are standing in");
        player.sendMessage(ChatColor.YELLOW + "/war declare <nation>" + ChatColor.WHITE + " - Declare war on a nation");
        player.sendMessage(ChatColor.YELLOW + "/war status" + ChatColor.WHITE + " - View active and upcoming wars");
    }
}
