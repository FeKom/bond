package github.fekom.bond.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Bond rate limiter.
 * <p>
 * Example {@code application.yml}:
 * <pre>
 * bond:
 *   capacity-bytes: 10485760          # 10MB global default
 *   refill-rate-bytes-per-second: 2912
 *   burst-multiplier: 2.0
 *   filter-enabled: true              # auto rate-limit HTTP requests
 *   endpoints:
 *     /api/upload:
 *       capacity-bytes: 52428800      # 50MB for uploads
 *       refill-rate-bytes-per-second: 14563
 *       burst-multiplier: 1.5
 * </pre>
 */
@ConfigurationProperties(prefix = "bond")
public class BondProperties {

	private long capacityBytes = 32_768;
	private long refillRateBytesPerSecond = 9;
	private double burstMultiplier = 1.5;
	private Map<String, EndpointCapacity> endpoints = new HashMap<>();
	private boolean filterEnabled = true;

	public long getCapacityBytes() {
		return capacityBytes;
	}

	public void setCapacityBytes(long capacityBytes) {
		this.capacityBytes = capacityBytes;
	}

	public long getRefillRateBytesPerSecond() {
		return refillRateBytesPerSecond;
	}

	public void setRefillRateBytesPerSecond(long refillRateBytesPerSecond) {
		this.refillRateBytesPerSecond = refillRateBytesPerSecond;
	}

	public double getBurstMultiplier() {
		return burstMultiplier;
	}

	public void setBurstMultiplier(double burstMultiplier) {
		this.burstMultiplier = burstMultiplier;
	}

	public Map<String, EndpointCapacity> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(Map<String, EndpointCapacity> endpoints) {
		this.endpoints = endpoints;
	}

	public boolean isFilterEnabled() {
		return filterEnabled;
	}

	public void setFilterEnabled(boolean filterEnabled) {
		this.filterEnabled = filterEnabled;
	}

	public static class EndpointCapacity {
		private long capacityBytes;
		private long refillRateBytesPerSecond;
		private double burstMultiplier = 1.5;

		public long getCapacityBytes() {
			return capacityBytes;
		}

		public void setCapacityBytes(long capacityBytes) {
			this.capacityBytes = capacityBytes;
		}

		public long getRefillRateBytesPerSecond() {
			return refillRateBytesPerSecond;
		}

		public void setRefillRateBytesPerSecond(long refillRateBytesPerSecond) {
			this.refillRateBytesPerSecond = refillRateBytesPerSecond;
		}

		public double getBurstMultiplier() {
			return burstMultiplier;
		}

		public void setBurstMultiplier(double burstMultiplier) {
			this.burstMultiplier = burstMultiplier;
		}
	}
}
