package github.fekom.bond.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.ApplicationScope;

import github.fekom.bond.domain.entities.Client.ClientQuery;
import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.enums.TierType;
import github.fekom.bond.domain.entities.Client.Client;

@Repository
@ApplicationScope
public class ImpClientRepository implements ClientRepository {

	@Override
	public void save(List<Client> clientsList) {

	}
	public void delete(String id) {

	}
	@Override
	public List<Client> find(ClientQuery query) {
		return List.of();
	}
	@Override
	public Optional<Client> findById(String id) {
		return findById(id);
	}
	@Override
	public List<Client> findByTier(TierType tier) {
		return List.of();
	}
	@Override
	public Set<Client> findUserTierById(String id) {
		return Set.of();
	}
}

