package nl.nationwar.data;

import org.bukkit.Chunk;
import java.util.*;

public class Nation {

    private final String name;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<String> claimedChunks = new HashSet<>();

    public Nation(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getMembers() { return members; }
    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public Set<String> getClaimedChunks() { return claimedChunks; }

    public boolean claimChunk(Chunk chunk) {
        if (claimedChunks.size() >= 50) return false;
        claimedChunks.add(chunkKey(chunk));
        return true;
    }

    public boolean unclaimChunk(Chunk chunk) {
        return claimedChunks.remove(chunkKey(chunk));
    }

    public boolean ownsChunk(Chunk chunk) {
        return claimedChunks.contains(chunkKey(chunk));
    }

    public int getClaimCount() { return claimedChunks.size(); }

    public static String chunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
    }
}
