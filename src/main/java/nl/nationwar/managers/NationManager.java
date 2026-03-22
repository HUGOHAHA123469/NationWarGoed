package nl.nationwar.managers;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NationManager {

    private final NationWar plugin;
    private final Map<String, Nation> nations = new HashMap<>();
    private final Map<UUID, String> playerNation = new HashMap<>();
    private final File dataFile;

    public NationManager(NationWar plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "nations.yml");
    }

    public String createNation(Player leader, String name) {
        if (name.length() > 20) return "Nation name cannot exceed 20 characters.";
        if (!name.matches("[a-zA-Z0-9_]+")) return "Nation name may only contain letters, numbers and underscores.";
        if (nations.containsKey(name.toLowerCase())) return "A nation with that name already exists.";
        if (playerNation.containsKey(leader.getUniqueId())) return "You are already in a nation.";
        Nation nation = new Nation(name, leader.getUniqueId());
        nations.put(name.toLowerCase(), nation);
        playerNation.put(leader.getUniqueId(), name.toLowerCase());
        save();
        return null;
    }

    public String disbandNation(Player player) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (!nation.getLeader().equals(player.getUniqueId())) return "Only the leader can disband the nation.";
        for (UUID uuid : nation.getMembers()) playerNation.remove(uuid);
        nations.remove(nation.getName().toLowerCase());
        save();
        return null;
    }

    public String joinNation(Player player, String name) {
        if (playerNation.containsKey(player.getUniqueId())) return "You are already in a nation.";
        Nation nation = nations.get(name.toLowerCase());
        if (nation == null) return "Nation not found.";
        nation.addMember(player.getUniqueId());
        playerNation.put(player.getUniqueId(), name.toLowerCase());
        save();
        return null;
    }

    public String leaveNation(Player player) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (nation.getLeader().equals(player.getUniqueId())) return "You are the leader. Use /nation disband to disband the nation.";
        nation.removeMember(player.getUniqueId());
        playerNation.remove(player.getUniqueId());
        save();
        return null;
    }

    public Nation getNationOf(Player player) {
        String name = playerNation.get(player.getUniqueId());
        if (name == null) return null;
        return nations.get(name);
    }

    public Nation getNation(String name) {
        return nations.get(name.toLowerCase());
    }

    public Nation getNationAt(Chunk chunk) {
        for (Nation n : nations.values()) {
            if (n.ownsChunk(chunk)) return n;
        }
        return null;
    }

    public String claimChunk(Player player, Chunk chunk) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (!nation.getLeader().equals(player.getUniqueId())) return "Only the nation leader can claim chunks.";
        if (getNationAt(chunk) != null) return "This chunk is already claimed.";
        if (!nation.claimChunk(chunk)) return "Your nation has reached the maximum of 50 claimed chunks.";
        save();
        return null;
    }

    public String unclaimChunk(Player player, Chunk chunk) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (!nation.getLeader().equals(player.getUniqueId())) return "Only the nation leader can unclaim chunks.";
        Nation owner = getNationAt(chunk);
        if (owner == null || !owner.getName().equalsIgnoreCase(nation.getName())) return "Your nation does not own this chunk.";
        nation.unclaimChunk(chunk);
        save();
        return null;
    }

    public Collection<Nation> getAllNations() { return nations.values(); }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Nation> entry : nations.entrySet()) {
            Nation n = entry.getValue();
            String path = "nations." + entry.getKey();
            config.set(path + ".name", n.getName());
            config.set(path + ".leader", n.getLeader().toString());
            List<String> members = new ArrayList<>();
            for (UUID uuid : n.getMembers()) members.add(uuid.toString());
            config.set(path + ".members", members);
            config.set(path + ".chunks", new ArrayList<>(n.getClaimedChunks()));
        }
        try {
