package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.PagoInscripcion;
import com.example.torneos.entities.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoInscripcionDao extends JpaRepository<PagoInscripcion, Long> {
    List<PagoInscripcion> findByEquipoOrderByFechaPagoDesc(Equipo equipo);
    List<PagoInscripcion> findByEquipoAndTorneoOrderByFechaPagoDesc(Equipo equipo, Torneo torneo);
}