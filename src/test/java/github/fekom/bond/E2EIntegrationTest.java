package github.fekom.bond;

import static org.junit.jupiter.api.Assertions.*;

import github.fekom.bond.config.TestSecurityConfig;
import github.fekom.bond.domain.enums.TierType;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes End-to-End com banco de dados real (Testcontainers)
 * Estes testes desabilitam a segurança para focar na lógica de negócio
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Integration Tests")
class E2EIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
		.withDatabaseName("bond_test")
		.withUsername("test")
		.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		// Desabilita segurança para testes E2E focados em lógica de negócio
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
		registry.add("spring.autoconfigure.exclude", () ->
			"org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
		);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private String baseUrl;
	private static String createdClientId;

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port;
	}

	@Test
	@Order(1)
	@DisplayName("Health check deve estar disponível")
	void healthCheckShouldBeAvailable() {
		ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().contains("UP"));
	}

	@Test
	@Order(2)
	@DisplayName("Deve criar cliente FREE com sucesso")
	void shouldCreateFreeClientSuccessfully() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<>("{\"tier\": \"FREE\"}", headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/clients", request, Map.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().get("id").toString().startsWith("API_KEY_"));
		assertEquals("FREE", response.getBody().get("tier"));
		assertTrue((Boolean) response.getBody().get("enabled"));

		// Salva para próximos testes
		createdClientId = response.getBody().get("id").toString();
	}

	@Test
	@Order(3)
	@DisplayName("Deve buscar cliente criado por ID")
	void shouldFindClientById() {
		assertNotNull(createdClientId, "Cliente deve ter sido criado no teste anterior");

		ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/clients/" + createdClientId, Map.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(createdClientId, response.getBody().get("id"));
		assertEquals("FREE", response.getBody().get("tier"));
	}

	@Test
	@Order(4)
	@DisplayName("Deve retornar 404 para cliente inexistente")
	void shouldReturn404ForNonExistentClient() {
		ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/clients/API_KEY_NOT_EXISTS", String.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	@Order(5)
	@DisplayName("Rate limiter deve permitir primeira request")
	void rateLimiterShouldAllowFirstRequest() {
		assertNotNull(createdClientId, "Cliente deve ter sido criado");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-API-Key", createdClientId);

		HttpEntity<String> request = new HttpEntity<>("{\"data\": \"hello world\"}", headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/check", request, Map.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue((Boolean) response.getBody().get("allowed"));
		assertTrue((Double) response.getBody().get("usagePercentage") > 0);
		assertEquals(0, ((Number) response.getBody().get("waitTimeMs")).intValue());
	}

	@Test
	@Order(6)
	@DisplayName("Rate limiter deve acumular uso em múltiplas requests")
	void rateLimiterShouldAccumulateUsage() {
		assertNotNull(createdClientId, "Cliente deve ter sido criado");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-API-Key", createdClientId);

		double previousUsage = 0;

		for (int i = 0; i < 5; i++) {
			HttpEntity<String> request = new HttpEntity<>(
				"{\"data\": \"request number " + i + " with some content\"}",
				headers
			);

			ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/check", request, Map.class);

			assertEquals(HttpStatus.OK, response.getStatusCode());

			double currentUsage = (Double) response.getBody().get("usagePercentage");
			assertTrue(
				currentUsage > previousUsage,
				"Uso deve aumentar a cada request: " + currentUsage + " > " + previousUsage
			);
			previousUsage = currentUsage;
		}
	}

	@Test
	@Order(7)
	@DisplayName("Deve criar cliente STARTUP com maior capacidade")
	void shouldCreateStartupClientWithHigherCapacity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> request = new HttpEntity<>("{\"tier\": \"STARTUP\"}", headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/clients", request, Map.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("STARTUP", response.getBody().get("tier"));

		String startupClientId = response.getBody().get("id").toString();

		// Verifica que rate limiter aceita payload maior
		headers.set("X-API-Key", startupClientId);

		// Payload de 1MB (só funciona com STARTUP ou ENTERPRISE)
		String largePayload = "{\"data\": \"" + "x".repeat(100000) + "\"}";
		HttpEntity<String> checkRequest = new HttpEntity<>(largePayload, headers);

		ResponseEntity<Map> checkResponse = restTemplate.postForEntity(baseUrl + "/check", checkRequest, Map.class);

		assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
		assertTrue((Boolean) checkResponse.getBody().get("allowed"));
	}

	@Test
	@Order(8)
	@DisplayName("Deve retornar tier do cliente")
	void shouldReturnClientTier() {
		assertNotNull(createdClientId, "Cliente deve ter sido criado");

		ResponseEntity<String> response = restTemplate.getForEntity(
			baseUrl + "/clients/tier/" + createdClientId,
			String.class
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().contains("FREE"));
	}

	@Test
	@Order(9)
	@DisplayName("Rate limiter deve rejeitar quando API Key não existe")
	void rateLimiterShouldRejectInvalidApiKey() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-API-Key", "API_KEY_INVALID");

		HttpEntity<String> request = new HttpEntity<>("{\"data\": \"test\"}", headers);

		ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/check", request, String.class);

		// Deve retornar erro (400 ou 500 dependendo da implementação)
		assertTrue(
			response.getStatusCode() == HttpStatus.BAD_REQUEST || response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
		);
	}
}
