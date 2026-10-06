package com.example.torneos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern USER_PATH = Pattern.compile("^/usuario/(\\d+)(?:/.*)?$");
    private final JwtTokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getMethod().equals(HttpMethod.OPTIONS.name()) ||
                request.getMethod().equals(HttpMethod.POST.name()) &&
                        (request.getRequestURI().endsWith("/usuario/login") || request.getRequestURI().equals("/usuario"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Map<String, Object> claims = tokenService.validar(authorization.substring(7).trim());
                long usuarioId = ((Number) claims.get("uid")).longValue();
                String rol = String.valueOf(claims.get("rol"));
                var authentication = new UsernamePasswordAuthenticationToken(usuarioId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (!coincideUsuario(request, usuarioId)) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "El usuario solicitado no coincide con la sesión");
                    return;
                }
            } catch (IllegalArgumentException error) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o vencido");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean coincideUsuario(HttpServletRequest request, long usuarioId) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        Matcher matcher = USER_PATH.matcher(path);
        if (matcher.matches() && Long.parseLong(matcher.group(1)) != usuarioId) return false;
        String usuarioSolicitado = request.getParameter("usuarioId");
        return usuarioSolicitado == null || usuarioSolicitado.isBlank() || Long.parseLong(usuarioSolicitado) == usuarioId;
    }
}