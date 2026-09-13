package com.example.torneos.dao;

import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoPartidoDao extends JpaRepository<EventoPartido, Long> {
    List<EventoPartido> findByPartidoOrderByMinutoAscIdAsc(Partido partido);
}
