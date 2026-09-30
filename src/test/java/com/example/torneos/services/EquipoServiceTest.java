package com.example.torneos.services;

import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EquipoDao equipoDao;

    @Mock
    private TorneoDao torneoDao;

    @Mock
    private PartidoDao partidoDao;

    @Mock
    private PersonaDao personaDao;

    @Mock
    private UsuarioDao usuarioDao;

    @Mock
    private JugadorDao jugadorDao;

    @Mock
    private ParticipacionJugadorTorneoDao participacionDao;

    @Mock
    private ParticipacionEquipoTorneoDao participacionEquipoDao;

    @Mock
    private NotificacionUsuarioDao notificacionDao;

    @InjectMocks
    private EquipoService equipoService;

    private Torneo torneo;
    private Persona delegado;

    @BeforeEach
    void setup() {
        torneo = new Torneo();
        torneo.setId(10L);
        torneo.setNombre("Copa Test");
        torneo.setCantidadEquipos(8);
        torneo.setEstadoTorneo(EstadoTorneo.INSCRIPCIONES);
        torneo.setFaseTorneo(FaseActual.FASE_GRUPOS);

        delegado = new Persona();
        delegado.setId(5L);
        delegado.setNombre("Ana");
        delegado.setNumeroCelular("3001234567");
        lenient().when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
    }

    @Test
    void saveSolicitud_debeCrearSolicitudSinAsignarFase() {
        Equipo solicitud = new Equipo();
        solicitud.setNombre("Equipo Rojo");
        solicitud.setId(100L);
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);
        torneo.setUbicacion("Popayan");

        Usuario usuario = new Usuario();
        usuario.setNumeroCelular("3001234567");
        usuario.setUbicacion("Popayan");

        lenient().when(personaDao.existsById(any(Long.class))).thenReturn(false);
        lenient().when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(personaDao.findAll()).thenReturn(Collections.emptyList());
        lenient().when(usuarioDao.findByNumeroCelular("3001234567")).thenReturn(usuario);
        when(equipoDao.findById(100L)).thenReturn(Optional.of(solicitud));
        when(participacionEquipoDao.findByEquipoAndTorneo(solicitud, torneo)).thenReturn(Optional.empty());
        when(participacionEquipoDao.findByTorneo(torneo)).thenReturn(Collections.emptyList());
        when(participacionEquipoDao.save(any(ParticipacionEquipoTorneo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipo resultado = equipoService.saveSolicitud(solicitud);

        assertNotNull(resultado);
        assertNull(resultado.getFaseActual());
        assertEquals(0, resultado.getGrupo());
        assertEquals(100L, resultado.getId());
        assertEquals("PENDIENTE", resultado.getParticipacionTorneo().getEstado());
        assertEquals(10L, resultado.getParticipacionTorneo().getTorneo().getId());
        verify(equipoDao, never()).save(any(Equipo.class));
    }

    @Test
    void save_debeCrearEquipoSinTorneoCuandoSeCreaIndependiente() {
        Equipo equipo = new Equipo();
        equipo.setNombre("Equipo Independiente");
        equipo.setDelegado(delegado);

        when(personaDao.existsById(any(Long.class))).thenReturn(false);
        when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(equipoDao.save(any(Equipo.class))).thenAnswer(invocation -> {
            Equipo guardado = invocation.getArgument(0);
            guardado.setId(200L);
            return guardado;
        });

        Equipo resultado = equipoService.save(equipo);

        assertNotNull(resultado);
        assertEquals("Equipo Independiente", resultado.getNombre());
        assertNull(resultado.getTorneo());
        assertNull(resultado.getFaseActual());
    }

    @Test
    void saveSolicitud_debePermitirRelacionarElMismoEquipoConVariosTorneos() {
        Equipo equipoGuardado = new Equipo();
        equipoGuardado.setId(77L);
        equipoGuardado.setNombre("Equipo Compartido");
        equipoGuardado.setDelegado(delegado);
        Torneo segundoTorneo = new Torneo();
        segundoTorneo.setId(11L);
        segundoTorneo.setNombre("Copa Dos");
        segundoTorneo.setUbicacion("Popayan");
        segundoTorneo.setEstadoTorneo(EstadoTorneo.INSCRIPCIONES);
        torneo.setUbicacion("Popayan");

        Usuario usuario = new Usuario();
        usuario.setNumeroCelular("3001234567");
        usuario.setUbicacion("Popayan");
        lenient().when(personaDao.existsById(any(Long.class))).thenReturn(false);
        lenient().when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(personaDao.findAll()).thenReturn(Collections.emptyList());
        lenient().when(usuarioDao.findByNumeroCelular("3001234567")).thenReturn(usuario);
        when(torneoDao.findById(11L)).thenReturn(Optional.of(segundoTorneo));
        when(equipoDao.findById(77L)).thenReturn(Optional.of(equipoGuardado));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipoGuardado, torneo)).thenReturn(Optional.empty());
        when(participacionEquipoDao.findByEquipoAndTorneo(equipoGuardado, segundoTorneo)).thenReturn(Optional.empty());
        when(participacionEquipoDao.findByTorneo(torneo)).thenReturn(Collections.emptyList());
        when(participacionEquipoDao.findByTorneo(segundoTorneo)).thenReturn(Collections.emptyList());
        when(participacionEquipoDao.save(any(ParticipacionEquipoTorneo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipo primeraSolicitud = new Equipo();
        primeraSolicitud.setId(77L);
        primeraSolicitud.setDelegado(delegado);
        primeraSolicitud.setTorneo(torneo);
        Equipo segundaSolicitud = new Equipo();
        segundaSolicitud.setId(77L);
        segundaSolicitud.setDelegado(delegado);
        segundaSolicitud.setTorneo(segundoTorneo);

        Equipo primera = equipoService.saveSolicitud(primeraSolicitud);
        Equipo segunda = equipoService.saveSolicitud(segundaSolicitud);

        assertEquals(77L, primera.getId());
        assertEquals(77L, segunda.getId());
        assertEquals(11L, segunda.getParticipacionTorneo().getTorneo().getId());
        ArgumentCaptor<ParticipacionEquipoTorneo> participaciones = ArgumentCaptor.forClass(ParticipacionEquipoTorneo.class);
        verify(participacionEquipoDao, times(2)).save(participaciones.capture());
        assertEquals(List.of(10L, 11L), participaciones.getAllValues().stream()
            .map(participacion -> participacion.getTorneo().getId())
            .toList());
        verify(equipoDao, never()).save(any(Equipo.class));
    }

    @Test
    void saveSolicitud_debeRechazarDuplicadaParaMismoDelegadoYtorneo() {
        Equipo solicitud = new Equipo();
        solicitud.setNombre("Equipo Azul");
        solicitud.setId(101L);
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);

        lenient().when(personaDao.existsById(any(Long.class))).thenReturn(false);
        lenient().when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(personaDao.findAll()).thenReturn(Collections.emptyList());
        when(equipoDao.findById(101L)).thenReturn(Optional.of(solicitud));
        when(participacionEquipoDao.findByEquipoAndTorneo(solicitud, torneo))
            .thenReturn(Optional.of(new ParticipacionEquipoTorneo()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> equipoService.saveSolicitud(solicitud));

        assertTrue(ex.getMessage().contains("solicitud"));
    }

    @Test
    void aceptarSolicitud_debeActualizarLaParticipacionSinModificarElEquipo() {
        Equipo equipo = new Equipo();
        equipo.setId(25L);
        equipo.setNombre("Equipo Verde");
        equipo.setDelegado(delegado);
        ParticipacionEquipoTorneo solicitud = new ParticipacionEquipoTorneo();
        solicitud.setId(35L);
        solicitud.setEquipo(equipo);
        solicitud.setTorneo(torneo);
        solicitud.setEstado("PENDIENTE");

        when(participacionEquipoDao.findById(35L)).thenReturn(Optional.of(solicitud));
        when(participacionEquipoDao.countByTorneoAndEstado(torneo, "ACEPTADO")).thenReturn(0L);
        when(participacionEquipoDao.save(any(ParticipacionEquipoTorneo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Usuario usuarioDelegado = new Usuario();
        usuarioDelegado.setNumeroCelular(delegado.getNumeroCelular());
        when(usuarioDao.findByNumeroCelular(delegado.getNumeroCelular())).thenReturn(usuarioDelegado);
        when(notificacionDao.save(any(NotificacionUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipo resultado = equipoService.aceptarSolicitud(35L);

        assertEquals(25L, resultado.getId());
        assertEquals("ACEPTADO", resultado.getParticipacionTorneo().getEstado());
        assertEquals(torneo.getFaseTorneo(), resultado.getParticipacionTorneo().getFaseActual());
        assertEquals(torneo.getId(), resultado.getTorneo().getId());
        verify(notificacionDao).save(any(NotificacionUsuario.class));
        verify(jugadorDao, never()).findByEquiposContaining(equipo);
        verify(equipoDao, never()).save(any(Equipo.class));
    }

    @Test
    void agregarJugadorATorneo_debeRechazarSiPerteneceAOtroEquipoEnEseTorneo() {
        Equipo equipo = new Equipo();
        equipo.setId(25L);
        equipo.setDelegado(delegado);
        Equipo otroEquipo = new Equipo();
        otroEquipo.setId(26L);
        Jugador jugador = new Jugador();
        jugador.setId(90L);
        ParticipacionJugadorTorneo participacionJugador = new ParticipacionJugadorTorneo();
        participacionJugador.setEquipo(otroEquipo);
        participacionJugador.setTorneo(torneo);
        ParticipacionEquipoTorneo solicitud = new ParticipacionEquipoTorneo();
        solicitud.setId(35L);
        solicitud.setEquipo(equipo);
        solicitud.setTorneo(torneo);
        solicitud.setEstado("ACEPTADO");
        jugador.getEquipos().add(equipo);

        Usuario delegadoUsuario = new Usuario();
        delegadoUsuario.setId(5L);
        delegadoUsuario.setTipoUsuario(TipoUsuario.DELEGADO);
        delegadoUsuario.setNumeroCelular(delegado.getNumeroCelular());
        when(equipoDao.findById(25L)).thenReturn(Optional.of(equipo));
        when(usuarioDao.findById(5L)).thenReturn(Optional.of(delegadoUsuario));
        when(torneoDao.findById(10L)).thenReturn(Optional.of(torneo));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)).thenReturn(Optional.of(solicitud));
        when(jugadorDao.findById(90L)).thenReturn(Optional.of(jugador));
        when(participacionDao.findByTorneoAndJugador(torneo, jugador)).thenReturn(Optional.of(participacionJugador));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> equipoService.agregarJugadorATorneo(25L, 10L, 90L, 5L));

        assertTrue(error.getMessage().contains("otro equipo"));
        verify(participacionDao, never()).save(any(ParticipacionJugadorTorneo.class));
    }

    @Test
    void saveSolicitud_debeRechazarCuandoUbicacionDelUsuarioNoCoincide() {
        Equipo solicitud = new Equipo();
        solicitud.setNombre("Equipo Morado");
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);
        torneo.setUbicacion("Popayan");

        Usuario usuario = new Usuario();
        usuario.setNumeroCelular("3001234567");
        usuario.setUbicacion("Pasto");

        when(usuarioDao.findByNumeroCelular("3001234567")).thenReturn(usuario);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> equipoService.saveSolicitud(solicitud));

        assertTrue(ex.getMessage().contains("ubicación"));
    }

    @Test
    void cambiarEquipoJugador_debeRechazarEquipoDestinoNoAceptadoEnElTorneo() {
        Usuario organizador = new Usuario();
        organizador.setId(9L);
        organizador.setTipoUsuario(TipoUsuario.ORGANIZADOR);
        torneo.setEncargadoTorneo(organizador);
        Equipo equipoNuevo = new Equipo();
        equipoNuevo.setId(26L);
        Jugador jugador = new Jugador();
        jugador.setId(90L);
        ParticipacionJugadorTorneo participacionJugador = new ParticipacionJugadorTorneo();
        participacionJugador.setEquipo(new Equipo());

        when(usuarioDao.findById(9L)).thenReturn(Optional.of(organizador));
        when(jugadorDao.findById(90L)).thenReturn(Optional.of(jugador));
        when(equipoDao.findById(26L)).thenReturn(Optional.of(equipoNuevo));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipoNuevo, torneo)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> equipoService.cambiarEquipoJugador(10L, 90L, 26L, 9L));

        assertTrue(error.getMessage().contains("no participa"));
        verify(participacionDao, never()).save(any(ParticipacionJugadorTorneo.class));
    }
}
