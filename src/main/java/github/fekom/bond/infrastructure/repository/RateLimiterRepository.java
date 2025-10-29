package github.fekom.bond.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import github.fekom.bond.infrastructure.persistence.RateLimiter;

public interface RateLimiterRepository extends JpaRepository<RateLimiter, String> {
	Optional<RateLimiter> findByClientIdAndEndpoint(String clientId, String endpoint);
	List<RateLimiter> findByClientId(String clientId);


}
