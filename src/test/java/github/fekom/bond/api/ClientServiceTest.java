package github.fekom.bond.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import github.fekom.bond.domain.entities.Client.Client;
import github.fekom.bond.domain.entities.Client.ClientRepository;
import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService")
class ClientServiceTest {

	@Mock
	private ClientRepository repository;

	@InjectMocks
	private ClientService service;

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		@DisplayName("deve criar cliente com tier FREE")
		void shouldCreateClientWithFreeTier() {
			// When
			Client result = service.create(TierType.FREE);

			// Then
			assertNotNull(result);
			assertEquals(TierType.FREE, result.tier());
			assertTrue(result.enabled());
			assertTrue(result.id().startsWith("API_KEY_"));

			// Verifica se salvou no repositório
			ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
			verify(repository).save(captor.capture());
			assertEquals(TierType.FREE, captor.getValue().tier());
		}

		@Test
		@DisplayName("deve criar cliente com tier STARTUP")
		void shouldCreateClientWithStartupTier() {
			Client result = service.create(TierType.STARTUP);

			assertNotNull(result);
			assertEquals(TierType.STARTUP, result.tier());
			verify(repository).save(any(Client.class));
		}

		@Test
		@DisplayName("deve criar cliente com tier ENTERPRISE")
		void shouldCreateClientWithEnterpriseTier() {
			Client result = service.create(TierType.ENTERPRISE);

			assertNotNull(result);
			assertEquals(TierType.ENTERPRISE, result.tier());
			verify(repository).save(any(Client.class));
		}

		@Test
		@DisplayName("deve gerar IDs únicos para cada cliente")
		void shouldGenerateUniqueIds() {
			Client client1 = service.create(TierType.FREE);
			Client client2 = service.create(TierType.FREE);

			assertNotEquals(client1.id(), client2.id());
		}
	}

	@Nested
	@DisplayName("getById")
	class GetById {

		@Test
		@DisplayName("deve retornar cliente quando existe")
		void shouldReturnClientWhenExists() {
			// Given
			String clientId = "API_KEY_123";
			LocalDateTime now = LocalDateTime.now();
			Client mockClient = new Client(clientId, true, now, now, TierType.FREE);
			when(repository.findById(clientId)).thenReturn(Optional.of(mockClient));

			// When
			Optional<Client> result = service.getById(clientId);

			// Then
			assertTrue(result.isPresent());
			assertEquals(clientId, result.get().id());
			assertEquals(TierType.FREE, result.get().tier());
		}

		@Test
		@DisplayName("deve retornar empty quando cliente não existe")
		void shouldReturnEmptyWhenClientNotExists() {
			// Given
			String clientId = "API_KEY_NOT_FOUND";
			when(repository.findById(clientId)).thenReturn(Optional.empty());

			// When
			Optional<Client> result = service.getById(clientId);

			// Then
			assertTrue(result.isEmpty());
		}
	}

	@Nested
	@DisplayName("getTierById")
	class GetTierById {

		@Test
		@DisplayName("deve retornar tier do cliente quando existe")
		void shouldReturnTierWhenClientExists() {
			// Given
			String clientId = "API_KEY_123";
			when(repository.findClientTierById(clientId)).thenReturn(Optional.of(TierType.STARTUP));

			// When
			Optional<?> result = service.getTierById(clientId);

			// Then
			assertTrue(result.isPresent());
			assertEquals(TierType.STARTUP, result.get());
		}

		@Test
		@DisplayName("deve retornar empty quando cliente não existe")
		void shouldReturnEmptyWhenClientNotExists() {
			// Given
			String clientId = "API_KEY_NOT_FOUND";
			when(repository.findClientTierById(clientId)).thenReturn(Optional.empty());

			// When
			Optional<?> result = service.getTierById(clientId);

			// Then
			assertTrue(result.isEmpty());
		}
	}
}
