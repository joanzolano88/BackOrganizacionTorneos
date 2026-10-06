package com.example.torneos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(new ObjectMapper());
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-with-at-least-thirty-two-bytes-long");
        ReflectionTestUtils.setField(tokenService, "expirationMs", 60_000L);
    }

    @Test
    void validar_aceptaTokenFirmadoYDevuelveIdentidad() {
        String token = tokenService.emitir(45L, "identificacion-45", "ORGANIZADOR");

        Map<String, Object> claims = tokenService.validar(token);

        assertEquals(45L, ((Number) claims.get("uid")).longValue());
        assertEquals("identificacion-45", claims.get("sub"));
        assertEquals("ORGANIZADOR", claims.get("rol"));
    }

    @Test
    void validar_rechazaFirmaAlterada() {
        String[] partes = tokenService.emitir(45L, "identificacion-45", "JUGADOR").split("\\.");
        partes[2] = (partes[2].startsWith("A") ? "B" : "A") + partes[2].substring(1);

        assertThrows(IllegalArgumentException.class, () -> tokenService.validar(String.join(".", partes)));
    }

    @Test
    void validar_rechazaTokenExpirado() {
        ReflectionTestUtils.setField(tokenService, "expirationMs", -1_000L);
        String token = tokenService.emitir(45L, "identificacion-45", "DELEGADO");

        assertThrows(IllegalArgumentException.class, () -> tokenService.validar(token));
    }
}