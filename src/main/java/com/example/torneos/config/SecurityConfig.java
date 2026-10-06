package com.example.torneos.config;

import com.example.torneos.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuario", "/usuario/login").permitAll()
                       .requestMatchers(HttpMethod.GET, "/usuario").denyAll()
                        .requestMatchers("/usuario", "/usuario/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/pagos/**", "/persona/**", "/jugador/**", "/equipo/delegado/**", "/equipo/solicitudes/**",
                            "/equipo/invitaciones-jugador", "/equipo/torneo/*/participaciones",
                            "/equipo/*/participaciones", "/equipo/*/torneos", "/equipo/*/solicitudes-jugador",
                            "/equipo/*/jugadores/buscar", "/partido/*/convocados", "/partido/*/puede-gestionar",
                            "/partido/*/jugadores-disponibles/*", "/partido/*/jugador/**",
                            "/partido/*/eventos", "/partido/*/sanciones/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, error) ->
                        response.sendError(401, "Autenticación requerida")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}