package com.example.torneos.dao;

import com.example.torneos.entities.GrupoLlave;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.FaseActual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoLlaveDao extends JpaRepository<GrupoLlave, Long> {
    List<GrupoLlave> findByTorneoAndFaseTorneoOrderByGrupoLlaveAscPuntosDescGolesFavorDesc(Torneo torneo, FaseActual faseTorneo);
    Optional<GrupoLlave> findByEquipoIdAndTorneoAndFaseTorneo(long equipoId, Torneo torneo, FaseActual faseTorneo);
    void deleteByTorneoAndFaseTorneo(Torneo torneo, FaseActual faseTorneo);
}