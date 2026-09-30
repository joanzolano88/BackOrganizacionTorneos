package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.dao.DistribucionEquipoTorneoDao;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.ReglamentoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.FaseActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorenoServiceTest {

    @Mock private TorneoDao torneoDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private ReglamentoDao reglamentoDao;
    @Mock private PartidoDao partidoDao;
    @Mock private EquipoDao equipoDao;
    @Mock private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Mock private DistribucionEquipoTorneoDao distribucionDao;

    @InjectMocks
    private TorenoService torneoService;

    @Test
    void cambiarFase_debeRechazarEquiposConSolicitudPendiente() {
        Torneo torneo = new Torneo();
        torneo.setId(10L);
        torneo.setFaseTorneo(FaseActual.FASE_GRUPOS);
        torneo.setEstadoTorneo(EstadoTorneo.ACTIVO);
        Equipo equipo = new Equipo();
        equipo.setId(20L);
        ParticipacionEquipoTorneo participacion = new ParticipacionEquipoTorneo();
        participacion.setEstado("PENDIENTE");
        DtoGrupoEquipo distribucion = new DtoGrupoEquipo();
        distribucion.setIdEquipo(20L);
        distribucion.setGrupo(1);
        distribucion.setFaseActual(FaseActual.ELIMINATORIAS_GRUPOS);

        when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
        when(distribucionDao.findByTorneoAndFase(torneo, FaseActual.ELIMINATORIAS_GRUPOS)).thenReturn(List.of());
        when(equipoDao.findById(20L)).thenReturn(Optional.of(equipo));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)).thenReturn(Optional.of(participacion));

        assertThrows(IllegalArgumentException.class,
            () -> torneoService.cabiarFaseTorneo(List.of(distribucion), 10L));

        verify(participacionEquipoDao, never()).save(any(ParticipacionEquipoTorneo.class));
        verify(distribucionDao, never()).save(any());
        verify(partidoDao, never()).save(any());
        verify(torneoDao, never()).save(any(Torneo.class));
    }
}