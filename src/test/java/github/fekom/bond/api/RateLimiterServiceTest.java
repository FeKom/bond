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
		@DisplayName("deve permitir request quando há capacidade (novo bucket)")
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
		@DisplayName("deve permitir request com bucket existente que tem capacidade")
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
		@DisplayName("deve rejeitar request quando não há capacidade")
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
		@DisplayName("deve usar default capacity quando endpoint não tem config específica")
		void shouldUseDefaultCapacityWhenNoEndpointConfig() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.empty());

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertTrue(result.allowed());
			assertEquals(100, result.usedBytes());
		}

		@Test
		@DisplayName("deve usar capacity do endpoint quando configurado")
		void shouldUseEndpointCapacityWhenConfigured() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 5MB - maior que default (32KB) mas menor que UPLOAD (10MB)
			RequestResult result = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 5_000_000);

			assertTrue(result.allowed());
		}

		@Test
		@DisplayName("deve rejeitar no default mas permitir no endpoint com capacity maior")
		void shouldRejectOnDefaultButAllowOnEndpointWithHigherCapacity() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, ENDPOINT)).thenReturn(Optional.empty());
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 50KB - maior que default (32KB)
			RequestResult defaultResult = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 50_000);
			assertFalse(defaultResult.allowed());

			// Mesmo tamanho no endpoint upload (10MB) deve passar
			RequestResult uploadResult = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 50_000);
			assertTrue(uploadResult.allowed());
		}

		@Test
		@DisplayName("deve usar IP override sobre capacity do endpoint")
		void shouldUseIpOverrideOverEndpointCapacity() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(false);
			when(bucketStore.findCapacityByIp(IP_ADDRESS)).thenReturn(Optional.of(IP_OVERRIDE_CAPACITY));
			when(bucketStore.findBucket(IP_ADDRESS, UPLOAD_ENDPOINT)).thenReturn(Optional.empty());

			// 50MB - maior que UPLOAD (10MB) mas menor que IP_OVERRIDE (1GB)
			RequestResult result = service.checkRateLimit(IP_ADDRESS, UPLOAD_ENDPOINT, 50_000_000);

			assertTrue(result.allowed());
		}

		@Test
		@DisplayName("deve rejeitar imediatamente quando IP está bloqueado")
		void shouldRejectImmediatelyWhenIpIsBlocked() {
			when(bucketStore.isBlocked(IP_ADDRESS)).thenReturn(true);

			RequestResult result = service.checkRateLimit(IP_ADDRESS, ENDPOINT, 100);

			assertFalse(result.allowed());
			assertTrue(result.blocked());
			assertEquals(0, result.usedBytes());
			// Não deve acessar bucket nem capacity quando bloqueado
			verify(bucketStore, never()).findBucket(any(), any());
			verify(bucketStore, never()).findCapacityByIp(any());
			verify(bucketStore, never()).saveBucket(any(), any(), any());
		}
	}
}
