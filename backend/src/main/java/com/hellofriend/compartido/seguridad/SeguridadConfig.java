package com.hellofriend.compartido.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Seguridad sin sesión: el login entrega un JWT y el frontend lo envía en {@code Authorization: Bearer ...}. El claim
 * "rol" se convierte en la autoridad {@code ROLE_<rol>}, así que los controladores usan
 * {@code @PreAuthorize("hasRole('ADMINISTRADOR')")}. Los errores salen como {@code {"mensaje": "..."}}.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
		JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
		roles.setAuthoritiesClaimName(ServicioJwt.CLAIM_ROL);
		roles.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(roles);

		AuthenticationEntryPoint noAutenticado = (peticion, respuesta, e) -> escribir(respuesta, 401,
				"Debes iniciar sesión.");
		AccessDeniedHandler sinPermiso = (peticion, respuesta, e) -> escribir(respuesta, 403,
				"No tienes permiso para realizar esta acción.");

		http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/auth/login", "/auth/registro", "/auth/recuperar", "/auth/restablecer",
								"/actuator/health", "/error")
						.permitAll().anyRequest().authenticated())
				.exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado).accessDeniedHandler(sinPermiso))
				.oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor))
						.authenticationEntryPoint(noAutenticado).accessDeniedHandler(sinPermiso));
		return http.build();
	}

	private static void escribir(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
		respuesta.setStatus(estado);
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
		respuesta.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${hellofriend.cors.origenes}") String origenes) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::strip).toList());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", cors);
		return fuente;
	}

	@Bean
	SecretKey claveJwt(@Value("${hellofriend.jwt.secreto}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder codificadorJwt(SecretKey clave) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
	}

	/**
	 * Además de la firma, el emisor y la expiración, la sesión debe seguir vigente: cuenta activa y "ver" igual a la
	 * versión de sesión guardada. Así cerrar sesión o desactivar una cuenta corta el acceso al instante.
	 */
	@Bean
	JwtDecoder decodificadorJwt(SecretKey clave, VerificadorSesion verificador) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256)
				.build();
		OAuth2TokenValidator<Jwt> sesionVigente = jwt -> {
			Object version = jwt.getClaim(ServicioJwt.CLAIM_VERSION);
			boolean vigente = jwt.getSubject() != null && version instanceof Number numero
					&& verificador.sesionVigente(jwt.getSubject(), numero.intValue());
			return vigente
					? OAuth2TokenValidatorResult.success()
					: OAuth2TokenValidatorResult
							.failure(new OAuth2Error("invalid_token", "La sesión ya no es válida", null));
		};
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(ServicioJwt.EMISOR), sesionVigente));
		return decodificador;
	}

	@Bean
	PasswordEncoder codificadorContrasenas(@Value("${hellofriend.seguridad.costo-bcrypt:12}") int costo) {
		return new BCryptPasswordEncoder(costo);
	}

}
