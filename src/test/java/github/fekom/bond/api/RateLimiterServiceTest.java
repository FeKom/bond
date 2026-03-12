package github.fekom.bond.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.Capacity;
import github.fekom.bond.domain.RequestResult;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

	private static final Capacity DEFAULT_CAPACITY = new Capacity(
		32_768, 32_768 / 3600, 1.5
	);

	private static final Capacity UPLOAD_CAPACITY = new Capacity(
		10 * 1024 * 1024, (10 * 1024 * 1024) / 3600, 2.0
	);

	private static final Capacity IP_OVERRIDE_CAPACITY = new Capacity(
		1L * 1024 * 1024 * 1024, (1L * 1024 * 1024 * 1024) / 3600, 3.0
	);

	private static final String IP_ADDRESS = "192.168.1.100";
	private static final String ENDPOINT = "/api/data";
	private static final String UPLOAD_ENDPOINT = "/api/upload";

	@Mock
	private BucketStore bucketStore;

	private RateLimiterService service;

	@BeforeEach
	void setUp() {
		Map<String, Capacity> endpointCapacities = Map.of(UPLOAD_ENDPOINT, UPLOAD_CAPACITY);
		service = new RateLimiterService(bucketStore, DEFAULT_CAPACITY, endpointCapacities);
	}

	@Nested
	@DisplayName("checkRateLimit")
	class CheckRateLimit {

		@Test
		@DisplayName("should allow request when there is capacity (new bucket)")
		void shouldAllowRequestWhenNewBucket() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.empty());

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertTrue(result.allowed());
			assertFalse(result.blocked());
			assertEquals(100, result.usedBytes());
			assertTrue(result.usagePercentage() > 0);
			assertEquals(0, result.waitTimeMs());
			verify(bucketStore).saveBucket(eq(IP_ADDRESS), eq(ENDPOINT), any(TokenBucket.class));
		}

		@Test
		@DisplayName("should allow request when existing bucket has capacity")
		void shouldAllowRequestWhenExistingBucketHasCapacity() {
			TokenBucket existingBucket = new TokenBucket(DEFAULT_CAPACITY);
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.of(existingBucket));

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertTrue(result.allowed());
			assertFalse(result.blocked());
			verify(bucketStore).saveBucket(eq(IP_ADDRESS), eq(ENDPOINT), any(TokenBucket.class));
		}

		@Test
		@DisplayName("should reject request when there is no capacity")
		void shouldRejectRequestWhenNoCapacity() {
			TokenBucket almostEmptyBucket = new TokenBucket(DEFAULT_CAPACITY);
			almostEmptyBucket.allowRequest(32_718);
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.of(almostEmptyBucket));

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 1000);

			assertFalse(result.allowed());
			assertFalse(result.blocked());
			assertTrue(result.waitTimeMs() > 0);
		}

		@Test
		@DisplayName("should use default capacity when endpoint has no specific config")
		void shouldUseDefaultCapacityWhenNoEndpointConfig() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.empty());

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertTrue(result.allowed());
			assertEquals(100, result.usedBytes());
		}

		@Test
		@DisplayName("should use endpoint capacity when configured")
		void shouldUseEndpointCapacityWhenConfigured() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 5MB - larger than default (32KB) but smaller than UPLOAD (10MB)
			RequestResult result = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 5_000_000);

			assertTrue(result.allowed());
		}

		@Test
		@DisplayName("should reject on default but allow on endpoint with higher capacity")
		void shouldRejectOnDefaultButAllowOnEndpointWithHigherCapacity() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 50KB - larger than default (32KB)
			RequestResult defaultResult = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 50_000);
			assertFalse(defaultResult.allowed());

			// Same size on upload endpoint (10MB) should pass
			RequestResult uploadResult = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 50_000);
			assertTrue(uploadResult.allowed());
		}

		@Test
		@DisplayName("should use IP override over endpoint capacity")
		void shouldUseIpOverrideOverEndpointCapacity() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.of(IP_OVERRIDE_CAPACITY));
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 50MB - larger than UPLOAD (10MB) but smaller than IP_OVERRIDE (1GB)
			RequestResult result = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 50_000_000);

			assertTrue(result.allowed());
		}

		@Test
		@DisplayName("should reject immediately when IP is blocked")
		void shouldRejectImmediatelyWhenIpIsBlocked() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(true);

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertFalse(result.allowed());
			assertTrue(result.blocked());
			assertEquals(0, result.usedBytes());
			// Should not access bucket or capacity when blocked
			verify(bucketStore, never()).findBucket(any(), any());
			verify(bucketStore, never()).findCapacityByIp(any());
			verify(bucketStore, never()).saveBucket(any(), any(), any());
		}
	}
}
