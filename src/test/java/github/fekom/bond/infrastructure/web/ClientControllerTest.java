package github.fekom.bond.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.fekom.bond.api.ClientService;
import github.fekom.bond.api.dto.in.Client.CreateClientRequest;
import github.fekom.bond.domain.entities.Client.Client;
import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientController.class)
@Import(github.fekom.bond.config.SecurityConfig.class)
@DisplayName("ClientController")
class ClientControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ClientService clientService;

	@MockBean
	private JwtDecoder jwtDecoder;

	private static final String CLIENT_ID = "API_KEY_123";

	@Nested
	@DisplayName("POST /clients")
	class CreateClient {

		@Test
		@DisplayName("deve retornar 401 quando não autenticado")
		void shouldReturn401WhenNotAuthenticated() throws Exception {
			mockMvc
				.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content("{\"tier\": \"FREE\"}"))
				.andExpect(status().isUnauthorized());
		}

		@Test
		@WithMockUser(roles = "USER")
		@DisplayName("deve retornar 403 quando usuário não tem role ADMIN")
		void shouldReturn403WhenUserNotAdmin() throws Exception {
			mockMvc
				.perform(post("/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tier\": \"FREE\"}"))
				.andExpect(status().isForbidden());
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		@DisplayName("deve criar cliente quando autenticado como ADMIN")
		void shouldCreateClientWhenAdmin() throws Exception {
			// Given
			Client mockClient = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.FREE);
			when(clientService.create(TierType.FREE)).thenReturn(mockClient);

			// When/Then
			mockMvc
				.perform(post("/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tier\": \"FREE\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(CLIENT_ID))
				.andExpect(jsonPath("$.tier").value("FREE"))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.message").value("Client created successfully"));
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		@DisplayName("deve criar cliente STARTUP quando solicitado")
		void shouldCreateStartupClient() throws Exception {
			// Given
			Client mockClient = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.STARTUP);
			when(clientService.create(TierType.STARTUP)).thenReturn(mockClient);

			// When/Then
			mockMvc
				.perform(
					post("/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tier\": \"STARTUP\"}")
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tier").value("STARTUP"));
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		@DisplayName("deve retornar 400 quando tier inválido")
		void shouldReturn400WhenInvalidTier() throws Exception {
			mockMvc
				.perform(
					post("/clients").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tier\": \"INVALID_TIER\"}")
				)
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /clients/{id}")
	class GetClientById {

		@Test
		@DisplayName("deve retornar 401 quando não autenticado")
		void shouldReturn401WhenNotAuthenticated() throws Exception {
			mockMvc.perform(get("/clients/{id}", CLIENT_ID)).andExpect(status().isUnauthorized());
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar cliente quando existe")
		void shouldReturnClientWhenExists() throws Exception {
			// Given
			Client mockClient = new Client(CLIENT_ID, true, LocalDateTime.now(), LocalDateTime.now(), TierType.FREE);
			when(clientService.getById(CLIENT_ID)).thenReturn(Optional.of(mockClient));

			// When/Then
			mockMvc
				.perform(get("/clients/{id}", CLIENT_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CLIENT_ID))
				.andExpect(jsonPath("$.tier").value("FREE"));
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar 404 quando cliente não existe")
		void shouldReturn404WhenClientNotExists() throws Exception {
			// Given
			when(clientService.getById(CLIENT_ID)).thenReturn(Optional.empty());

			// When/Then
			mockMvc.perform(get("/clients/{id}", CLIENT_ID)).andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("GET /clients/tier/{id}")
	class GetClientTier {

		@Test
		@WithMockUser
		@DisplayName("deve retornar tier quando cliente existe")
		void shouldReturnTierWhenClientExists() throws Exception {
			// Given
			doReturn(Optional.of(TierType.STARTUP)).when(clientService).getTierById(CLIENT_ID);

			// When/Then
			mockMvc
				.perform(get("/clients/tier/{id}", CLIENT_ID))
				.andExpect(status().isOk())
				.andExpect(content().string("\"STARTUP\""));
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar 404 quando cliente não existe")
		void shouldReturn404WhenClientNotExists() throws Exception {
			// Given
			when(clientService.getTierById(CLIENT_ID)).thenReturn(Optional.empty());

			// When/Then
			mockMvc.perform(get("/clients/tier/{id}", CLIENT_ID)).andExpect(status().isNotFound());
		}
	}
}
