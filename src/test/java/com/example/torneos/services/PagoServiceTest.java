package com.example.torneos.services;

import com.example.torneos.DTO.DtoPago;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.PagoDao;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.Pago;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.TipoPago;
import com.example.torneos.enums.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
    @Mock private PagoDao pagoDao;
    @Mock private TorneoDao torneoDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Mock private ParticipacionJugadorTorneoDao participacionJugadorDao;
    @Mock private NotificacionUsuarioDao notificacionDao;

    @InjectMocks
    private PagoService service;

    @Test
    void registrar_guardaPagoYNotificaAlDelegadoDelEquipo() {
        Usuario organizador = new Usuario();
        organizador.setId(2L);
        organizador.setTipoUsuario(TipoUsuario.ORGANIZADOR);
        Usuario delegadoUsuario = new Usuario();
        delegadoUsuario.setNumeroCelular("3001234567");
        Persona delegado = new Persona();
        delegado.setNumeroCelular("3001234567");
        Equipo equipo = new Equipo();
        equipo.setId(3L);
        equipo.setNombre("Equipo Prueba");
        equipo.setDelegado(delegado);
        Torneo torneo = new Torneo();
        torneo.setId(4L);
        torneo.setNombre("Torneo Prueba");
        torneo.setEncargadoTorneo(organizador);
        ParticipacionEquipoTorneo participacion = new ParticipacionEquipoTorneo();
        participacion.setEstado("ACEPTADO");
        when(torneoDao.findById(4L)).thenReturn(Optional.of(torneo));
        when(usuarioDao.findById(2L)).thenReturn(Optional.of(organizador));
        when(participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)).thenReturn(Optional.of(participacion));
        when(pagoDao.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pagoDao.buscarDto(0L)).thenReturn(new DtoPago(0L, 25000, TipoPago.ARBITRAJE, null, "Fecha 1", null,
            4L, "Torneo Prueba", 3L, "Equipo Prueba", "3001234567", null, null, null));
        when(usuarioDao.findByNumeroCelular("3001234567")).thenReturn(delegadoUsuario);
        when(notificacionDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Pago pago = new Pago();
        pago.setValor(25000);
        pago.setTipo(TipoPago.ARBITRAJE);
        pago.setEquipo(equipo);
        pago.setUsuarioId(2L);
        pago.setConcepto("Fecha 1");

        DtoPago guardado = service.registrar(4L, pago);

        assertEquals(4L, guardado.getTorneoId());
        assertEquals(3L, guardado.getEquipoId());
        assertEquals(TipoPago.ARBITRAJE, guardado.getTipo());
        verify(notificacionDao).save(any());
    }
}