package github.fekom.bond.domain.entities.Client;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;

import github.fekom.bond.domain.enums.TierType;

 public interface ClientRepository {
	// boolean existsByClientId(String clientId);
	List<Client> find(ClientQuery query);

	default List<Client> findOne(ClientQuery query) {
		var result = ClientQuery.Builder
				.from(query)
				.limit(1)
				.build();
		return find(result);
	}

	default Optional<Client> findFirst(ClientQuery query) {
		var clients = findOne(query);
		return clients.stream().findFirst();
	}

	default List<Client> findAll() {
		return find(new ClientQuery.Builder().build());
	}

	default Optional<Client> findById(String id) {
		return findFirst(ClientQuery.builder()
				.ids(Set.of(id))
				.build());
	}

	List<Client> findByTier(TierType tier);

	Set<Client> findUserTierById(String id);

	void save(List<Client> clientsList);

	@Query("INSERT INTO clients (id, tier, enabled, created_at, updated_at) VALUES (:id, :tier, :enabled, :createdAt, :updatedAt)	")
	default void save(Client client) {
		save(List.of(client));
	};

	void delete(String id);

}
