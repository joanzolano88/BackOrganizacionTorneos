package com.example.torneos.services;

import com.example.torneos.dao.PersonaDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MigracionPersonaCompartidaTest {
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private PersonaDao personaDao;
    @Mock private ApplicationArguments applicationArguments;

    @Test
    void run_permiteNulosEnColumnasPersonalesLegadas() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String tabla = invocation.getArgument(2);
                    String columna = invocation.getArgument(3);
                    return "jugador".equals(tabla) && "fecha_nacimiento".equals(columna) ? 0 : 1;
                });
        when(jdbcTemplate.queryForObject(contains("setval"), eq(Long.class))).thenReturn(1L);
        lenient().when(jdbcTemplate.queryForList(anyString())).thenReturn(java.util.List.of());
        lenient().when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());
        lenient().when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(java.util.List.of());

        new MigracionPersonaCompartida(jdbcTemplate, personaDao).run(applicationArguments);

        verify(jdbcTemplate).execute(
                "ALTER TABLE jugador ALTER COLUMN whatsapp_activo DROP NOT NULL");
        verify(jdbcTemplate).execute(
                "ALTER TABLE usuario ALTER COLUMN whatsapp_activo DROP NOT NULL");
    }

    @Test
    void run_sincronizaSecuenciaDeJugadorCuandoExiste() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("setval"), eq(Long.class))).thenReturn(1L);
        when(jdbcTemplate.queryForList(contains("information_schema.sequences"), eq(String.class)))
                .thenReturn(List.of("jugador_seq"));

        new MigracionPersonaCompartida(jdbcTemplate, personaDao).run(applicationArguments);

        verify(jdbcTemplate).queryForObject(
                "SELECT setval('jugador_seq', COALESCE((SELECT MAX(id) FROM jugador), 0) + 1, false)",
                Long.class);
    }
}
