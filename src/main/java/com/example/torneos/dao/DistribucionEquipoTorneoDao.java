package com.example.torneos.dao;

import com.example.torneos.entities.DistribucionEquipoTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.FaseActual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistribucionEquipoTorneoDao extends JpaRepository<DistribucionEquipoTorneo, Long> {
    List<DistribucionEquipoTorneo> findByTorneoAndFase(Torneo torneo, FaseActual fase);
}