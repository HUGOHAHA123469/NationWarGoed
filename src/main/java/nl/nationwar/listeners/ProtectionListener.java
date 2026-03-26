package nl.nationwar.listeners;

import nl.nationwar.data.Nation;
import nl.nationwar.data.War;
import nl.nationwar.managers.NationManager;
import nl.nationwar.managers.WarManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Chunk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionListener implements Listener {

    private final NationManager nationManager;
    private final WarManager warManager;
    private final Map<UUID, String> playerCurrentNation = new HashMap<>();

    public ProtectionListener(NationManager nationManager, WarManager warManager) {
        this.nationManager = nationManager;
        this.warManager = warManager;
    }

    private boolean isProtected(Player player, Block block) {
        Nation owner = nationManager.getNationAt(block.getChunk());
        if (owner == null) return false;
        Nation playerNation = nationManager.getNationOf(player);
        if (playerNation != null && playerNation.getName().equalsIgnoreCase(owner.getName())) return false;
        if (playerNation != null && warManager.areAtWar(playerNation.getName(), owner.getName())) return false;
        return true;
    }

    private boolean isWarActive(Nation owner) {
        for (War w : warManager.getWars()) {
            if (w.isActive() && w.involves(owner.getName())) return true;
        }
        return false;
    }

    private boolean isCreeperExplosion(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Creeper) return true;
        if (entity instanceof Vehicle) {
            for (Entity passenger : entity.getPassengers()) {
                if (passenger instanceof Creeper) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nationManager.updatePlayerDisplayName(event.getPlayer());
        Nation nation = nationManager.getNationAt(event.getPlayer().getLocation().getChunk());
        if (nation != null) {
            playerCurrentNation.put(event.getPlayer().getUniqueId(), nation.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerCurrentNation.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Chunk toChunk = event.getTo().getChunk();
        Nation toNation = nationManager.getNationAt(toChunk);

        String toNationName = toNation != null ? toNation.getName() : null;
        String currentNationName = playerCurrentNation.get(player.getUniqueId());

        boolean changed = (toNationName == null && currentNationName != null)
                || (toNationName != null && !toNationName.equals(currentNationName));

        if (!changed) return;

        if (toNationName != null) {
            playerCurrentNation.put(player.getUniqueId(), toNationName);
        } else {
            playerCurrentNation.remove(player.getUniqueId());
        }

        if (toNation != null) {
            Nation playerNation = nationManager.getNationOf(player);
            boolean isOwn = playerNation != null && playerNation.getName().equalsIgnoreCase(toNation.getName());
            String titleColor = isOwn ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            String subtitle = isOwn ? ChatColor.GRAY + "Your territory" : ChatColor.GRAY + "Foreign territory";
            player.sendTitle(titleColor + toNation.getName(), subtitle, 10, 40, 10);
        } else {
            player.sendTitle(ChatColor.WHITE + "Wilderness", ChatColor.GRAY + "Unclaimed territory", 10, 40, 10);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This area belongs to another nation!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This area belongs to another nation!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        if (isProtected(event.getPlayer(), block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This area belongs to another nation!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFireballLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Fireball) {
            if (event.getEntity().getShooter() instanceof Player) {
                Player player = (Player) event.getEntity().getShooter();
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Fireballs are disabled on this server!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Material type = block.getType();
        boolean isContainer = type == Material.CHEST
                || type == Material.BARREL
                || type == Material.TRAPPED_CHEST
                || type.name().contains("SHULKER_BOX")
                || type == Material.ENDER_CHEST
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER;
        if (!isContainer) return;
        if (isProtected(event.getPlayer(), block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot access containers in another nation's territory!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplosion(EntityExplodeEvent event) {
        boolean isCreeper = isCreeperExplosion(event);
        event.blockList().removeIf(block -> {
            Nation owner = nationManager.getNationAt(block.getChunk());
            if (owner == null) return false;
            if (isCreeper) return true;
            return !isWarActive(owner);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Nation owner = nationManager.getNationAt(block.getChunk());
            if (owner == null) return false;
            return !isWarActive(owner);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() != Material.FIRE) return;
        Nation owner = nationManager.getNationAt(event.getBlock().getChunk());
        if (owner == null) return;
        if (!isWarActive(owner)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        Nation owner = nationManager.getNationAt(event.getBlock().getChunk());
        if (owner == null) return;
        if (!isWarActive(owner)) event.setCancelled(true);
    }
}
