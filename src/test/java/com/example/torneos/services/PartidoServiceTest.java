package com.example.torneos.services;

import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadFase;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.enums.PeriodoPartido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {

    @Mock private PartidoDao partidoDao;
    @Mock private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Mock private TorneoDao torneoDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private AyudanteTorneoDao ayudanteTorneoDao;

    @InjectMocks
    private PartidoService partidoService;

    private Torneo torneo;
    private Equipo equipoLocal;
    private Equipo equipoVisitante;
    private Usuario organizador;

    @BeforeEach
    void configurarPartidoDeEliminatoriasIdaYVuelta() {
        torneo = new Torneo();
        torneo.setId(10L);
        torneo.setFaseTorneo(FaseActual.SEMIFINAL);
        torneo.setModalidadTorneo(ModalidadTorneo.ELIMINATORIAS);
        torneo.setModalidadGrupos(ModalidadFase.PARTIDO_UNICO);
        torneo.setModalidadEliminatorias(ModalidadFase.IDA_VUELTA);
        organizador = new Usuario();
        organizador.setId(7L);
        torneo.setEncargadoTorneo(organizador);

        equipoLocal = new Equipo();
        equipoLocal.setId(20L);
        equipoVisitante = new Equipo();
        equipoVisitante.setId(21L);

        ParticipacionEquipoTorneo participacionLocal = crearParticipacion(equipoLocal);
        ParticipacionEquipoTorneo participacionVisitante = crearParticipacion(equipoVisitante);

        lenient().when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
        when(usuarioDao.findById(7L)).thenReturn(Optional.of(organizador));
        lenient().when(ayudanteTorneoDao.findByTorneoAndUsuario(torneo, organizador)).thenReturn(Optional.empty());
        lenient().when(participacionEquipoDao.findByTorneoAndEstadoAndFaseActual(torneo, "ACEPTADO", FaseActual.SEMIFINAL))
                .thenReturn(List.of(participacionLocal, participacionVisitante));
        lenient().when(participacionEquipoDao.findByEquipoAndTorneo(equipoLocal, torneo)).thenReturn(Optional.of(participacionLocal));
        lenient().when(participacionEquipoDao.findByEquipoAndTorneo(equipoVisitante, torneo)).thenReturn(Optional.of(participacionVisitante));
        lenient().when(partidoDao.save(any(Partido.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generarPartidos_debeDevolverSoloLosNuevosPartidosDeIdaYVuelta() {
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(
                torneo, equipoLocal, equipoVisitante, FaseActual.SEMIFINAL)).thenReturn(null);
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(
                torneo, equipoVisitante, equipoLocal, FaseActual.SEMIFINAL)).thenReturn(null);

        List<Partido> partidos = partidoService.generarPartidos(10L, 7L);

        assertEquals(2, partidos.size());
        assertEquals(EstadoPartido.PENDIENTE, partidos.get(0).getEstadoPartido());
        assertEquals(FaseActual.SEMIFINAL, partidos.get(0).getFaseEncuentro());
        assertEquals(equipoLocal, partidos.get(0).getEquipoLocal());
        assertEquals(equipoVisitante, partidos.get(1).getEquipoLocal());
        verify(partidoDao, times(2)).save(any(Partido.class));
    }

    @Test
    void generarPartidos_debeDevolverListaVaciaSiYaExistenLosEncuentros() {
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(
                torneo, equipoLocal, equipoVisitante, FaseActual.SEMIFINAL)).thenReturn(new Partido());
        when(partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(
                torneo, equipoVisitante, equipoLocal, FaseActual.SEMIFINAL)).thenReturn(new Partido());

        List<Partido> partidos = partidoService.generarPartidos(10L, 7L);

        assertEquals(0, partidos.size());
        verify(partidoDao, never()).save(any(Partido.class));
    }

    @Test
    void cerrarPeriodos_debeUsarDuracionesDelTorneoSinAcumularAlargues() {
        torneo.setDuracionTiempoMinutos(40);
        torneo.setDuracionTiempoExtraMinutos(10);
        Partido partido = new Partido();
        partido.setId(5L);
        partido.setTorneo(torneo);
        partido.setEstadoPartido(EstadoPartido.EN_PROCESO);
        partido.setControlTiempoActivo(true);
        partido.setInicioPeriodoActual(Instant.now());
        partido.setPeriodoActual(PeriodoPartido.PRIMER_TIEMPO);
        when(partidoDao.findById(5L)).thenReturn(Optional.of(partido));

        partidoService.cerrarPeriodo(5L, 3, "", 7L);
        assertEquals(40, partido.getMinutoBasePeriodo());

        partido.setPeriodoActual(PeriodoPartido.SEGUNDO_TIEMPO);
        partido.setInicioPeriodoActual(Instant.now());
        partidoService.cerrarPeriodo(5L, 5, "PRORROGA", 7L);
        assertEquals(80, partido.getMinutoBasePeriodo());

        partido.setPeriodoActual(PeriodoPartido.PRIMER_TIEMPO_EXTRA);
        partido.setInicioPeriodoActual(Instant.now());
        partidoService.cerrarPeriodo(5L, 2, "", 7L);
        assertEquals(90, partido.getMinutoBasePeriodo());

        partido.setPeriodoActual(PeriodoPartido.SEGUNDO_TIEMPO_EXTRA);
        partido.setInicioPeriodoActual(Instant.now());
        partidoService.cerrarPeriodo(5L, 4, "PENALTIS", 7L);
        assertEquals(100, partido.getMinutoBasePeriodo());
    }

    private ParticipacionEquipoTorneo crearParticipacion(Equipo equipo) {
        ParticipacionEquipoTorneo participacion = new ParticipacionEquipoTorneo();
        participacion.setEquipo(equipo);
        participacion.setGrupo(1);
        return participacion;
    }
}