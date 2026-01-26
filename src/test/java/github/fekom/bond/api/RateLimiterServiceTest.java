package github.fekom.bond.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.entities.Client.Client;
import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiterResult;
import github.fekom.bond.domain.enums.TierType;
import github.fekom.bond.infrastructure.persistence.RateLimiter;
import github.fekom.bond.infrastructure.repository.RateLimiterRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private RateLimiterRepository rateLimiterRepository;

	@InjectMocks
	private RateLimiterService service;

	private static final String CLIENT_ID = "API_KEY_123";
	private static final String ENDPOINT = "/api";

	@Nested
	@DisplayName("checkRateLimit")
	class CheckRateLimit {

		@Test
		@DisplayName("deve permitir request quando cliente tem capacidade (novo rate limiter)")
		void shouldAllowRequestWhenClientHasCapacity() {
			// Given
			Client client = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.FREE);
			when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
			when(rateLimiterRepository.findByClientIdAndEndPoint(CLIENT_ID, ENDPOINT)).thenReturn(Optional.empty()); // Novo rate limiter

			// When
			RateLimiterResult result = service.checkRateLimit(CLIENT_ID, 100);

			// Then
			assertTrue(result.allowed());
			assertEquals(100, result.usedBytes());
			assertTrue(result.usagePercentage() > 0);
			assertEquals(0, result.waitTimeMs());

			// Verifica se salvou o rate limiter
			verify(rateLimiterRepository).save(any(RateLimiter.class));
		}

		@Test
		@DisplayName("deve permitir request quando rate limiter existente tem capacidade")
		void shouldAllowRequestWhenExistingRateLimiterHasCapacity() {
			// Given
			Client client = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.FREE);
			when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

			// Rate limiter existente com bucket quase cheio
			RateLimiter existingLimiter = createRateLimiterEntity(CLIENT_ID, TierType.FREE, 30_000);
			when(rateLimiterRepository.findByClientIdAndEndPoint(CLIENT_ID, ENDPOINT)).thenReturn(
				Optional.of(existingLimiter)
			);

			// When
			RateLimiterResult result = service.checkRateLimit(CLIENT_ID, 100);

			// Then
			assertTrue(result.allowed());
			verify(rateLimiterRepository).save(any(RateLimiter.class));
		}

		@Test
		@DisplayName("deve rejeitar request quando cliente não tem capacidade")
		void shouldRejectRequestWhenClientHasNoCapacity() {
			// Given
			Client client = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.FREE);
			when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

			// Rate limiter com bucket quase vazio (apenas 50 bytes restantes)
			RateLimiter existingLimiter = createRateLimiterEntity(CLIENT_ID, TierType.FREE, 50);
			when(rateLimiterRepository.findByClientIdAndEndPoint(CLIENT_ID, ENDPOINT)).thenReturn(
				Optional.of(existingLimiter)
			);

			// When
			RateLimiterResult result = service.checkRateLimit(CLIENT_ID, 1000);

			// Then
			assertFalse(result.allowed());
			assertTrue(result.waitTimeMs() > 0);
		}

		@Test
		@DisplayName("deve lançar exceção quando cliente não existe")
		void shouldThrowExceptionWhenClientNotFound() {
			// Given
			when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

			// When/Then
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
				service.checkRateLimit(CLIENT_ID, 100)
			);

			assertEquals("Client not found", exception.getMessage());
			verify(rateLimiterRepository, never()).save(any());
		}

		@Test
		@DisplayName("deve usar tier correto do cliente para criar rate limiter")
		void shouldUseCorrectClientTierForRateLimiter() {
			// Given - Cliente ENTERPRISE
			Client client = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.ENTERPRISE);
			when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
			when(rateLimiterRepository.findByClientIdAndEndPoint(CLIENT_ID, ENDPOINT)).thenReturn(Optional.empty());

			// When - Request grande que só ENTERPRISE suporta
			RateLimiterResult result = service.checkRateLimit(CLIENT_ID, 50_000_000); // 50MB

			// Then - Deve permitir (ENTERPRISE tem 1GB)
			assertTrue(result.allowed());
		}
	}

	// Helper para criar RateLimiter entity com bucket específico
	private RateLimiter createRateLimiterEntity(String clientId, TierType tier, long currentBytes) {
		RateLimiter entity = new RateLimiter();
		entity.setId("rate-limiter-123");
		entity.setClientId(clientId);
		entity.setEndPoint(ENDPOINT);
		entity.setCreatedAt(LocalDateTime.now());
		entity.setupdatedAt(LocalDateTime.now());

		// Cria bucket com bytes específicos
		TokenBucket bucket = new TokenBucket(tier);
		// Consome bytes para deixar apenas currentBytes restantes
		long toConsume = bucket.getBucketCapacityBytes() - currentBytes;
		if (toConsume > 0) {
			bucket.allowRequest(toConsume);
		}
		entity.setBucket(bucket);

		return entity;
	}
}
