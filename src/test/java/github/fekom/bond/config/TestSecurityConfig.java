package github.fekom.bond.config;

import java.time.Instant;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança para testes E2E.
 * Desabilita autenticação para permitir testes de lógica de negócio.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

	@Bean
	@Primary
	public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}

	@Bean
	@Primary
	public JwtDecoder jwtDecoder() {
		// Mock JwtDecoder que não faz nada (usado para satisfazer dependência)
		return token ->
			Jwt.withTokenValue(token)
				.header("alg", "none")
				.claim("sub", "test-user")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
	}
}
