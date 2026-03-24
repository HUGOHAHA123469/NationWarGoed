package nl.nationwar.managers;

import nl.nationwar.NationWar;
import nl.nationwar.data.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
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

    // Spawn chunk protection: 5x5 around spawn = chunks from -2 to +2 in both axes
    private static final int SPAWN_CHUNK_RADIUS = 2;

    public NationManager(NationWar plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "nations.yml");
    }

    public boolean isSpawnChunk(Chunk chunk) {
        World world = chunk.getWorld();
        if (!world.getName().equals("world")) return false;
        Location spawn = world.getSpawnLocation();
        int spawnChunkX = spawn.getBlockX() >> 4;
        int spawnChunkZ = spawn.getBlockZ() >> 4;
        return Math.abs(chunk.getX() - spawnChunkX) <= SPAWN_CHUNK_RADIUS
                && Math.abs(chunk.getZ() - spawnChunkZ) <= SPAWN_CHUNK_RADIUS;
    }

    public String createNation(Player leader, String name) {
        if (name.length() > 20) return "Nation name cannot exceed 20 characters.";
        if (!name.matches("[a-zA-Z0-9_]+")) return "Nation name may only contain letters, numbers and underscores.";
        if (nations.containsKey(name.toLowerCase())) return "A nation with that name already exists.";
        if (playerNation.containsKey(leader.getUniqueId())) return "You are already in a nation.";
        Nation nation = new Nation(name, leader.getUniqueId());
        nations.put(name.toLowerCase(), nation);
        playerNation.put(leader.getUniqueId(), name.toLowerCase());
        updatePlayerDisplayName(leader);
        save();
        return null;
    }

    public String disbandNation(Player player) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (!nation.getLeader().equals(player.getUniqueId())) return "Only the leader can disband the nation.";
        for (UUID uuid : nation.getMembers()) {
            playerNation.remove(uuid);
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                resetPlayerDisplayName(member);
            }
        }
        nations.remove(nation.getName().toLowerCase());
        save();
        return null;
    }

    public String invitePlayer(Player leader, Player target) {
        Nation nation = getNationOf(leader);
        if (nation == null) return "You are not in a nation.";
        if (!nation.getLeader().equals(leader.getUniqueId())) return "Only the leader can invite players.";
        if (nation.isFull()) return "Your nation is full (max " + Nation.MAX_MEMBERS + " members).";
        if (playerNation.containsKey(target.getUniqueId())) return target.getName() + " is already in a nation.";
        if (nation.hasInvite(target.getUniqueId())) return target.getName() + " already has a pending invite.";
        nation.addInvite(target.getUniqueId());
        save();
        return null;
    }

    public String acceptInvite(Player player, String nationName) {
        if (playerNation.containsKey(player.getUniqueId())) return "You are already in a nation.";
        Nation nation = nations.get(nationName.toLowerCase());
        if (nation == null) return "Nation not found.";
        if (!nation.hasInvite(player.getUniqueId())) return "You do not have an invite from that nation.";
        if (nation.isFull()) return "That nation is now full.";
        nation.removeInvite(player.getUniqueId());
        nation.addMember(player.getUniqueId());
        playerNation.put(player.getUniqueId(), nationName.toLowerCase());
        updatePlayerDisplayName(player);
        save();
        return null;
    }
    public String joinNation(Player player, String nationName) {
    return acceptInvite(player, nationName);
}

    public String leaveNation(Player player) {
        Nation nation = getNationOf(player);
        if (nation == null) return "You are not in a nation.";
        if (nation.getLeader().equals(player.getUniqueId())) return "You are the leader. Use /nation disband to disband the nation.";
        nation.removeMember(player.getUniqueId());
        playerNation.remove(player.getUniqueId());
        resetPlayerDisplayName(player);
        save();
        return null;
    }

    public Nation getNationOf(Player player) {
        String name = playerNation.get(player.getUniqueId());
        if (name == null) return null;
        return nations.get(name);
    }

    public Nation getNationOfUUID(UUID uuid) {
        String name = playerNation.get(uuid);
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
        if (isSpawnChunk(chunk)) return "You cannot claim spawn chunks!";
        if (getNationAt(chunk) != null) return "This chunk is already claimed.";
        if (!nation.claimChunk(chunk)) return "Your nation has reached the maximum of " + Nation.MAX_CLAIMS + " claimed chunks.";
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

    public void updatePlayerDisplayName(Player player) {
        Nation nation = getNationOf(player);
        if (nation == null) {
            resetPlayerDisplayName(player);
            return;
        }
        String display = player.getName() + " §6" + nation.getName();
        player.setDisplayName(display);
        player.setPlayerListName(display);
    }

    public void resetPlayerDisplayName(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
    }

    public void updateAllDisplayNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplayName(player);
        }
    }

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
            List<String> invites = new ArrayList<>();
            for (UUID uuid : n.getPendingInvites()) invites.add(uuid.toString());
            config.set(path + ".invites", invites);
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save nations.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.isConfigurationSection("nations")) return;
        for (String key : config.getConfigurationSection("nations").getKeys(false)) {
            String path = "nations." + key;
            String name = config.getString(path + ".name");
            UUID leader = UUID.fromString(config.getString(path + ".leader"));
            Nation nation = new Nation(name, leader);
            for (String m : config.getStringList(path + ".members")) nation.addMember(UUID.fromString(m));
            nation.getClaimedChunks().addAll(config.getStringList(path + ".chunks"));
            for (String i : config.getStringList(path + ".invites")) nation.addInvite(UUID.fromString(i));
            nations.put(key, nation);
            for (UUID uuid : nation.getMembers()) playerNation.put(uuid, key);
        }
        plugin.getLogger().info("Loaded " + nations.size() + " nations.");
    }
}
