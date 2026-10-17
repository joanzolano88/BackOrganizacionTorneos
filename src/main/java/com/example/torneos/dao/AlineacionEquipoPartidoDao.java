package com.example.torneos.dao;

import com.example.torneos.entities.AlineacionEquipoPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Partido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlineacionEquipoPartidoDao extends JpaRepository<AlineacionEquipoPartido, Long> {
    List<AlineacionEquipoPartido> findByPartido(Partido partido);
    Optional<AlineacionEquipoPartido> findByPartidoAndEquipo(Partido partido, Equipo equipo);
}
