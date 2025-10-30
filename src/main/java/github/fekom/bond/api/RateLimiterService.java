package github.fekom.bond.api;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import github.fekom.bond.api.dto.out.Client.RateLimiterResponse;
import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiter;
import github.fekom.bond.infrastructure.repository.RateLimiterRepository;

@Service
public class RateLimiterService {
	private final ClientRepository clientRepository;
	private final RateLimiterRepository rateLimiterRepository;

	public RateLimiterService(ClientRepository clientRepository, RateLimiterRepository rateLimiterRepository) {
		this.clientRepository = clientRepository;
		this.rateLimiterRepository = rateLimiterRepository;
	}

	public RateLimiterResponse checkRateLimit(String clientId, long requestSizeBytes) {
		var client = clientRepository.findById(clientId)
				.orElseThrow(() -> new IllegalArgumentException("Client not found"));

		github.fekom.bond.infrastructure.persistence.RateLimiter entity = rateLimiterRepository.findById(clientId)
		.orElseThrow(() -> new IllegalArgumentException("RateLimiter not found"));

		var limiter = RateLimiter.create(clientId, "/api", client.tier());
		boolean allowed = limiter.bucket().allowRequest(requestSizeBytes);
		entity.setUpdateAt(LocalDateTime.now().toString());
		rateLimiterRepository.save(entity);

		return new RateLimiterResponse(allowed, limiter.bucket().getUsedBytes(),
				limiter.bucket().getUsagePercentage(), allowed ? 0 : limiter.bucket().getWaitTime(requestSizeBytes));
	}
}
