package cl.duoc.transportista.despacho.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long startedAt = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startedAt;
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      log.info(
          "HTTP {} {} -> {} ({} ms) ip={} user={} roles={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs,
          clientIp(request),
          user(request, authentication),
          roles(request, authentication));
    }
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String user(HttpServletRequest request, Authentication authentication) {
    Optional<Map<String, Object>> jwtClaims = jwtClaims(request);
    if (jwtClaims.isPresent()) {
      Map<String, Object> claims = jwtClaims.get();
      return firstClaim(claims, "preferred_username", "name", "appid", "azp", "sub");
    }
    if (authentication == null || !authentication.isAuthenticated()) {
      return "anonymous";
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      String preferredUsername = jwt.getClaimAsString("preferred_username");
      if (preferredUsername != null && !preferredUsername.isBlank()) {
        return preferredUsername;
      }
      String name = jwt.getClaimAsString("name");
      if (name != null && !name.isBlank()) {
        return name;
      }
      return jwt.getSubject();
    }
    return authentication.getName();
  }

  private String roles(HttpServletRequest request, Authentication authentication) {
    Optional<Map<String, Object>> jwtClaims = jwtClaims(request);
    if (jwtClaims.isPresent()) {
      Object rolesClaim = jwtClaims.get().get("roles");
      if (rolesClaim instanceof List<?> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
      }
      if (rolesClaim instanceof String value) {
        return "[" + value + "]";
      }
    }
    if (authentication == null || !authentication.isAuthenticated()) {
      return "[]";
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(",", "[", "]"));
  }

  private String firstClaim(Map<String, Object> claims, String... names) {
    for (String name : names) {
      Object value = claims.get(name);
      if (value != null && !String.valueOf(value).isBlank()) {
        return String.valueOf(value);
      }
    }
    return "jwt";
  }

  private Optional<Map<String, Object>> jwtClaims(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return Optional.empty();
    }
    String[] parts = authorization.substring("Bearer ".length()).split("\\.");
    if (parts.length < 2) {
      return Optional.empty();
    }
    try {
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      @SuppressWarnings("unchecked")
      Map<String, Object> claims =
          new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
      return Optional.of(claims);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
