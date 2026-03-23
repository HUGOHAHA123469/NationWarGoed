package nl.nationwar.data;

import org.bukkit.Chunk;
import java.util.*;

public class Nation {

    private final String name;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<String> claimedChunks = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();

    public static final int MAX_MEMBERS = 5;
    public static final int MAX_CLAIMS = 50;

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
    public boolean isFull() { return members.size() >= MAX_MEMBERS; }

    public Set<UUID> getPendingInvites() { return pendingInvites; }
    public void addInvite(UUID uuid) { pendingInvites.add(uuid); }
    public void removeInvite(UUID uuid) { pendingInvites.remove(uuid); }
    public boolean hasInvite(UUID uuid) { return pendingInvites.contains(uuid); }

    public Set<String> getClaimedChunks() { return claimedChunks; }

    public boolean claimChunk(Chunk chunk) {
        if (claimedChunks.size() >= MAX_CLAIMS) return false;
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

    public static String chunkKey(String world, int x, int z) {
        return world + "," + x + "," + z;
    }
}
