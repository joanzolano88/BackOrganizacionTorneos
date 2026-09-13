package com.example.torneos.services;

import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.FaseActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    }

    @Test
    void saveSolicitud_debeCrearSolicitudSinAsignarFase() {
        Equipo solicitud = new Equipo();
        solicitud.setNombre("Equipo Rojo");
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);
        torneo.setUbicacion("Popayan");

        Usuario usuario = new Usuario();
        usuario.setNumeroCelular("3001234567");
        usuario.setUbicacion("Popayan");

        lenient().when(personaDao.existsById(any(Long.class))).thenReturn(false);
        lenient().when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(personaDao.findAll()).thenReturn(Collections.emptyList());
        when(usuarioDao.findByNumeroCelular("3001234567")).thenReturn(usuario);
        when(equipoDao.findByDelegadoAndTorneoAndFaseActualIsNull(any(Persona.class), any(Torneo.class)))
                .thenReturn(Optional.empty());
        when(equipoDao.save(any(Equipo.class))).thenAnswer(invocation -> {
            Equipo guardado = invocation.getArgument(0);
            guardado.setId(100L);
            return guardado;
        });

        Equipo resultado = equipoService.saveSolicitud(solicitud);

        assertNotNull(resultado);
        assertNull(resultado.getFaseActual());
        assertEquals(0, resultado.getGrupo());
        assertEquals(10L, resultado.getTorneo().getId());
    }

    @Test
    void saveSolicitud_debeRechazarDuplicadaParaMismoDelegadoYtorneo() {
        Equipo solicitud = new Equipo();
        solicitud.setNombre("Equipo Azul");
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);

        lenient().when(personaDao.existsById(any(Long.class))).thenReturn(false);
        lenient().when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(personaDao.findAll()).thenReturn(Collections.emptyList());
        when(equipoDao.findByDelegadoAndTorneoAndFaseActualIsNull(any(Persona.class), any(Torneo.class)))
                .thenReturn(Optional.of(solicitud));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> equipoService.saveSolicitud(solicitud));

        assertTrue(ex.getMessage().contains("solicitud"));
    }

    @Test
    void aceptarSolicitud_debeAsignarEquipoAlTorneo() {
        Equipo solicitud = new Equipo();
        solicitud.setId(25L);
        solicitud.setNombre("Equipo Verde");
        solicitud.setDelegado(delegado);
        solicitud.setTorneo(torneo);
        solicitud.setFaseActual(null);

        when(equipoDao.findById(25L)).thenReturn(Optional.of(solicitud));
        lenient().when(equipoDao.countByTorneoAndFaseActual(torneo, torneo.getFaseTorneo())).thenReturn(0);
        when(equipoDao.save(any(Equipo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipo resultado = equipoService.aceptarSolicitud(25L);

        assertEquals(torneo.getFaseTorneo(), resultado.getFaseActual());
        assertEquals(0, resultado.getGrupo());
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
}
