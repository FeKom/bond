package github.fekom.bond.infrastructure.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiterResult;
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

@WebMvcTest(RateLimiterController.class)
@Import(github.fekom.bond.config.SecurityConfig.class)
@DisplayName("RateLimiterController")
class RateLimiterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RateLimiterService rateLimiterService;

	@MockBean
	private JwtDecoder jwtDecoder;

	private static final String CLIENT_ID = "API_KEY_123";

	@Nested
	@DisplayName("POST /check")
	class CheckRateLimit {

		@Test
		@DisplayName("deve retornar 401 quando não autenticado")
		void shouldReturn401WhenNotAuthenticated() throws Exception {
			mockMvc
				.perform(
					post("/check")
						.header("X-API-Key", CLIENT_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"data\": \"test\"}")
				)
				.andExpect(status().isUnauthorized());
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar 200 quando request permitida")
		void shouldReturn200WhenRequestAllowed() throws Exception {
			// Given
			RateLimiterResult result = new RateLimiterResult(true, 100, 0.5, 0);
			when(rateLimiterService.checkRateLimit(eq(CLIENT_ID), anyLong())).thenReturn(result);

			// When/Then
			mockMvc
				.perform(
					post("/check")
						.with(csrf())
						.header("X-API-Key", CLIENT_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"data\": \"test\"}")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allowed").value(true))
				.andExpect(jsonPath("$.usagePercentage").value(0.5))
				.andExpect(jsonPath("$.waitTimeMs").value(0))
				.andExpect(header().exists("X-RateLimit-Used"))
				.andExpect(header().exists("X-RateLimit-Usage"));
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar 429 quando rate limit excedido")
		void shouldReturn429WhenRateLimitExceeded() throws Exception {
			// Given
			RateLimiterResult result = new RateLimiterResult(false, 32000, 99.5, 5000);
			when(rateLimiterService.checkRateLimit(eq(CLIENT_ID), anyLong())).thenReturn(result);

			// When/Then
			mockMvc
				.perform(
					post("/check")
						.with(csrf())
						.header("X-API-Key", CLIENT_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"data\": \"test\"}")
				)
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.allowed").value(false))
				.andExpect(jsonPath("$.waitTimeMs").value(5000))
				.andExpect(header().string("X-RateLimit-Reset-After", "5000"));
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar 400 quando X-API-Key ausente")
		void shouldReturn400WhenApiKeyMissing() throws Exception {
			mockMvc
				.perform(post("/check").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"data\": \"test\"}"))
				.andExpect(status().isBadRequest());
		}

		@Test
		@WithMockUser
		@DisplayName("deve processar payload grande corretamente")
		void shouldProcessLargePayloadCorrectly() throws Exception {
			// Given
			String largePayload = "{\"data\": \"" + "x".repeat(10000) + "\"}";
			RateLimiterResult result = new RateLimiterResult(true, 500, 1.5, 0);
			when(rateLimiterService.checkRateLimit(eq(CLIENT_ID), anyLong())).thenReturn(result);

			// When/Then
			mockMvc
				.perform(
					post("/check")
						.with(csrf())
						.header("X-API-Key", CLIENT_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(largePayload)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allowed").value(true));
		}

		@Test
		@WithMockUser
		@DisplayName("deve retornar erro quando cliente não encontrado")
		void shouldReturnErrorWhenClientNotFound() throws Exception {
			// Given
			when(rateLimiterService.checkRateLimit(eq(CLIENT_ID), anyLong())).thenThrow(
				new IllegalArgumentException("Client not found")
			);

			// When/Then
			mockMvc
				.perform(
					post("/check")
						.with(csrf())
						.header("X-API-Key", CLIENT_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"data\": \"test\"}")
				)
				.andExpect(status().isBadRequest());
		}
	}
}
