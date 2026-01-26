package github.fekom.bond.algorithms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TokenBucket {

	private final TierType tierType;
	// o que cada request consome
	private final long bucketCapacityBytes;
	private final long refillRateBytesPerSecond;
	private final double burstMultiplier;
	private final long maxBurstBytes;
	private long currentBytes;
	private long lastRefillTime;
	private LocalDateTime createdAt;

	public TokenBucket(TierType tierType) {
		this.tierType = tierType;
		this.bucketCapacityBytes = tierType.getCapacityBytes();
		this.refillRateBytesPerSecond = tierType.getRefillRateBytesPerSecond();
		this.burstMultiplier = tierType.getBurstMultiplier();
		this.maxBurstBytes = (long) (bucketCapacityBytes * burstMultiplier);

		this.currentBytes = bucketCapacityBytes;
		this.lastRefillTime = System.currentTimeMillis();
		this.createdAt = LocalDateTime.now();
	}

	// Construtor para deserialização JSON
	@JsonCreator
	public TokenBucket(
		@JsonProperty("tierType") TierType tierType,
		@JsonProperty("bucketCapacityBytes") long bucketCapacityBytes,
		@JsonProperty("refillRateBytesPerSecond") long refillRateBytesPerSecond,
		@JsonProperty("burstMultiplier") double burstMultiplier,
		@JsonProperty("maxBurstBytes") long maxBurstBytes,
		@JsonProperty("currentBytes") long currentBytes,
		@JsonProperty("lastRefillTime") long lastRefillTime,
		@JsonProperty("createdAt") LocalDateTime createdAt
	) {
		this.tierType = tierType;
		this.bucketCapacityBytes = bucketCapacityBytes;
		this.refillRateBytesPerSecond = refillRateBytesPerSecond;
		this.burstMultiplier = burstMultiplier;
		this.maxBurstBytes = maxBurstBytes;
		this.currentBytes = currentBytes;
		this.lastRefillTime = lastRefillTime;
		this.createdAt = createdAt;
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

	@JsonIgnore
	public long getUsedBytes() {
		return bucketCapacityBytes - currentBytes;
	}

	@JsonIgnore
	public double getUsagePercentage() {
		return (getUsedBytes() * 100.0) / bucketCapacityBytes;
	}

	public long getCurrentBytes() {
		return currentBytes;
	}

	public long getLastRefillTime() {
		return lastRefillTime;
	}

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
