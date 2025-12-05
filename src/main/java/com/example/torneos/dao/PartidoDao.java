package com.example.torneos.dao;

import com.example.torneos.entities.Cancha;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface PartidoDao extends JpaRepository<Partido, Long> {
    @Transactional
    List<Partido> findByTorneo(Torneo torneo);

    List<Partido> findByTorneoAndEquipoLocal(Torneo torneo, Equipo equipo);
    @Transactional
    List<Partido> findByEstadoPartido(EstadoPartido estado);
    @Transactional
    List<Partido> findByGrupoAndTorneo(int i, Torneo torneo);
    int countByEstadoPartidoInAndTorneoAndFaseEncuentro(List<EstadoPartido> listEP, Torneo torneo, FaseActual faseTorneo);
    int countByTorneoAndFaseEncuentro(Torneo torneo, FaseActual faseTorneo);
    Partido findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(Torneo torneo, Equipo equipo, Equipo equipo1, FaseActual faseTorneo);
    @Transactional
    List<Partido> findByCanchaAndFechaPartidoBetween(Cancha cancha, LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    @Transactional
    List<Partido> findByEstadoPartidoInAndTorneoAndFechaPartidoBetween(List<EstadoPartido> listEP, Torneo torneo, LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    @Transactional
    List<Partido> findByTorneoAndFaseEncuentroIn(Torneo torneo, List<FaseActual> listaFaseActual);
    @Transactional
    List<Partido> findByTorneoAndFaseEncuentro(Torneo torneo, FaseActual faseActual);
    @Transactional
    List<Partido> findByGrupoAndTorneoAndFaseEncuentro(int i, Torneo torneo, FaseActual faseActual);
}
