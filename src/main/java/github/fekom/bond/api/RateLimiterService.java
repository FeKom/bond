package github.fekom.bond.api;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.Capacity;
import github.fekom.bond.domain.RequestResult;
import java.util.Map;

/**
 * Core service that performs byte-based rate limiting using token buckets.
 * <p>
 * Capacity is resolved in order: per-IP override &gt; per-endpoint config &gt; global default.
 * <p>
 * This bean is auto-configured by Bond. If you need to call it manually
 * (instead of relying on {@link github.fekom.bond.infrastructure.web.BondRateLimitFilter}),
 * inject it and call {@link #checkRateLimit(String, String, long)}.
 */
public class RateLimiterService {

	private final BucketStore bucketStore;
	private final Capacity defaultCapacity;
	private final Map<String, Capacity> endpointCapacities;

	public RateLimiterService(BucketStore bucketStore, Capacity defaultCapacity, Map<String, Capacity> endpointCapacities) {
		this.bucketStore = bucketStore;
		this.defaultCapacity = defaultCapacity;
		this.endpointCapacities = endpointCapacities;
	}

	public RequestResult checkRateLimit(String ipAddress, String endpoint, long requestSizeBytes) {
		// 1. Check if IP is blocked
		if (bucketStore.isBlocked(ipAddress)) {
			return RequestResult.blockedResult();
		}

		// 2. Resolve capacity: IP override > endpoint config > global default
		Capacity capacity = bucketStore.findCapacityByIp(ipAddress)
				.orElseGet(() -> endpointCapacities.getOrDefault(endpoint, defaultCapacity));

		// 3. Find existing bucket or create new one
		TokenBucket bucket = bucketStore.findBucket(ipAddress, endpoint)
				.orElseGet(() -> new TokenBucket(capacity));

		// 4. Check if request is allowed
		boolean allowed = bucket.allowRequest(requestSizeBytes);

		// 5. Save bucket state
		bucketStore.saveBucket(ipAddress, endpoint, bucket);

		// 6. Return result
		return new RequestResult(
			allowed,
			false,
			bucket.getUsedBytes(),
			bucket.getUsagePercentage(),
			allowed ? 0 : bucket.getWaitTime(requestSizeBytes)
		);
	}
}
