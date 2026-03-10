package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.infrastructure.persistence.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<Client, String> {
	Optional<Client> findByIpAddress(String ipAddress);
}
