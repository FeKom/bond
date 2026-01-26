package github.fekom.bond.infrastructure.repository;

import github.fekom.bond.domain.entities.Client.Client;
import github.fekom.bond.domain.entities.Client.ClientQuery;
import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.enums.TierType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ImpClientRepository implements ClientRepository {

	private final EntityManager entityManager;

	public ImpClientRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	@Transactional
	public void save(List<Client> clientsList) {
		clientsList
			.stream()
			.map(github.fekom.bond.infrastructure.persistence.Client::fromDomain)
			.forEach(entityManager::merge);
	}

	public void delete(String id) {}

	@Override
	public List<Client> find(ClientQuery query) {
		var cb = entityManager.getCriteriaBuilder();
		var cq = cb.createQuery(github.fekom.bond.infrastructure.persistence.Client.class);
		// from client
		var root = cq.from(github.fekom.bond.infrastructure.persistence.Client.class);
		Predicate[] filtros = conditions(query, cb, root);
		if (filtros.length > 0) {
			cq.where(filtros); // Aplica as condições combinadas com AND
		}

		// 3. Executa a consulta, mapeia para o Domínio e retorna
		return entityManager
			.createQuery(cq)
			// Garante que o tipo retornado é Client
			.getResultStream()
			.map(github.fekom.bond.infrastructure.persistence.Client::toDomain) // Se você usa mapeamento
			.toList();
	}

	@Override
	public Optional<Client> findById(String id) {
		return find(ClientQuery.builder().ids(Set.of(id)).build()).stream().findFirst();
	}

	@Override
	public List<Client> findByTier(TierType tier) {
		return find(ClientQuery.builder().tiers(Set.of(tier)).build());
	}

	@Override
	public Optional<TierType> findClientTierById(String id) {
		var cb = entityManager.getCriteriaBuilder();
		var cq = cb.createQuery(TierType.class);
		// from client
		var root = cq.from(github.fekom.bond.infrastructure.persistence.Client.class);
		cq.select(root.get("tier"));
		//isso aq aplica o filtro where
		Predicate[] filters = conditions(ClientQuery.builder().ids(Set.of(id)).build(), cb, root);
		if (filters.length > 0) {
			cq.where(filters); // Aplica as condições combinadas com AND
		}
		return entityManager
			.createQuery(cq)
			// Garante que o tipo retornado é Client
			.getResultStream()
			.findFirst();
	}

	private Predicate[] conditions(
		ClientQuery query,
		CriteriaBuilder cb,
		Root<github.fekom.bond.infrastructure.persistence.Client> root
	) {
		var predicates = new java.util.ArrayList<Predicate>();

		// 1. FILTRO POR TIERS (Lista de ENUMs)
		// Se a lista de tiers for fornecida, construímos um predicado IN.
		query
			.tiers()
			.ifPresent(tiers -> {
				// 1.1. Inicia o predicado IN para o campo "tier" da entidade.
				CriteriaBuilder.In<Object> inClause = cb.in(root.get("tier"));

				// 1.2. Adiciona todos os valores do ENUM à cláusula IN.
				tiers.forEach(inClause::value);

				// 1.3. Adiciona a condição IN à lista de predicados.
				predicates.add(inClause);
			});

		// 2. FILTRO POR IDs (Lista de IDs)
		// Se IDs forem fornecidos, construímos um predicado IN.
		query
			.ids()
			.ifPresent(ids -> {
				CriteriaBuilder.In<Object> inClause = cb.in(root.get("id"));
				ids.forEach(inClause::value);
				predicates.add(inClause);
			});

		// Retorna um array. O JPA irá uni-los com AND: (tier IN (X, Y)) AND (id = Z)
		return predicates.toArray(new Predicate[0]);
	}
}
