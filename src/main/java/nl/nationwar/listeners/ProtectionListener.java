package nl.nationwar.listeners;

import nl.nationwar.data.Nation;
import nl.nationwar.data.War;
import nl.nationwar.managers.NationManager;
import nl.nationwar.managers.WarManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    private final NationManager nationManager;
    private final WarManager warManager;

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
        event.blockList().removeIf(block -> {
            Nation owner = nationManager.getNationAt(block.getChunk());
            if (owner == null) return false;
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
