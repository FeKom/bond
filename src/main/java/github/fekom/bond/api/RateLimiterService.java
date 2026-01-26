package github.fekom.bond.api;

import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiter;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiterResult;
import github.fekom.bond.infrastructure.repository.RateLimiterRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimiterService {

	private final ClientRepository clientRepository;
	private final RateLimiterRepository rateLimiterRepository;

	public RateLimiterService(ClientRepository clientRepository, RateLimiterRepository rateLimiterRepository) {
		this.clientRepository = clientRepository;
		this.rateLimiterRepository = rateLimiterRepository;
	}

	@Transactional
	public RateLimiterResult checkRateLimit(String clientId, long requestSizeBytes) {
		var client = clientRepository
			.findById(clientId)
			.orElseThrow(() -> new IllegalArgumentException("Client not found"));

		String endpoint = "/api";

		// Busca ou cria o rate limiter para este cliente/endpoint
		Optional<github.fekom.bond.infrastructure.persistence.RateLimiter> existingEntity =
			rateLimiterRepository.findByClientIdAndEndPoint(clientId, endpoint);

		github.fekom.bond.infrastructure.persistence.RateLimiter entity;
		RateLimiter limiter;

		if (existingEntity.isPresent()) {
			// Usa o rate limiter existente com o bucket persistido
			entity = existingEntity.get();
			limiter = entity.toDomain();
		} else {
			// Cria novo rate limiter para o cliente
			limiter = RateLimiter.create(clientId, endpoint, client.tier());
			entity = github.fekom.bond.infrastructure.persistence.RateLimiter.fromDomain(limiter);
		}

		// Verifica se a request é permitida
		boolean allowed = limiter.bucket().allowRequest(requestSizeBytes);

		// Atualiza o estado do bucket na entity e salva
		entity.setBucket(limiter.bucket());
		entity.setupdatedAt(LocalDateTime.now());
		rateLimiterRepository.save(entity);

		return new RateLimiterResult(
			allowed,
			limiter.bucket().getUsedBytes(),
			limiter.bucket().getUsagePercentage(),
			allowed ? 0 : limiter.bucket().getWaitTime(requestSizeBytes)
		);
	}
}
