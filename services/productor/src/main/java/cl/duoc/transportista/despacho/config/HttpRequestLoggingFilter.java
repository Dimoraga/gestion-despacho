package cl.duoc.transportista.despacho.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
          user(authentication),
          roles(authentication));
    }
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  String user(Authentication authentication) {
    if (isAnonymous(authentication)) {
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

  String roles(Authentication authentication) {
    if (isAnonymous(authentication)) {
      return "[]";
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(",", "[", "]"));
  }

  private boolean isAnonymous(Authentication authentication) {
    return authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken;
  }
}
