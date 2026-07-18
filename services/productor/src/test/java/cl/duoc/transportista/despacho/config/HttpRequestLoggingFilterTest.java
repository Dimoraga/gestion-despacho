package cl.duoc.transportista.despacho.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class HttpRequestLoggingFilterTest {

  private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

  @Test
  void logging_usaSoloAutenticacionVerificada() {
    Jwt jwt =
        new Jwt(
            "verified-token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("preferred_username", "usuario-verificado", "roles", List.of("MANIPULADO")));
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, "n/a", List.of(new SimpleGrantedAuthority("ROLE_GESTION_GUIAS")));

    assertEquals("usuario-verificado", filter.user(authentication));
    assertEquals("[ROLE_GESTION_GUIAS]", filter.roles(authentication));
  }

  @Test
  void logging_sinAutenticacion_esAnonimo() {
    assertEquals("anonymous", filter.user(null));
    assertEquals("[]", filter.roles(null));
  }
}
