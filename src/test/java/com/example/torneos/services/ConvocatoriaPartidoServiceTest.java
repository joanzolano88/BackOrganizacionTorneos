package com.example.torneos.services;

import com.example.torneos.DTO.DtoRegistrarGol;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.TipoEventoPartido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaPartidoServiceTest {
    @Mock private PartidoDao partidoDao;
    @Mock private JugadorDao jugadorDao;
    @Mock private ConvocatoriaPartidoDao convocatoriaDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private AyudanteTorneoDao ayudanteTorneoDao;
    @Mock private EventoPartidoDao eventoDao;
    @Mock private ParticipacionJugadorTorneoDao participacionDao;
    @Mock private SancionJugadorTorneoService sancionService;

    @InjectMocks
    private ConvocatoriaPartidoService service;

    private Partido partido;
    private Equipo local;
    private Equipo visitante;
    private Jugador jugadorLocal;
    private Jugador jugadorVisitante;
    private Jugador asistenteLocal;
    private Usuario organizador;

    @BeforeEach
    void setUp() {
        local = new Equipo();
        local.setId(10L);
        visitante = new Equipo();
        visitante.setId(20L);

        organizador = new Usuario();
        organizador.setId(99L);
        Torneo torneo = new Torneo();
        torneo.setEncargadoTorneo(organizador);

        partido = new Partido();
        partido.setId(5L);
        partido.setEstadoPartido(EstadoPartido.EN_PROCESO);
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partido.setTorneo(torneo);

        jugadorLocal = new Jugador();
        jugadorLocal.setId(100L);
        jugadorLocal.setEquipo(local);
        jugadorVisitante = new Jugador();
        jugadorVisitante.setId(200L);
        jugadorVisitante.setEquipo(visitante);
        asistenteLocal = new Jugador();
        asistenteLocal.setId(101L);
        asistenteLocal.setEquipo(local);

        when(partidoDao.findById(5L)).thenReturn(Optional.of(partido));
        when(usuarioDao.findById(99L)).thenReturn(Optional.of(organizador));
    }

    @Test
    void registrarGol_guardaAutorYAsistenciaYActualizaMarcador() {
        convocar(jugadorLocal);
        convocar(asistenteLocal);
        when(jugadorDao.findById(100L)).thenReturn(Optional.of(jugadorLocal));
        when(jugadorDao.findById(101L)).thenReturn(Optional.of(asistenteLocal));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoDao.save(partido)).thenReturn(partido);
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(100L);
        solicitud.setAsistenciaId(101L);
        solicitud.setMinuto(24);

        service.registrarGol(5L, solicitud, 99L);

        assertEquals(1, partido.getAnotacionesEquipoLocal());
        assertEquals(0, partido.getAnotacionesEquipoVisitante());
        ArgumentCaptor<EventoPartido> eventos = ArgumentCaptor.forClass(EventoPartido.class);
        verify(eventoDao, times(2)).save(eventos.capture());
        assertEquals(List.of(TipoEventoPartido.GOL, TipoEventoPartido.ASISTENCIA),
                eventos.getAllValues().stream().map(EventoPartido::getTipo).toList());
        verify(partidoDao).save(partido);
    }

    @Test
    void registrarGol_autogolSumaAlEquipoObjetivoYRegistraAlJugadorRival() {
        convocar(jugadorVisitante);
        when(jugadorDao.findById(200L)).thenReturn(Optional.of(jugadorVisitante));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoDao.save(partido)).thenReturn(partido);
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(200L);
        solicitud.setAutogol(true);
        solicitud.setMinuto(37);

        service.registrarGol(5L, solicitud, 99L);

        assertEquals(1, partido.getAnotacionesEquipoLocal());
        ArgumentCaptor<EventoPartido> evento = ArgumentCaptor.forClass(EventoPartido.class);
        verify(eventoDao).save(evento.capture());
        assertEquals(TipoEventoPartido.AUTOGOL, evento.getValue().getTipo());
        assertEquals(jugadorVisitante, evento.getValue().getJugador());
    }

    @Test
    void registrarGol_rechazaAutorDeGolDelEquipoContrarioSinCambiarMarcador() {
        convocar(jugadorVisitante);
        when(jugadorDao.findById(200L)).thenReturn(Optional.of(jugadorVisitante));
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(200L);

        assertThrows(IllegalArgumentException.class, () -> service.registrarGol(5L, solicitud, 99L));

        assertEquals(0, partido.getAnotacionesEquipoLocal());
        verify(eventoDao, org.mockito.Mockito.never()).save(any(EventoPartido.class));
    }

    private void convocar(Jugador jugador) {
        when(convocatoriaDao.findByPartidoAndJugador(partido, jugador))
                .thenReturn(Optional.of(new ConvocatoriaPartido()));
    }
}
