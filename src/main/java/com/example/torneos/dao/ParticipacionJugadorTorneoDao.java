package com.example.torneos.dao;

import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.Torneo;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipacionJugadorTorneoDao extends JpaRepository<ParticipacionJugadorTorneo, Long> {
    List<ParticipacionJugadorTorneo> findByJugador(Jugador jugador);
    Optional<ParticipacionJugadorTorneo> findByTorneoAndJugador(Torneo torneo, Jugador jugador);
    List<ParticipacionJugadorTorneo> findByTorneo(Torneo torneo);
}