package com.example.torneos.services;

import com.example.torneos.DTO.DtoPago;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.PagoDao;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.*;
import com.example.torneos.enums.TipoPago;
import com.example.torneos.enums.TipoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {
    @Autowired private PagoDao pagoDao;
    @Autowired private TorneoDao torneoDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionJugadorDao;
    @Autowired private NotificacionUsuarioDao notificacionDao;

    @Transactional(readOnly = true)
    public List<DtoPago> listar(long idTorneo, long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("Debes iniciar sesión para consultar pagos"));
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        boolean propietario = torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() == usuario.getId();
        boolean delegado = equipoDelegadoEnTorneo(idTorneo, usuario);
        if (!propietario && !delegado) throw new IllegalArgumentException("No tienes permiso para consultar los pagos de este torneo");
        return pagoDao.listarDtoPorTorneo(idTorneo);
    }

    @Transactional
    public DtoPago registrar(long idTorneo, Pago pago, long usuarioAutenticadoId) {
        if (pago == null || pago.getValor() <= 0) {
            throw new IllegalArgumentException("El pago y su monto son obligatorios");
        }
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Usuario organizador = usuarioDao.findById(usuarioAutenticadoId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (organizador.getTipoUsuario() != TipoUsuario.ORGANIZADOR || torneo.getEncargadoTorneo() == null ||
                torneo.getEncargadoTorneo().getId() != organizador.getId()) {
            throw new IllegalArgumentException("Solo el organizador del torneo puede registrar pagos");
        }
        if ((pago.getEquipo() == null) == (pago.getJugador() == null)) {
            throw new IllegalArgumentException("El pago debe corresponder a un equipo o a un jugador");
        }
        if (pago.getTipo() == null) throw new IllegalArgumentException("Selecciona el tipo de pago");
        if (pago.getEquipo() != null) {
            Equipo equipo = pago.getEquipo();
            ParticipacionEquipoTorneo participacion = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                    .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este torneo"));
            if (!"ACEPTADO".equals(participacion.getEstado())) throw new IllegalArgumentException("El equipo no está aceptado en el torneo");
            pago.setEquipo(equipo);
        } else {
            Jugador jugador = pago.getJugador();
            participacionJugadorDao.findByTorneoAndJugador(torneo, jugador)
                    .orElseThrow(() -> new IllegalArgumentException("El jugador no participa en este torneo"));
            pago.setJugador(jugador);
        }
        pago.setTorneo(torneo);
        pago.setRegistradoPor(organizador);
        if (pago.getFecha() == null) pago.setFecha(LocalDate.now());
        Pago guardado = pagoDao.save(pago);
        notificarDestinatario(guardado);
        return pagoDao.buscarDto(guardado.getId());
    }

    private boolean equipoDelegadoEnTorneo(long torneoId, Usuario usuario) {
        if (usuario.getNumeroCelular() == null) return false;
        Torneo torneo = torneoDao.findById(torneoId).orElse(null);
        if (torneo == null) return false;
        return participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
            .anyMatch(participacion -> participacion.getEquipo().getDelegado() != null &&
                usuario.getNumeroCelular().equals(participacion.getEquipo().getDelegado().getNumeroCelular()));
    }

    private void notificarDestinatario(Pago pago) {
        Usuario destinatario = pago.getJugador() != null
                ? usuarioDao.findByIdentificacion(pago.getJugador().getIdentificacion())
                : pago.getEquipo().getDelegado() == null ? null
                : usuarioDao.findByNumeroCelular(pago.getEquipo().getDelegado().getNumeroCelular());
        if (destinatario == null) return;
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(destinatario);
        notificacion.setTitulo("Pago registrado");
        notificacion.setMensaje("Se registró " + pago.getTipo() + " por $" + pago.getValor() + " el " + pago.getFecha() +
                (pago.getConcepto() == null ? "." : ": " + pago.getConcepto()));
        notificacion.setRuta(pago.getEquipo() != null
            ? "/auth/torneos/torneo/" + pago.getTorneo().getId() + "/equipo/" + pago.getEquipo().getId()
            : "/auth/usuario");
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionDao.save(notificacion);
    }
}