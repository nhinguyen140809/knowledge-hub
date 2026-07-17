package com.knowledgehub.access.infrastructure.security;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.port.Authenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer <secret>} header and, when present, resolves it to a
 * principal placed in the security context with a {@code ROLE_*} authority for method security. A
 * header that is present but invalid fails the request immediately (fail-closed); a missing header
 * is left for the authorization rules to reject on protected endpoints. Applies to every HTTP
 * request, REST and {@code /mcp} alike, since both run through this chain. Wired as a bean by
 * {@link SecurityConfig} rather than component-scanned, so it loads only with the full security
 * context.
 *
 * <p>Example: a request carrying {@code Authorization: Bearer 3f9c…} whose secret hashes to an
 * active credential of principal {@code team-alpha} (role {@code MEMBER}) continues down the chain
 * with the context holding that principal and authority {@code ROLE_MEMBER}. The same request with
 * a revoked or mistyped secret is answered on the spot with a 401 problem-detail body and never
 * reaches a controller.
 */
class BearerAuthFilter extends OncePerRequestFilter {

  private static final String PREFIX = "Bearer ";

  private final Authenticator authenticator;
  private final ProblemSecurityHandler problemHandler;

  BearerAuthFilter(Authenticator authenticator, ProblemSecurityHandler problemHandler) {
    this.authenticator = authenticator;
    this.problemHandler = problemHandler;
  }

  /**
   * Settles the request's authentication in one of three ways:
   *
   * <ul>
   *   <li><b>No bearer header</b> — the request continues unauthenticated; the authorization rules
   *       later return 401 on protected endpoints, while public ones (health, API docs) stay
   *       reachable without a token.
   *   <li><b>Header present, secret unknown or revoked</b> — the context is cleared and the request
   *       ends here with a 401 problem-detail response (fail-closed); the filter chain is not
   *       continued.
   *   <li><b>Secret resolves to a principal</b> — an {@link AuthenticatedPrincipal} is stored in
   *       the {@link SecurityContextHolder} with authority {@code ROLE_<role>} (what
   *       {@code @PreAuthorize("hasRole('ADMIN')")} checks), and the request continues.
   * </ul>
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    String secret = header.substring(PREFIX.length()).trim();
    Optional<AuthenticatedPrincipal> principal = authenticator.authenticate(secret);
    if (principal.isEmpty()) {
      SecurityContextHolder.clearContext();
      problemHandler.commence(request, response, new BadCredentialsException("Invalid credential"));
      return;
    }

    AuthenticatedPrincipal authenticated = principal.get();
    var authentication =
        new PreAuthenticatedAuthenticationToken(
            authenticated,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + authenticated.role().name())));
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    chain.doFilter(request, response);
  }
}
