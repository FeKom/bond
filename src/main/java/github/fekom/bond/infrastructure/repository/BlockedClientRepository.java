package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.infrastructure.persistence.BlockedClient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedClientRepository extends JpaRepository<BlockedClient, String> {
	Optional<BlockedClient> findByIpAddress(String ipAddress);
	boolean existsByIpAddress(String ipAddress);
	void deleteByIpAddress(String ipAddress);
}
