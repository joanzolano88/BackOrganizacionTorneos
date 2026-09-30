package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.FaseActual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipacionEquipoTorneoDao extends JpaRepository<ParticipacionEquipoTorneo, Long> {
    Optional<ParticipacionEquipoTorneo> findByEquipoAndTorneo(Equipo equipo, Torneo torneo);
    List<ParticipacionEquipoTorneo> findByEquipo(Equipo equipo);
    List<ParticipacionEquipoTorneo> findByTorneo(Torneo torneo);
    List<ParticipacionEquipoTorneo> findByTorneoAndEstado(Torneo torneo, String estado);
    List<ParticipacionEquipoTorneo> findByTorneoAndEstadoAndFaseActual(Torneo torneo, String estado, FaseActual fase);
    long countByTorneoAndEstado(Torneo torneo, String estado);
}