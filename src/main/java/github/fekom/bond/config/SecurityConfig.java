package github.fekom.bond.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Desabilita CSRF (não necessário para API stateless)
			.csrf(csrf -> csrf.disable())
			// Configura sessão como stateless (JWT)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Configura autorização de endpoints
			.authorizeHttpRequests(auth ->
				auth
					// Endpoints públicos (sem autenticação)
					.requestMatchers(
						"/actuator/health",
						"/actuator/info",
						"/actuator/prometheus",
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/v3/api-docs/**",
						"/api-docs/**"
					)
					.permitAll()
					// Endpoint de criar cliente - requer role ADMIN
					.requestMatchers("/clients")
					.hasRole("ADMIN")
					// Endpoint de check rate limit - requer autenticação (qualquer role)
					.requestMatchers("/check")
					.authenticated()
					// Qualquer outro endpoint requer autenticação
					.anyRequest()
					.authenticated()
			)
			// Configura OAuth2 Resource Server com JWT
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	/**
	 * Converte roles do Keycloak (realm_access.roles) para authorities do Spring Security
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
		grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
			var authorities = grantedAuthoritiesConverter.convert(jwt);

			// Também extrai roles de realm_access.roles (padrão Keycloak)
			var realmAccess = jwt.getClaimAsMap("realm_access");
			if (realmAccess != null && realmAccess.containsKey("roles")) {
				@SuppressWarnings("unchecked")
				var roles = (java.util.List<String>) realmAccess.get("roles");
				var realmAuthorities = roles
					.stream()
					.map(role ->
						new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
					)
					.toList();

				var allAuthorities = new java.util.ArrayList<>(authorities);
				allAuthorities.addAll(realmAuthorities);
				return allAuthorities;
			}

			return authorities;
		});

		return jwtAuthenticationConverter;
	}
}
