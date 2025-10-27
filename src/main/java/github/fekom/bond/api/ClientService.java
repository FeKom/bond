package github.fekom.bond.api;

import org.springframework.stereotype.Service;

import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.enums.TierType;
import github.fekom.bond.domain.entities.Client.Client;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {
	private final ClientRepository repository;

	public ClientService(ClientRepository repository) {
		this.repository = repository;
	}
 // Criar novo cliente
    public Client create(TierType tier) {
        Client client = Client.create(tier);
        repository.save(client);
        return client;
    }

    // Buscar por ID
    public Optional<Client> getById(String id) {
        return repository.findById(id);
    }

    // Listar por tier
    public List<Client> getByTier(TierType tier) {
        return repository.findByTier(tier);
    }

}
