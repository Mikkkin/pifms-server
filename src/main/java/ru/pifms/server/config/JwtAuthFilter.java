package ru.pifms.server.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthFilter(JwtTokenProvider jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jws<Claims> jws = jwt.parseAndValidate(token);
                if (jwt.isAccessToken(jws)) {
                    String username = jws.getBody().getSubject();
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) jws.getBody().get("roles");
                    Collection<SimpleGrantedAuthority> authorities =
                        roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())).toList();

                    AbstractAuthenticationToken authentication =
                        new AbstractAuthenticationToken(authorities) {
                            @Override public Object getCredentials() { return token; }
                            @Override public Object getPrincipal() { return username; }
                        };
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
            }
        }
        chain.doFilter(req, res);
    }
}
