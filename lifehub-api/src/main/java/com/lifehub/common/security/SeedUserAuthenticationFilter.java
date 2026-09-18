package com.lifehub.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * v1 (personal, single-user) authentication: every request is treated as the seeded default user.
 * <p>
 * When real login is introduced, only this filter is replaced (e.g. with a JWT filter that resolves
 * {@link AppUserPrincipal} from a verified token). Controllers/services that consume the current user
 * via {@code @CurrentUserId} do not change.
 */
public class SeedUserAuthenticationFilter extends OncePerRequestFilter {

    private final Long seedUserId;

    public SeedUserAuthenticationFilter(Long seedUserId) {
        this.seedUserId = seedUserId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var principal = new AppUserPrincipal(seedUserId);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
