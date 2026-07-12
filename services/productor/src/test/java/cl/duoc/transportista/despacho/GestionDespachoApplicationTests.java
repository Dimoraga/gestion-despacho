package cl.duoc.transportista.despacho;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
class GestionDespachoApplicationTests {

  @TestConfiguration
  static class SecurityTestConfig {
    @Bean
    public JwtDecoder jwtDecoder() {
      // JwtDecoder falso solo para tests — no valida tokens reales
      return token -> {
        throw new UnsupportedOperationException("Test decoder");
      };
    }
  }

  @Test
  void contextLoads() {}
}
