package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EquipoDao extends JpaRepository<Equipo, Long> {
    boolean existsByDelegado(Persona delegado);
    @Transactional
    List<Equipo> findByTorneo(Torneo torneo);
    @Transactional
    List<Equipo> findByDelegado(Persona delegado);
    int countByTorneoAndGrupoAndFaseActual(Torneo torneo, int grupo, FaseActual faseTorneo);
    @Transactional
    List<Equipo> findByTorneoAndFaseActual(Torneo torneo, FaseActual faseTorneo);
    @Transactional
    List<Equipo> findByTorneoAndFaseActualIn(Torneo torneo, List<FaseActual> listFA);
}
