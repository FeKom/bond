package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.infrastructure.persistence.RateLimiter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimiterRepository extends JpaRepository<RateLimiter, String> {
	Optional<RateLimiter> findByClientIdAndEndPoint(String clientId, String endPoint);
	List<RateLimiter> findByClientId(String clientId);
}
