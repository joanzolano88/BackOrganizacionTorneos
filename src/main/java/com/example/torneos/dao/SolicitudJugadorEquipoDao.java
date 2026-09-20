package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.SolicitudJugadorEquipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudJugadorEquipoDao extends JpaRepository<SolicitudJugadorEquipo, Long> {
    Optional<SolicitudJugadorEquipo> findByEquipoAndJugadorAndEstado(Equipo equipo, Jugador jugador, String estado);
    List<SolicitudJugadorEquipo> findByEquipoAndEstado(Equipo equipo, String estado);
}