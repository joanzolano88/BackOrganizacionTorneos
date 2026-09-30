package com.example.torneos.services;

import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.SancionJugadorTorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.SancionJugadorTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoJugador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SancionJugadorTorneoServiceTest {
    @Mock private SancionJugadorTorneoDao sancionDao;
    @Mock private EventoPartidoDao eventoDao;
    @Mock private ParticipacionJugadorTorneoDao participacionDao;

    @InjectMocks
    private SancionJugadorTorneoService service;

    @Test
    void validarElegibilidad_rechazaJugadorConSuspensionActiva() {
        Torneo torneo = new Torneo();
        torneo.setId(4L);
        Equipo equipoLocal = new Equipo();
        equipoLocal.setId(7L);
        Equipo equipoVisitante = new Equipo();
        equipoVisitante.setId(8L);
        Partido partido = new Partido();
        partido.setId(9L);
        partido.setTorneo(torneo);
        partido.setEquipoLocal(equipoLocal);
        partido.setEquipoVisitante(equipoVisitante);
        Jugador jugador = new Jugador();
        jugador.setId(10L);
        jugador.setEstadoJugador(EstadoJugador.ACTIVO);
        ParticipacionJugadorTorneo participacion = new ParticipacionJugadorTorneo();
        participacion.setEquipo(equipoLocal);
        SancionJugadorTorneo sancion = new SancionJugadorTorneo();
        sancion.setActiva(true);
        sancion.setPartidosPendientes(1);

        when(participacionDao.findByTorneoAndJugador(torneo, jugador)).thenReturn(Optional.of(participacion));
        when(sancionDao.findByTorneoAndJugadorAndActivaTrue(torneo, jugador)).thenReturn(Optional.of(sancion));

        assertThrows(IllegalArgumentException.class, () -> service.validarElegibilidad(partido, jugador));
    }
}