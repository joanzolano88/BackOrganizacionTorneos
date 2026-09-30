package com.example.torneos.services;

import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.SancionJugadorTorneoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.*;
import com.example.torneos.enums.EstadoJugador;
import com.example.torneos.enums.TipoEventoPartido;
import com.example.torneos.enums.TipoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SancionJugadorTorneoService {
    @Autowired private SancionJugadorTorneoDao sancionDao;
    @Autowired private EventoPartidoDao eventoDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired private PartidoDao partidoDao;
    @Autowired private TorneoDao torneoDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private NotificacionUsuarioDao notificacionDao;

    public void validarElegibilidad(Partido partido, Jugador jugador) {
        ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugador)
                .orElseThrow(() -> new IllegalArgumentException("El jugador debe estar inscrito en el torneo antes de ser convocado"));
        if (!mismoEquipo(participacion.getEquipo(), partido.getEquipoLocal()) &&
                !mismoEquipo(participacion.getEquipo(), partido.getEquipoVisitante())) {
            throw new IllegalArgumentException("El jugador no está inscrito con uno de los equipos de este partido");
        }
        if (jugador.getEstadoJugador() != null && jugador.getEstadoJugador() != EstadoJugador.ACTIVO) {
            throw new IllegalArgumentException("El jugador no está activo");
        }
        SancionJugadorTorneo sancion = sancionDao.findByTorneoAndJugadorAndActivaTrue(partido.getTorneo(), jugador).orElse(null);
        if (sancion != null && !Long.valueOf(partido.getId()).equals(sancion.getExencionPartidoId()) &&
                (sancion.isPermanente() || sancion.getPartidosPendientes() > 0)) {
            throw new IllegalArgumentException(sancion.isPermanente()
                    ? "El jugador está suspendido por el resto del torneo"
                    : "El jugador tiene " + sancion.getPartidosPendientes() + " partido(s) de suspensión pendientes");
        }
        if (eventoDao.existsByPartidoAndJugadorAndTipo(partido, jugador, TipoEventoPartido.TARJETA_ROJA)) {
            throw new IllegalArgumentException("Un jugador expulsado no puede volver a jugar este partido");
        }
    }

    @Transactional
    public void registrarTarjeta(EventoPartido evento) {
        Torneo torneo = evento.getPartido().getTorneo();
        if (evento.getTipo() == TipoEventoPartido.TARJETA_AMARILLA) {
            int umbral = torneo.getAmarillasParaSuspension();
            if (umbral <= 0) return;
            long total = eventoDao.findAll().stream()
                    .filter(item -> item.getJugador().getId() == evento.getJugador().getId())
                    .filter(item -> item.getPartido().getTorneo().getId().equals(torneo.getId()))
                    .filter(item -> item.getTipo() == TipoEventoPartido.TARJETA_AMARILLA).count();
            if (total == 0 || total % umbral != 0) return;
                aplicar(evento, "ACUMULACION_AMARILLAS", Math.max(1, torneo.getPartidosSuspensionRoja()), torneo.isExpulsionPermanenteTorneo());
        } else if (evento.getTipo() == TipoEventoPartido.TARJETA_ROJA) {
                aplicar(evento, "TARJETA_ROJA", torneo.isExpulsionPermanenteTorneo() ? 0 : Math.max(1, torneo.getPartidosSuspensionRoja()),
                    torneo.isExpulsionPermanenteTorneo());
        }
    }

    private void aplicar(EventoPartido evento, String motivo, int partidos, boolean permanente) {
        Partido partido = evento.getPartido();
        Jugador jugador = evento.getJugador();
        ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugador)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no está inscrito en el torneo"));
        SancionJugadorTorneo sancion = sancionDao.findByTorneoAndJugadorAndActivaTrue(partido.getTorneo(), jugador)
                .orElseGet(SancionJugadorTorneo::new);
        boolean yaAplicadaEnEstePartido = Long.valueOf(partido.getId()).equals(sancion.getPartidoOrigenId());
        sancion.setTorneo(partido.getTorneo());
        sancion.setJugador(jugador);
        sancion.setEquipo(participacion.getEquipo());
        sancion.setMotivo(motivo);
        sancion.setPermanente(permanente || sancion.isPermanente());
        sancion.setPartidosPendientes(sancion.isPermanente() ? 0 : sancion.getPartidosPendientes() + (yaAplicadaEnEstePartido ? 0 : partidos));
        sancion.setActiva(true);
        sancion.setPartidoOrigenId(partido.getId());
        sancion.setCreadaEn(LocalDateTime.now());
        sancionDao.save(sancion);
        String mensaje = jugador.getNombre() + " recibió una sanción en " + partido.getTorneo().getNombre() +
            (sancion.isPermanente() ? " por el resto del torneo." : ". Partidos pendientes: " + sancion.getPartidosPendientes() + ".");
        notificarDelegados(partido, mensaje);
        notificarJugador(jugador, "Sanción registrada", mensaje, partido.getTorneo(), participacion.getEquipo());
    }

    @Transactional
    public void cumplirSanciones(Partido partido) {
        List<SancionJugadorTorneo> sanciones = sancionDao.findByTorneoAndEquipoAndActivaTrue(partido.getTorneo(), partido.getEquipoLocal());
        sanciones.addAll(sancionDao.findByTorneoAndEquipoAndActivaTrue(partido.getTorneo(), partido.getEquipoVisitante()));
        for (SancionJugadorTorneo sancion : sanciones) {
            if (Long.valueOf(partido.getId()).equals(sancion.getPartidoOrigenId())) continue;
            if (Long.valueOf(partido.getId()).equals(sancion.getExencionPartidoId())) {
                sancion.setExencionPartidoId(null);
            } else if (!sancion.isPermanente() && sancion.getPartidosPendientes() > 0) {
                sancion.setPartidosPendientes(sancion.getPartidosPendientes() - 1);
                if (sancion.getPartidosPendientes() == 0) sancion.setActiva(false);
            }
            sancionDao.save(sancion);
        }
    }

    @Transactional
    public SancionJugadorTorneo modificarEnPartido(long idPartido, long idJugador, long idUsuario, boolean levantar) {
        Partido partido = partidoDao.findById(idPartido).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (usuario.getTipoUsuario() != TipoUsuario.ORGANIZADOR || partido.getTorneo().getEncargadoTorneo() == null ||
                partido.getTorneo().getEncargadoTorneo().getId() != usuario.getId()) {
            throw new IllegalArgumentException("Solo el organizador del torneo puede modificar la sanción");
        }
        if (partido.getFechaPartido() == null || !partido.getFechaPartido().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalArgumentException("La sanción solo se puede modificar el día del partido");
        }
        Jugador jugador = participacionDao.findByTorneo(partido.getTorneo()).stream().map(ParticipacionJugadorTorneo::getJugador)
                .filter(item -> item.getId() == idJugador).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El jugador no participa en este torneo"));
        SancionJugadorTorneo sancion = sancionDao.findByTorneoAndJugadorAndActivaTrue(partido.getTorneo(), jugador)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no tiene una sanción activa"));
        if (levantar) sancion.setExencionPartidoId(idPartido);
        else if (Long.valueOf(idPartido).equals(sancion.getExencionPartidoId())) sancion.setExencionPartidoId(null);
        else throw new IllegalArgumentException("No hay una exención que restaurar para este partido");
        SancionJugadorTorneo guardada = sancionDao.save(sancion);
        notificarJugador(jugador, levantar ? "Sanción levantada para este partido" : "Sanción restaurada",
            levantar ? "El organizador autorizó tu participación en el partido de hoy." : "El organizador restauró tu sanción para el partido de hoy.",
            partido.getTorneo(), sancion.getEquipo());
        return guardada;
    }

    public List<SancionJugadorTorneo> listarTorneo(Torneo torneo) {
        return sancionDao.findAll().stream().filter(item -> item.getTorneo().getId().equals(torneo.getId())).toList();
    }

    public List<SancionJugadorTorneo> listarTorneo(long torneoId) {
        Torneo torneo = torneoDao.findById(torneoId).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        return listarTorneo(torneo);
    }

    private void notificarDelegados(Partido partido, String mensaje) {
        for (Equipo equipo : Stream.of(partido.getEquipoLocal(), partido.getEquipoVisitante()).filter(java.util.Objects::nonNull).toList()) {
            if (equipo.getDelegado() == null || equipo.getDelegado().getNumeroCelular() == null) continue;
            Usuario delegado = usuarioDao.findByNumeroCelular(equipo.getDelegado().getNumeroCelular());
            if (delegado == null) continue;
            NotificacionUsuario notificacion = new NotificacionUsuario();
            notificacion.setUsuario(delegado);
            notificacion.setTitulo("Jugador suspendido");
            notificacion.setMensaje(mensaje);
            notificacion.setRuta("/auth/partidos/partido/" + partido.getId());
            notificacion.setCreadaEn(LocalDateTime.now());
            notificacion.setLeida(false);
            notificacionDao.save(notificacion);
        }
    }

    private void notificarJugador(Jugador jugador, String titulo, String mensaje, Torneo torneo, Equipo equipo) {
        if (jugador.getCedula() == null) return;
        Usuario usuario = usuarioDao.findByCedula(jugador.getCedula());
        if (usuario == null) return;
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(usuario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setRuta("/auth/torneos/torneo/" + torneo.getId() + "/equipo/" + equipo.getId());
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionDao.save(notificacion);
    }

    private boolean mismoEquipo(Equipo uno, Equipo dos) {
        return uno != null && dos != null && uno.getId() == dos.getId();
    }
}