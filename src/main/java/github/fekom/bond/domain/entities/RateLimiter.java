package github.fekom.bond.domain.entities;

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import github.fekom.bond.algorithms.TokenBucket;

public record RateLimiter(
    String id,
    String clientId,
    String endPoint,
    TokenBucket bucket

) {
     public boolean isAllowed(long requestSizeBytes) {
        return bucket.allowRequest(requestSizeBytes);
    }
    
    public long getWaitTimeMs(long requestSizeBytes) {
        return bucket.getWaitTime(requestSizeBytes);
    }
    public void refill(){
        bucket.refill();
    }
    public long usedBytes() {
        return bucket.getUsedBytes();
    }
    public double usagePercentage() {
        return bucket.getUsagePercentage();
    }

    public static RateLimiter create(String clientId, String endPoint, TokenBucket bucket) {
        UUID uuid = Generators.timeBasedEpochGenerator().generate();
        return new RateLimiter(uuid.toString(), clientId, endPoint, bucket);
    }
    
}
