package github.fekom.bond.config;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.api.BucketStore;
import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.domain.Capacity;
import github.fekom.bond.infrastructure.InMemoryBucketStore;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(TokenBucket.class)
@EnableConfigurationProperties(BondProperties.class)
public class BondAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(BucketStore.class)
	public InMemoryBucketStore inMemoryBucketStore() {
		return new InMemoryBucketStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public RateLimiterService rateLimiterService(BucketStore bucketStore, BondProperties properties) {
		Capacity defaultCapacity = new Capacity(
			properties.getCapacityBytes(),
			properties.getRefillRateBytesPerSecond(),
			properties.getBurstMultiplier()
		);

		Map<String, Capacity> endpointCapacities = new HashMap<>();
		properties.getEndpoints().forEach((endpoint, ec) ->
			endpointCapacities.put(endpoint, new Capacity(
				ec.getCapacityBytes(),
				ec.getRefillRateBytesPerSecond(),
				ec.getBurstMultiplier()
			))
		);

		return new RateLimiterService(bucketStore, defaultCapacity, endpointCapacities);
	}
}
