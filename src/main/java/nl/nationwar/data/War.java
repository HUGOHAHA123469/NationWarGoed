package nl.nationwar.data;

public class War {

    private final String attackerName;
    private final String defenderName;
    private final long declaredAt;
    private final long startTime;
    private final long endTime;
    private boolean active;

    public War(String attackerName, String defenderName, long declaredAt) {
        this.attackerName = attackerName;
        this.defenderName = defenderName;
        this.declaredAt = declaredAt;
        this.startTime = declaredAt + (24L * 60 * 60 * 1000);
        this.endTime = startTime + (48L * 60 * 60 * 1000);
        this.active = false;
    }

    public War(String attackerName, String defenderName, long declaredAt, long startTime, long endTime, boolean active) {
        this.attackerName = attackerName;
        this.defenderName = defenderName;
        this.declaredAt = declaredAt;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    public String getAttackerName() { return attackerName; }
    public String getDefenderName() { return defenderName; }
    public long getDeclaredAt() { return declaredAt; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean involves(String nationName) {
        return attackerName.equalsIgnoreCase(nationName) || defenderName.equalsIgnoreCase(nationName);
    }

    public boolean isAtWar(String a, String b) {
        return (attackerName.equalsIgnoreCase(a) && defenderName.equalsIgnoreCase(b))
                || (attackerName.equalsIgnoreCase(b) && defenderName.equalsIgnoreCase(a));
    }
}
