package github.fekom.bond.api;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.Capacity;
import java.util.Optional;

/**
 * Storage abstraction for token buckets and per-IP capacity overrides.
 * <p>
 * The default implementation is {@link github.fekom.bond.infrastructure.InMemoryBucketStore},
 * which stores everything in memory. For persistence across restarts, add
 * {@code spring-boot-starter-data-jpa} to your classpath and Bond will auto-configure
 * {@link github.fekom.bond.infrastructure.repository.JpaBucketStore}.
 * <p>
 * You can also provide your own implementation (e.g. Redis-backed) by declaring
 * a {@code BucketStore} bean in your application context.
 */
public interface BucketStore {

	/** Finds the token bucket for the given IP and endpoint, if one exists. */
	Optional<TokenBucket> findBucket(String ipAddress, String endpoint);

	/** Persists the token bucket state for the given IP and endpoint. */
	void saveBucket(String ipAddress, String endpoint, TokenBucket bucket);

	/** Finds a per-IP capacity override, if one exists. */
	Optional<Capacity> findCapacityByIp(String ipAddress);

	/** Saves a per-IP capacity override. */
	void saveCapacityOverride(String ipAddress, Capacity capacity);

	/** Checks whether the given IP address is blocked. */
	boolean isBlocked(String ipAddress);

	/** Blocks the given IP address with an optional reason. */
	void blockIp(String ipAddress, String reason);

	/** Unblocks the given IP address. Returns {@code true} if it was previously blocked. */
	boolean unblockIp(String ipAddress);
}
