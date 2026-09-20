package com.example.torneos.dao;

import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.example.torneos.enums.TipoEventoPartido;

public interface EventoPartidoDao extends JpaRepository<EventoPartido, Long> {
    List<EventoPartido> findByPartidoOrderByMinutoAscIdAsc(Partido partido);
    boolean existsByPartidoAndJugadorAndTipo(Partido partido, com.example.torneos.entities.Jugador jugador, TipoEventoPartido tipo);
}
