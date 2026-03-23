package nl.nationwar.managers;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import nl.nationwar.data.War;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WarManager {

    private final NationWar plugin;
    private final NationManager nationManager;
    private final List<War> wars = new ArrayList<>();
    private final File dataFile;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm");

    public WarManager(NationWar plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.dataFile = new File(plugin.getDataFolder(), "wars.yml");
    }

    public String declareWar(String attackerName, String defenderName) {
        Nation attacker = nationManager.getNation(attackerName);
        Nation defender = nationManager.getNation(defenderName);
        if (attacker == null) return "Your nation does not exist.";
        if (defender == null) return "The target nation does not exist.";
        if (attackerName.equalsIgnoreCase(defenderName)) return "You cannot declare war on your own nation.";
        for (War w : wars) {
            if (w.isAtWar(attackerName, defenderName)) return "There is already a war between these nations.";
        }
        long now = System.currentTimeMillis();
        War war = new War(attackerName, defenderName, now); // starts 24h later
        wars.add(war);
        save();

        String startTimeStr = timeFormat.format(new Date(war.getStartTime()));
        long hoursUntil = (war.getStartTime() - now) / 1000 / 3600;
        long minutesUntil = ((war.getStartTime() - now) / 1000 / 60) % 60;

        Bukkit.broadcastMessage(
            ChatColor.RED + "" + ChatColor.BOLD + "WAR DECLARED!" + ChatColor.RESET
            + ChatColor.RED + " " + attackerName + " has declared war on " + defenderName + "!"
            + "\n" + ChatColor.GOLD + "War begins in " + hoursUntil + "h " + minutesUntil + "m (at " + startTimeStr + ")."
            + "\n" + ChatColor.GRAY + "The war will last a maximum of 48 hours."
        );
        return null;
    }

    public boolean areAtWar(String a, String b) {
        for (War w : wars) {
            if (w.isAtWar(a, b) && w.isActive()) return true;
        }
        return false;
    }

    public void startWarTimer() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            boolean changed = false;

            for (War w : new ArrayList<>(wars)) {
                if (!w.isActive() && now >= w.getStartTime()) {
                    w.setActive(true);
                    changed = true;
                    Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.broadcastMessage(
                            ChatColor.DARK_RED + "" + ChatColor.BOLD + "WAR HAS BEGUN!" + ChatColor.RESET
                            + ChatColor.RED + " " + w.getAttackerName() + " vs " + w.getDefenderName() + "!"
                            + "\n" + ChatColor.RED + "Land protection is disabled between these nations. War lasts 48 hours!"
                        )
                    );
                } else if (w.isActive() && now >= w.getEndTime()) {
                    wars.remove(w);
                    changed = true;
                    Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.broadcastMessage(
                            ChatColor.GREEN + "" + ChatColor.BOLD + "WAR OVER!" + ChatColor.RESET
                            + ChatColor.GREEN + " The war between " + w.getAttackerName() + " and " + w.getDefenderName() + " has ended."
                            + "\n" + ChatColor.GRAY + "Land protection has been restored."
                        )
                    );
                }
            }

            for (War w : new ArrayList<>(wars)) {
                if (!w.isActive()) {
                    long msLeft = w.getStartTime() - now;
                    long minutesLeft = msLeft / 1000 / 60;
                    if (minutesLeft == 60 || minutesLeft == 30 || minutesLeft == 10 || minutesLeft == 5) {
                        String startStr = timeFormat.format(new Date(w.getStartTime()));
                        final long ml = minutesLeft;
                        Bukkit.getScheduler().runTask(plugin, () ->
                            Bukkit.broadcastMessage(
                                ChatColor.GOLD + "" + ChatColor.BOLD + "WAR INCOMING!" + ChatColor.RESET
                                + ChatColor.GOLD + " War between " + w.getAttackerName() + " and " + w.getDefenderName()
                                + " starts in " + ml + " minute(s) (at " + startStr + ")!"
                            )
                        );
                    }
                }
            }

            if (changed) save();
        }, 20L * 30, 20L * 30);
    }

    public List<War> getWars() { return wars; }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < wars.size(); i++) {
            War w = wars.get(i);
            String path = "wars." + i;
            config.set(path + ".attacker", w.getAttackerName());
            config.set(path + ".defender", w.getDefenderName());
            config.set(path + ".declaredAt", w.getDeclaredAt());
            config.set(path + ".startTime", w.getStartTime());
            config.set(path + ".endTime", w.getEndTime());
            config.set(path + ".active", w.isActive());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save wars.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.isConfigurationSection("wars")) return;
        for (String key : config.getConfigurationSection("wars").getKeys(false)) {
            String path = "wars." + key;
            wars.add(new War(
                config.getString(path + ".attacker"),
                config.getString(path + ".defender"),
                config.getLong(path + ".declaredAt"),
                config.getLong(path + ".startTime"),
                config.getLong(path + ".endTime"),
                config.getBoolean(path + ".active")
            ));
        }
        plugin.getLogger().info("Loaded " + wars.size() + " war(s).");
    }
}
