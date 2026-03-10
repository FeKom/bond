package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.infrastructure.persistence.Request;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, String> {
	Optional<Request> findByIpAddressAndEndpoint(String ipAddress, String endpoint);
}
