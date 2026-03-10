package github.fekom.bond.infrastructure;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.api.BucketStore;
import github.fekom.bond.domain.Capacity;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory {@link BucketStore} implementation using {@link ConcurrentHashMap}.
 * <p>
 * State is lost on application restart. For persistence, add
 * {@code spring-boot-starter-data-jpa} to auto-configure the JPA-backed store,
 * or provide your own {@link BucketStore} bean.
 */
public class InMemoryBucketStore implements BucketStore {

	private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Capacity> ipOverrides = new ConcurrentHashMap<>();
	private final ConcurrentHashMap.KeySetView<String, Boolean> blockedIps = ConcurrentHashMap.newKeySet();

	@Override
	public Optional<TokenBucket> findBucket(String ipAddress, String endpoint) {
		return Optional.ofNullable(buckets.get(key(ipAddress, endpoint)));
	}

	@Override
	public void saveBucket(String ipAddress, String endpoint, TokenBucket bucket) {
		buckets.put(key(ipAddress, endpoint), bucket);
	}

	@Override
	public Optional<Capacity> findCapacityByIp(String ipAddress) {
		return Optional.ofNullable(ipOverrides.get(ipAddress));
	}

	@Override
	public void saveCapacityOverride(String ipAddress, Capacity capacity) {
		ipOverrides.put(ipAddress, capacity);
	}

	@Override
	public boolean isBlocked(String ipAddress) {
		return blockedIps.contains(ipAddress);
	}

	@Override
	public void blockIp(String ipAddress, String reason) {
		blockedIps.add(ipAddress);
	}

	@Override
	public boolean unblockIp(String ipAddress) {
		return blockedIps.remove(ipAddress);
	}

	private String key(String ipAddress, String endpoint) {
		return ipAddress + "|" + endpoint;
	}
}
