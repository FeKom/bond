package github.fekom.bond.domain;

/**
 * Defines the rate limiting capacity for a token bucket.
 *
 * @param capacityBytes maximum number of bytes the bucket can hold
 * @param refillRateBytesPerSecond rate at which bytes are replenished per second
 * @param burstMultiplier multiplier applied to capacity for burst allowance (e.g. 1.5 = 150%)
 */
public record Capacity(
	long capacityBytes,
	long refillRateBytesPerSecond,
	double burstMultiplier
) {
	public Capacity {
		if (capacityBytes <= 0) throw new IllegalArgumentException("capacityBytes must be positive");
		if (refillRateBytesPerSecond <= 0) throw new IllegalArgumentException("refillRateBytesPerSecond must be positive");
		if (burstMultiplier <= 0) throw new IllegalArgumentException("burstMultiplier must be positive");
	}
}
