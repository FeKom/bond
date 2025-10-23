package github.fekom.bond.domain.enums;

public enum TierType {
    FREE(
        32_768,                 // 32KB capacity
        32_768 / 3600,          // 32KB/hour refill
        1.5,                    // 150% burst
        "Free tier - 432 requests/hour"
    ),
    
    STARTUP(
        10 * 1024 * 1024,       // 10MB capacity  
        (10 * 1024 * 1024) / 3600, // 10MB/hour
        2.0,                    // 200% burst
        "Startup - 140K requests/hour"
    ),
    
    ENTERPRISE(
        1 * 1024 * 1024 * 1024, // 1GB capacity
        (1 * 1024 * 1024 * 1024) / 3600, // 1GB/hour
        3.0,                    // 300% burst
        "Enterprise - 14M requests/hour"
    );

    public final long capacityBytes;
    public final long refillRateBytesPerSecond;
    public final double burstMultiplier;
    public final String description;
    
        TierType(long capacityBytes, long refillRateBytesPerSecond, double burstMultiplier, String description) {
        this.capacityBytes = capacityBytes;
        this.refillRateBytesPerSecond = refillRateBytesPerSecond;
        this.burstMultiplier = burstMultiplier;
        this.description = description;
    }

    public long getCapacityBytes() { return capacityBytes; }
    public long getRefillRateBytesPerSecond() { return refillRateBytesPerSecond; }
    public double getBurstMultiplier() { return burstMultiplier; }
    public String getDescription() { return description; }
}
