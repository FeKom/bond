package github.fekom.bond.algorithms;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import github.fekom.bond.domain.enums.TierType;

public class TokenBucket {
	private final TierType tierType;
	// o que cada request consome
	private final long bucketCapacityBytes;
	private final long refillRateBytesPerSecond;
	private final double burstMultiplier;
	private final long maxBurstBytes;
	private long currentBytes;
	private long lastRefillTime;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private String createdAt = LocalDateTime.now().format(formatter);

	public TokenBucket(TierType tierType) {
		this.tierType = tierType;
		this.bucketCapacityBytes = tierType.getCapacityBytes();
		this.refillRateBytesPerSecond = tierType.getRefillRateBytesPerSecond();
		this.burstMultiplier = tierType.getBurstMultiplier();
		this.maxBurstBytes = (long) (bucketCapacityBytes * burstMultiplier);

		this.currentBytes = bucketCapacityBytes;
		this.lastRefillTime = System.currentTimeMillis();
		this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	public boolean allowRequest(long requestSizeBytes) {
		refill();
		if (currentBytes >= requestSizeBytes) {
			currentBytes -= requestSizeBytes;
			return true;
		}
		return false;
	}

	public void refill() {
		long now = System.currentTimeMillis();
		long elapsedMs = (now - lastRefillTime);

		long bytesToAdd = (elapsedMs * refillRateBytesPerSecond) / 1000;
		currentBytes = Math.min(bucketCapacityBytes, currentBytes + bytesToAdd);
		lastRefillTime = now;

	}

	public long getWaitTime(long requestSizeBytes) {
		refill();
		long bytesNeeded = requestSizeBytes - currentBytes;
		if (bytesNeeded <= 0) {
			return 0;
		}
		return (bytesNeeded * 1000) / refillRateBytesPerSecond;
	}

	public long getUsedBytes() {
		return bucketCapacityBytes - currentBytes;
	}

	public double getUsagePercentage() {
		return (getUsedBytes() * 100.0) / bucketCapacityBytes;
	}

	public long getCurrentBytes() {
		return currentBytes;
	}

	public long getLastRefillTime() {
		return lastRefillTime;
	}

	// Setters (para restaurar estado do banco)
	public void setCurrentBytes(long currentBytes) {
		this.currentBytes = currentBytes;
	}

	public void setLastRefillTime(long lastRefillTime) {
		this.lastRefillTime = lastRefillTime;
	}

	public TierType getTierType() {
		return tierType;
	}

	public long getBucketCapacityBytes() {
		return bucketCapacityBytes;
	}

	public long getRefillRateBytesPerSecond() {
		return refillRateBytesPerSecond;
	}

	public double getBurstMultiplier() {
		return burstMultiplier;
	}

	public long getMaxBurstBytes() {
		return maxBurstBytes;
	}

	public String getCreatedAt() {
		return createdAt;
	}
}
