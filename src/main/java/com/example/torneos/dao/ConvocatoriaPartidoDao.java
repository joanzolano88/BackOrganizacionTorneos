package com.example.torneos.dao;

import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConvocatoriaPartidoDao extends JpaRepository<ConvocatoriaPartido, Long> {
    List<ConvocatoriaPartido> findByPartido(Partido partido);
    Optional<ConvocatoriaPartido> findByPartidoAndJugador(Partido partido, Jugador jugador);
    long countByPartidoAndTitularTrue(Partido partido);
    long countByPartidoAndJugadorEquipoAndTitularTrue(Partido partido, com.example.torneos.entities.Equipo equipo);
    long countByPartidoAndJugadorEquipo(Partido partido, com.example.torneos.entities.Equipo equipo);
}
