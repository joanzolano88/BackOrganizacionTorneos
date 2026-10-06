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
import com.example.torneos.entities.Partido;
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

import com.example.torneos.entities.Reglamento;
import com.example.torneos.entities.Usuario;
import com.example.torneos.entities.Ciudad;
import com.example.torneos.entities.Deporte;

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
        torneo.setModalidadGrupos(com.example.torneos.enums.ModalidadFase.PARTIDO_UNICO);
        com.example.torneos.entities.Usuario organizador = new com.example.torneos.entities.Usuario();
        organizador.setId(3L);
        torneo.setEncargadoTorneo(organizador);
        Equipo equipo = new Equipo();
        equipo.setId(20L);
        ParticipacionEquipoTorneo participacion = new ParticipacionEquipoTorneo();
        participacion.setEstado("PENDIENTE");
        DtoGrupoEquipo distribucion = new DtoGrupoEquipo();
        distribucion.setIdEquipo(20L);
        distribucion.setGrupo(1);
        distribucion.setFaseActual(FaseActual.ELIMINATORIAS_GRUPOS);

        when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
        when(usuarioDao.findById(3L)).thenReturn(Optional.of(organizador));
        when(distribucionDao.findByTorneoAndFase(torneo, FaseActual.ELIMINATORIAS_GRUPOS)).thenReturn(List.of());
        when(equipoDao.findById(20L)).thenReturn(Optional.of(equipo));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)).thenReturn(Optional.of(participacion));

        assertThrows(IllegalArgumentException.class,
            () -> torneoService.cabiarFaseTorneo(List.of(distribucion), 10L, 3L));

        verify(participacionEquipoDao, never()).save(any(ParticipacionEquipoTorneo.class));
        verify(distribucionDao, never()).save(any());
        verify(partidoDao, never()).save(any());
        verify(torneoDao, never()).save(any(Torneo.class));
    }

    @Test
    void cambiarFase_noDebeDuplicarPartidosDeIdaYVueltaExistentes() {
        Torneo torneo = new Torneo();
        torneo.setId(10L);
        torneo.setFaseTorneo(FaseActual.CUARTOS);
        torneo.setEstadoTorneo(EstadoTorneo.ACTIVO);
        torneo.setModalidadEliminatorias(com.example.torneos.enums.ModalidadFase.IDA_VUELTA);
        Usuario organizador = new Usuario();
        organizador.setId(3L);
        torneo.setEncargadoTorneo(organizador);

        Equipo equipoLocal = new Equipo();
        equipoLocal.setId(20L);
        Equipo equipoVisitante = new Equipo();
        equipoVisitante.setId(21L);
        ParticipacionEquipoTorneo participacionLocal = new ParticipacionEquipoTorneo();
        participacionLocal.setEstado("ACEPTADO");
        participacionLocal.setFaseActual(FaseActual.CUARTOS);
        ParticipacionEquipoTorneo participacionVisitante = new ParticipacionEquipoTorneo();
        participacionVisitante.setEstado("ACEPTADO");
        participacionVisitante.setFaseActual(FaseActual.CUARTOS);
        DtoGrupoEquipo local = new DtoGrupoEquipo();
        local.setIdEquipo(20L);
        local.setGrupo(1);
        local.setFaseActual(FaseActual.SEMIFINAL);
        DtoGrupoEquipo visitante = new DtoGrupoEquipo();
        visitante.setIdEquipo(21L);
        visitante.setGrupo(1);
        visitante.setFaseActual(FaseActual.SEMIFINAL);

        when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
        when(usuarioDao.findById(3L)).thenReturn(Optional.of(organizador));
        Partido partidoLocalIda = partidoTerminado(equipoLocal, crearEquipo(22L), 2, 0);
        Partido partidoLocalVuelta = partidoTerminado(crearEquipo(22L), equipoLocal, 0, 1);
        Partido partidoVisitanteIda = partidoTerminado(equipoVisitante, crearEquipo(23L), 2, 0);
        Partido partidoVisitanteVuelta = partidoTerminado(crearEquipo(23L), equipoVisitante, 0, 1);
        when(partidoDao.findByTorneoAndFaseEncuentro(torneo, FaseActual.CUARTOS))
            .thenReturn(List.of(partidoLocalIda, partidoLocalVuelta, partidoVisitanteIda, partidoVisitanteVuelta));
        when(distribucionDao.findByTorneoAndFase(torneo, FaseActual.SEMIFINAL)).thenReturn(List.of());
        when(equipoDao.findById(20L)).thenReturn(Optional.of(equipoLocal));
        when(equipoDao.findById(21L)).thenReturn(Optional.of(equipoVisitante));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipoLocal, torneo)).thenReturn(Optional.of(participacionLocal));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipoVisitante, torneo)).thenReturn(Optional.of(participacionVisitante));
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo, equipoLocal, equipoVisitante, FaseActual.SEMIFINAL))
                .thenReturn(new com.example.torneos.entities.Partido());
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo, equipoVisitante, equipoLocal, FaseActual.SEMIFINAL))
                .thenReturn(new com.example.torneos.entities.Partido());

        torneoService.cabiarFaseTorneo(List.of(local, visitante), 10L, 3L);

        verify(partidoDao, never()).save(any());
    }

    @Test
    void cambiarFase_debeRechazarEquipoQuePerdioLaLlaveAnterior() {
        Torneo torneo = new Torneo();
        torneo.setId(12L);
        torneo.setFaseTorneo(FaseActual.CUARTOS);
        torneo.setEstadoTorneo(EstadoTorneo.ACTIVO);
        torneo.setModalidadEliminatorias(com.example.torneos.enums.ModalidadFase.PARTIDO_UNICO);
        Usuario organizador = new Usuario();
        organizador.setId(4L);
        torneo.setEncargadoTorneo(organizador);
        Equipo ganador = crearEquipo(30L);
        Equipo perdedor = crearEquipo(31L);
        Partido partidoAnterior = partidoTerminado(ganador, perdedor, 2, 1);
        DtoGrupoEquipo equipoPerdedor = new DtoGrupoEquipo();
        equipoPerdedor.setIdEquipo(perdedor.getId());
        equipoPerdedor.setGrupo(1);
        equipoPerdedor.setFaseActual(FaseActual.SEMIFINAL);

        when(torneoDao.findById(12L)).thenReturn(Optional.of(torneo));
        when(usuarioDao.findById(4L)).thenReturn(Optional.of(organizador));
        when(partidoDao.findByTorneoAndFaseEncuentro(torneo, FaseActual.CUARTOS)).thenReturn(List.of(partidoAnterior));

        assertThrows(IllegalArgumentException.class,
                () -> torneoService.cabiarFaseTorneo(List.of(equipoPerdedor), 12L, 4L));

        verify(distribucionDao, never()).deleteAll(any());
        verify(participacionEquipoDao, never()).save(any(ParticipacionEquipoTorneo.class));
        verify(torneoDao, never()).save(any(Torneo.class));
    }

    private Equipo crearEquipo(long id) {
        Equipo equipo = new Equipo();
        equipo.setId(id);
        return equipo;
    }

    private Partido partidoTerminado(Equipo local, Equipo visitante, int golesLocal, int golesVisitante) {
        Partido partido = new Partido();
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partido.setFaseEncuentro(FaseActual.CUARTOS);
        partido.setEstadoPartido(com.example.torneos.enums.EstadoPartido.TERMINADO);
        partido.setAnotacionesEquipoLocal(golesLocal);
        partido.setAnotacionesEquipoVisitante(golesVisitante);
        return partido;
    }

    @Test
    void getByUsuarioId_debeLanzarExcepcionSiElUsuarioNoExiste() {
        when(usuarioDao.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> torneoService.getByUsuarioId(99L));
    }

    @Test
    void save_debeRechazarTorneoConInformacionObligatoriaFaltante() {
        Torneo torneo = new Torneo();
        torneo.setNombre("Torneo sin datos completos");
        torneo.setModalidadTorneo(com.example.torneos.enums.ModalidadTorneo.GRUPOS);
        torneo.setModalidadGrupos(com.example.torneos.enums.ModalidadFase.PARTIDO_UNICO);
        torneo.setCantidadEquipos(0);
        torneo.setCantidadGrupos(0);
        torneo.setDuracionMinutos(0);
        Usuario organizador = new Usuario();
        organizador.setId(5L);
        organizador.setTipoUsuario(com.example.torneos.enums.TipoUsuario.ORGANIZADOR);
        organizador.setUbicacion("Popayán");

        when(usuarioDao.findById(5L)).thenReturn(Optional.of(organizador));

        assertThrows(IllegalArgumentException.class, () -> torneoService.save(torneo, null, 5L));
    }

    @Test
    void save_debePermitirEliminatoriasSinModalidadDeGrupos() {
        Torneo torneo = new Torneo();
        torneo.setNombre("Copa de prueba");
        torneo.setUbicacion("Popayán");
        torneo.setCantidadEquipos(8);
        torneo.setDuracionMinutos(90);
        torneo.setModalidadTorneo(com.example.torneos.enums.ModalidadTorneo.ELIMINATORIAS);
        torneo.setFaseInicioEliminatorias(FaseActual.CUARTOS);
        torneo.setModalidadEliminatorias(com.example.torneos.enums.ModalidadFase.PARTIDO_UNICO);
        torneo.setModoCambioJugador(com.example.torneos.enums.ModoCambioJugador.LIBRES);
        Ciudad ciudad = new Ciudad();
        ciudad.setId(1L);
        ciudad.setNombre("Popayán");
        torneo.setCiudad(ciudad);
        Deporte deporte = new Deporte();
        deporte.setId(1L);
        torneo.setDeporte(deporte);
        Usuario organizador = new Usuario();
        organizador.setId(5L);
        organizador.setTipoUsuario(com.example.torneos.enums.TipoUsuario.ORGANIZADOR);
        organizador.setUbicacion("Popayán");

        when(usuarioDao.findById(5L)).thenReturn(Optional.of(organizador));

        torneoService.save(torneo, null, 5L);

        verify(torneoDao).save(torneo);
    }

    @Test
    void getReglamento_debeLanzarExcepcionSiElTorneoNoExiste() {
        when(torneoDao.findById(77L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> torneoService.getReglamento(77L));
    }
}