package cl.duoc.transportista.despacho.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!local")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/actuator/health"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/guias/*/s3")
                    .hasAnyRole("DESCARGADOR", "GESTOR")

                .requestMatchers("/api/guias/**")
                    .hasRole("GESTOR")

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new AzureRolesConverter());
        return converter;
    }

    static class AzureRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> values = new ArrayList<>();
            values.addAll(asList(jwt.getClaim("roles")));
            values.addAll(asList(jwt.getClaim("extension_consultaRole")));
            values.addAll(asList(jwt.getClaim("scp")));

            return values.stream()
                .flatMap(value -> Stream.of(value.split("[ ,]")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceFirst("^ROLE_", ""))
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(SecurityConfig.AzureRolesConverter::normalizeRole)
                .filter(role -> role.equals("DESCARGADOR") || role.equals("GESTOR"))
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        }

        private static String normalizeRole(String role) {
            return switch (role) {
                case "DESCARGA", "DESCARGADOR", "CONSULTA" -> "DESCARGADOR";
                case "GESTION", "GESTOR", "ADMIN" -> "GESTOR";
                default -> role;
            };
        }

        @SuppressWarnings("unchecked")
        private static List<String> asList(Object claim) {
            if (claim == null) {
                return List.of();
            }
            if (claim instanceof String value) {
                return List.of(value);
            }
            if (claim instanceof Collection<?> values) {
                return values.stream().map(String::valueOf).toList();
            }
            if (claim instanceof Map<?, ?> map && map.containsKey("roles")) {
                return asList(map.get("roles"));
            }
            return List.of(String.valueOf(claim));
        }
    }
}
