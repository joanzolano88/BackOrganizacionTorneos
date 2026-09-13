package com.example.torneos.services;

import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.InvitacionPartidoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.InvitacionPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.TipoUsuario;
import com.example.torneos.enums.TipoEventoPartido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ConvocatoriaPartidoService {
    @Autowired private PartidoDao partidoDao;
    @Autowired private JugadorDao jugadorDao;
    @Autowired private ConvocatoriaPartidoDao convocatoriaDao;
    @Autowired private InvitacionPartidoDao invitacionDao;
    @Autowired private EquipoDao equipoDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private TorneoDao torneoDao;
    @Autowired private EventoPartidoDao eventoDao;

    public List<ConvocatoriaPartido> listar(long partidoId) {
        return convocatoriaDao.findByPartido(partido(partidoId));
    }

    public List<Jugador> jugadoresDelEquipo(long equipoId) {
        return jugadorDao.findByEquipo(equipo(equipoId));
    }

    public Jugador buscarPorCedula(long partidoId, String cedula, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        Jugador jugador = jugadorDao.findByCedula(cedula.trim())
            .orElseThrow(() -> new IllegalArgumentException("No se encontró un jugador con esa cédula"));
        validarEquipoDelDelegado(partido, jugador.getEquipo(), usuario);
        return jugador;
    }

    @Transactional
    public ConvocatoriaPartido agregar(long partidoId, long jugadorId, boolean titular, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        Jugador jugador = jugadorDao.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        Equipo equipo = equipoDeJugadorEnPartido(partido, jugador);
        validarEquipoDelDelegado(partido, equipo, usuario);
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent()) {
            throw new IllegalArgumentException("El jugador ya está agregado a este partido");
        }
        int maxConvocados = maximoConvocados(partido);
        if (convocatoriaDao.countByPartidoAndJugadorEquipo(partido, equipo) >= maxConvocados) {
            throw new IllegalArgumentException("Se alcanzó el máximo de jugadores convocados para " + equipo.getNombre());
        }
        if (titular) {
            validarMaxTitulares(partido, equipo);
        }
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        convocatoria.setTitular(titular);
        return convocatoriaDao.save(convocatoria);
    }

    @Transactional
    public ConvocatoriaPartido agregarPorCedula(long partidoId, String identificacion, boolean titular, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        Jugador jugador = jugadorDao.findByCedula(identificacion.trim()).orElse(null);
        if (jugador == null) {
            Equipo equipo = equipoDelGestor(partido, usuario);
            if (equipo.getDelegado() == null || !identificacion.trim().equals(equipo.getDelegado().getCedula())) {
                throw new IllegalArgumentException("No se encontró un jugador con esa cédula");
            }
            jugador = new Jugador();
            jugador.setCedula(equipo.getDelegado().getCedula());
            jugador.setNombre(equipo.getDelegado().getNombre());
            jugador.setNumeroCelular(equipo.getDelegado().getNumeroCelular());
            jugador.setEquipo(equipo);
            jugador = jugadorDao.save(jugador);
        }
        return agregar(partidoId, jugador.getId(), titular, usuarioId);
    }

    @Transactional
    public ConvocatoriaPartido cambiarTitular(long convocatoriaId, boolean titular, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        Usuario usuario = usuario(usuarioId);
        validarGestor(convocatoria.getPartido(), usuario);
        validarEstadoEditable(convocatoria.getPartido());
        if (titular && !convocatoria.isTitular()) {
            validarMaxTitulares(convocatoria.getPartido(), convocatoria.getJugador().getEquipo());
        }
        convocatoria.setTitular(titular);
        registrarEvento(convocatoria.getPartido(), convocatoria.getJugador(), titular ? TipoEventoPartido.ENTRA_TITULAR : TipoEventoPartido.SALE_TITULAR);
        return convocatoriaDao.save(convocatoria);
    }

    @Transactional
    public ConvocatoriaPartido cambiarNumeroUniforme(long convocatoriaId, int numero, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        validarGestor(convocatoria.getPartido(), usuario(usuarioId));
        validarEstadoEditable(convocatoria.getPartido());
        if (numero < 0 || numero > 99) {
            throw new IllegalArgumentException("El número de uniforme debe estar entre 0 y 99");
        }
        convocatoria.setNumeroUniforme(numero);
        return convocatoriaDao.save(convocatoria);
    }

    public List<com.example.torneos.entities.EventoPartido> eventos(long partidoId) {
        return eventoDao.findByPartidoOrderByMinutoAscIdAsc(partido(partidoId));
    }

    @Transactional
    public com.example.torneos.entities.EventoPartido registrarEvento(long partidoId, long jugadorId, TipoEventoPartido tipo, int minuto, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestorOrganizador(partido, usuario(usuarioId));
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Los goles y tarjetas solo se pueden registrar durante el partido");
        }
        Jugador jugador = jugadorDao.findById(jugadorId).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isEmpty()) throw new IllegalArgumentException("El jugador no está convocado en este partido");
        if (tipo != TipoEventoPartido.GOL && tipo != TipoEventoPartido.TARJETA_AMARILLA && tipo != TipoEventoPartido.TARJETA_ROJA) throw new IllegalArgumentException("El evento no es válido");
        return guardarEvento(partido, jugador, tipo, minuto);
    }

    private void registrarEvento(Partido partido, Jugador jugador, TipoEventoPartido tipo) {
        if (partido.getEstadoPartido() == EstadoPartido.EN_PROCESO) guardarEvento(partido, jugador, tipo, 0);
    }

    private com.example.torneos.entities.EventoPartido guardarEvento(Partido partido, Jugador jugador, TipoEventoPartido tipo, int minuto) {
        com.example.torneos.entities.EventoPartido evento = new com.example.torneos.entities.EventoPartido();
        evento.setPartido(partido); evento.setJugador(jugador); evento.setTipo(tipo); evento.setMinuto(Math.max(minuto, 0)); evento.setRegistradoEn(LocalDateTime.now());
        return eventoDao.save(evento);
    }

    @Transactional
    public void eliminar(long convocatoriaId, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        validarGestor(convocatoria.getPartido(), usuario(usuarioId));
        validarEstadoEditable(convocatoria.getPartido());
        convocatoriaDao.delete(convocatoria);
    }

    public void validarTitulares(long partidoId, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestor(partido, usuario(usuarioId));
        for (Equipo equipo : equiposDelPartido(partido)) {
            long titulares = convocatoriaDao.countByPartidoAndJugadorEquipoAndTitularTrue(partido, equipo);
            int minimo = minimoTitulares(partido);
            int maximo = maximoTitulares(partido);
            if (titulares < minimo || titulares > maximo) {
                throw new IllegalArgumentException("La lista de titulares de " + equipo.getNombre() + " debe tener entre " + minimo + " y " + maximo + " jugadores");
            }
        }
    }

    @Transactional
    public InvitacionPartido crearInvitacion(long partidoId, long equipoId, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        Equipo equipo = equipo(equipoId);
        if (equipo.getId() != partido.getEquipoLocal().getId() && equipo.getId() != partido.getEquipoVisitante().getId()) {
            throw new IllegalArgumentException("El equipo no participa en este partido");
        }
        validarEquipoDelDelegado(partido, equipo, usuario);
        InvitacionPartido invitacion = new InvitacionPartido();
        invitacion.setPartido(partido);
        invitacion.setEquipo(equipo);
        invitacion.setToken(UUID.randomUUID().toString());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusDays(2));
        return invitacionDao.save(invitacion);
    }

    @Transactional
    public ConvocatoriaPartido aceptarInvitacion(String token, long usuarioId) {
        InvitacionPartido invitacion = invitacionDao.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de invitación no es válido"));
        if (invitacion.isUsada() || invitacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de invitación expiró o ya fue utilizado");
        }
        Usuario usuario = usuario(usuarioId);
        if (usuario.getTipoUsuario() != TipoUsuario.JUGADOR) {
            throw new IllegalArgumentException("Solo un usuario de tipo jugador puede aceptar esta invitación");
        }
        if (usuario.getCedula() == null || usuario.getCedula().isBlank()) {
            throw new IllegalArgumentException("El usuario jugador debe tener cédula registrada");
        }
        Jugador jugador = jugadorDao.findByCedula(usuario.getCedula()).orElseGet(() -> {
            Jugador nuevo = new Jugador();
            nuevo.setCedula(usuario.getCedula());
            nuevo.setNombre(usuario.getNombre());
            nuevo.setNumeroCelular(usuario.getNumeroCelular());
            nuevo.setCorreoElectronico(usuario.getCorreoElectronico());
            nuevo.setEquipo(invitacion.getEquipo());
            return jugadorDao.save(nuevo);
        });
        if (jugador.getEquipo() != invitacion.getEquipo()) {
            throw new IllegalArgumentException("El jugador no pertenece al equipo invitado");
        }
        invitacion.setUsada(true);
        invitacionDao.save(invitacion);
        return agregarInterno(invitacion.getPartido(), jugador, false);
    }

    private ConvocatoriaPartido agregarInterno(Partido partido, Jugador jugador, boolean titular) {
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent()) {
            throw new IllegalArgumentException("El jugador ya está agregado a este partido");
        }
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        convocatoria.setTitular(titular);
        return convocatoriaDao.save(convocatoria);
    }

    private void validarMaxTitulares(Partido partido, Equipo equipo) {
        int maximo = maximoTitulares(partido);
        if (convocatoriaDao.countByPartidoAndJugadorEquipoAndTitularTrue(partido, equipo) >= maximo) {
            throw new IllegalArgumentException("No se pueden registrar más de " + maximo + " titulares");
        }
    }

    private int maximoConvocados(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 22 : partido.getTorneo().getDeporte().getMaximoConvocados();
    }

    private int minimoTitulares(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 7 : partido.getTorneo().getDeporte().getMinimoTitulares();
    }

    private int maximoTitulares(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 11 : partido.getTorneo().getDeporte().getMaximoTitulares();
    }

    private List<Equipo> equiposDelPartido(Partido partido) {
        return java.util.stream.Stream.of(partido.getEquipoLocal(), partido.getEquipoVisitante())
                .filter(java.util.Objects::nonNull).toList();
    }

    private void validarEstadoEditable(Partido partido) {
        if (partido.getEstadoPartido() != EstadoPartido.PROGRAMADO && partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("La lista solo se puede modificar cuando el partido está programado o en proceso");
        }
    }

    private void validarGestor(Partido partido, Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.ORGANIZADOR && partido.getTorneo().getEncargadoTorneo() != null && partido.getTorneo().getEncargadoTorneo().getId() == usuario.getId()) {
            return;
        }
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoLocal(), usuario) || usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoVisitante(), usuario)) {
            return;
        }
        throw new IllegalArgumentException("Solo el organizador o el delegado del equipo puede modificar la lista");
    }

    private void validarGestorOrganizador(Partido partido, Usuario usuario) {
        if (usuario.getTipoUsuario() != TipoUsuario.ORGANIZADOR || partido.getTorneo().getEncargadoTorneo() == null || partido.getTorneo().getEncargadoTorneo().getId() != usuario.getId()) {
            throw new IllegalArgumentException("Solo el organizador del torneo puede registrar goles y tarjetas");
        }
    }

    private void validarEquipoDelDelegado(Partido partido, Equipo equipo, Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && (equipo == null || equipo.getId() != equipoDelGestor(partido, usuario).getId())) {
            throw new IllegalArgumentException("El delegado solo puede gestionar los jugadores de su propio equipo");
        }
    }

    private Equipo equipoDelGestor(Partido partido, Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoLocal(), usuario)) return partido.getEquipoLocal();
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoVisitante(), usuario)) return partido.getEquipoVisitante();
        return partido.getEquipoLocal();
    }

    private boolean esDelegadoDeEquipo(Equipo equipo, Usuario usuario) {
        return equipo != null && equipo.getDelegado() != null && equipo.getDelegado().getNumeroCelular() != null && equipo.getDelegado().getNumeroCelular().equals(usuario.getNumeroCelular());
    }

    private Equipo equipoDeJugadorEnPartido(Partido partido, Jugador jugador) {
        if (jugador.getEquipo() != null && (jugador.getEquipo().getId() == partido.getEquipoLocal().getId() || jugador.getEquipo().getId() == partido.getEquipoVisitante().getId())) {
            return jugador.getEquipo();
        }
        throw new IllegalArgumentException("El jugador no pertenece a uno de los equipos del partido");
    }

    private Partido partido(long id) { return partidoDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El partido no existe")); }
    private Equipo equipo(long id) { return equipoDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El equipo no existe")); }
    private Usuario usuario(long id) { return usuarioDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El usuario no existe")); }
}
