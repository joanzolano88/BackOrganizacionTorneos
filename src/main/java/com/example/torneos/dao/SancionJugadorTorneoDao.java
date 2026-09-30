package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.SancionJugadorTorneo;
import com.example.torneos.entities.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SancionJugadorTorneoDao extends JpaRepository<SancionJugadorTorneo, Long> {
    Optional<SancionJugadorTorneo> findByTorneoAndJugadorAndActivaTrue(Torneo torneo, Jugador jugador);
    List<SancionJugadorTorneo> findByTorneoAndEquipoAndActivaTrue(Torneo torneo, Equipo equipo);
    List<SancionJugadorTorneo> findByTorneoAndJugador(Torneo torneo, Jugador jugador);
}