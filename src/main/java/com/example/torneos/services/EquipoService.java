package com.example.torneos.services;

import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.SolicitudJugadorEquipo;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.dao.SolicitudJugadorEquipoDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.entities.InvitacionEquipo;
import com.example.torneos.dao.InvitacionEquipoDao;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.DTO.DtoInvitacionEquipo;
import com.example.torneos.DTO.DtoRegistroPlanilla;
import com.example.torneos.DTO.DtoResultadoRegistroPlanilla;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.enums.EstadoTorneo;
import com.example.torneos.enums.TipoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EquipoService {
    @Autowired
    private EquipoDao equipoDao;
    @Autowired
    private TorneoDao torneoDao;
    @Autowired
    private PartidoDao partidoDao;
    @Autowired
    private PersonaDao personaDao;
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private JugadorDao jugadorDao;
    @Autowired
    private SolicitudJugadorEquipoDao solicitudJugadorDao;
    @Autowired
    private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired
    private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Autowired
    private NotificacionUsuarioDao notificacionDao;
    @Autowired
    private InvitacionEquipoDao invitacionEquipoDao;
    @Autowired
    private AyudanteTorneoDao ayudanteTorneoDao;

    private boolean permiteGestionSolicitudes(Torneo torneo) {
        return torneo != null && torneo.getEstadoTorneo() != null &&
                (torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES ||
                 torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES_ACTIVO);
    }

    private Persona resolverDelegado(Persona delegado) {
        if (delegado == null) {
            return null;
        }

        if (personaDao == null) {
            return delegado;
        }

        if (delegado.getNumeroCelular() != null && !delegado.getNumeroCelular().isBlank()) {
            Persona existente = personaDao.findAll().stream()
                    .filter(persona -> persona.getNumeroCelular() != null && persona.getNumeroCelular().equals(delegado.getNumeroCelular()))
                    .findFirst()
                    .orElse(null);
            if (existente != null) {
                return existente;
            }
        }

        if (delegado.getId() > 0 && personaDao.existsById(delegado.getId())) {
            return personaDao.findById(delegado.getId()).orElse(delegado);
        }

        Usuario usuario = null;
        if (delegado.getId() > 0) {
            usuario = usuarioDao.findById(delegado.getId()).orElse(null);
        }
        if (usuario == null && delegado.getNumeroCelular() != null && !delegado.getNumeroCelular().isBlank()) {
            usuario = usuarioDao.findByNumeroCelular(delegado.getNumeroCelular());
        }

        if (usuario != null) {
            Persona persona = new Persona();
            persona.setNombre(usuario.getNombre());
            persona.setNumeroCelular(usuario.getNumeroCelular());
            persona.setNumeroTelefono(usuario.getNumeroTelefono());
            persona.setWhatsappActivo(usuario.isWhatsappActivo());
            persona.setCorreoElectronico(usuario.getCorreoElectronico());
            persona.setFoto(usuario.getFoto());
            persona.setIdentificacion(usuario.getIdentificacion());
            Persona guardada = personaDao.save(persona);
            return guardada != null ? guardada : persona;
        }

        Persona guardada = personaDao.save(delegado);
        return guardada != null ? guardada : delegado;
    }

    public Equipo save(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException("El equipo es obligatorio");
        }
        if (equipo.getDelegado() == null) {
            throw new IllegalArgumentException("El delegado es obligatorio");
        }
        if (equipo.getId() != 0) {
            Equipo equipoExistente = equipoDao.findById(equipo.getId()).orElse(null);
            if (equipoExistente == null) {
                throw new IllegalArgumentException("El equipo seleccionado no existe");
            }
            equipo.setNombre(equipoExistente.getNombre());
            equipo.setEscudo(equipoExistente.getEscudo());
            equipo.setBandera(equipoExistente.getBandera());
            equipo.setDelegado(equipoExistente.getDelegado());
        }
        equipo.setDelegado(resolverDelegado(equipo.getDelegado()));
        if (equipo.getNombre() == null || equipo.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }

        equipo.setTorneo(null);
        equipo.setFaseActual(null);
        equipo.setGrupo(0);
        equipo.setParticipacionTorneo(null);
        return equipoDao.save(equipo);
    }

    public Equipo save(Equipo equipo, long usuarioId) {
        Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (usuario.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.DELEGADO &&
                usuario.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.ORGANIZADOR) {
            throw new IllegalArgumentException("Solo un organizador o delegado puede crear un equipo");
        }
        equipo.setDelegado(delegadoDeUsuario(usuario));
        return save(equipo);
    }

    private Persona delegadoDeUsuario(Usuario usuario) {
        Persona delegado = new Persona();
        delegado.setNombre(usuario.getNombre());
        delegado.setNumeroCelular(usuario.getNumeroCelular());
        delegado.setNumeroTelefono(usuario.getNumeroTelefono());
        delegado.setWhatsappActivo(usuario.isWhatsappActivo());
        delegado.setCorreoElectronico(usuario.getCorreoElectronico());
        delegado.setFoto(usuario.getFoto());
        delegado.setIdentificacion(usuario.getIdentificacion());
        return delegado;
    }

    @Transactional
    public Equipo saveSolicitud(Equipo equipo, long usuarioId) {
        if (equipo == null || equipo.getTorneo() == null) {
            throw new IllegalArgumentException("Torneo nulo");
        }
        Usuario solicitante = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (solicitante.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.DELEGADO &&
                solicitante.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.ORGANIZADOR) {
            throw new IllegalArgumentException("Solo un organizador o delegado puede postular un equipo");
        }
        equipo.setDelegado(delegadoDeUsuario(solicitante));
        Torneo torneo = torneoDao.findById(equipo.getTorneo().getId())
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Persona delegadoSolicitante = resolverDelegado(equipo.getDelegado());
        Equipo equipoBase;
        if (equipo.getId() > 0) {
            equipoBase = equipoDao.findById(equipo.getId())
                    .orElseThrow(() -> new IllegalArgumentException("El equipo seleccionado no existe"));
            if (equipoBase.getDelegado() == null || delegadoSolicitante.getNumeroCelular() == null ||
                    !delegadoSolicitante.getNumeroCelular().equals(equipoBase.getDelegado().getNumeroCelular())) {
                throw new IllegalArgumentException("Solo el delegado del equipo puede solicitar su participación");
            }
        } else {
            equipo.setTorneo(null);
            equipo.setDelegado(delegadoSolicitante);
            equipoBase = equipo;
        }
        if (equipoBase.getNombre() == null || equipoBase.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }
        if (participacionEquipoDao.findByEquipoAndTorneo(equipoBase, torneo).isPresent()) {
            throw new IllegalArgumentException("Este equipo ya tiene una solicitud o participa en este torneo");
        }
        String nombreEquipo = equipoBase.getNombre();
        boolean equipoRepetido = participacionEquipoDao.findByTorneo(torneo).stream()
                .anyMatch(participacion -> participacion.getEquipo().getNombre() != null &&
                participacion.getEquipo().getNombre().equalsIgnoreCase(nombreEquipo));
        if (equipoRepetido) {
            throw new IllegalArgumentException("Ya existe un equipo con ese nombre en el torneo");
        }

        if (torneo.getUbicacion() == null || torneo.getUbicacion().isBlank()) {
            throw new IllegalArgumentException("El torneo no tiene ubicación registrada");
        }
        if (delegadoSolicitante.getNumeroCelular() == null || delegadoSolicitante.getNumeroCelular().isBlank()) {
            throw new IllegalArgumentException("El delegado debe tener un número de celular registrado");
        }
        Usuario usuarioDelegado = usuarioDao.findByNumeroCelular(delegadoSolicitante.getNumeroCelular());
        if (usuarioDelegado == null || usuarioDelegado.getUbicacion() == null || usuarioDelegado.getUbicacion().isBlank()) {
            throw new IllegalArgumentException("El delegado debe tener una ubicación registrada para enviar solicitudes");
        }
        if (!usuarioDelegado.getUbicacion().equalsIgnoreCase(torneo.getUbicacion())) {
            throw new IllegalArgumentException("Solo puedes enviar solicitudes en la misma ubicación del usuario");
        }
        if (torneo.getEstadoTorneo() == null ||
            (!torneo.getEstadoTorneo().name().equals("INSCRIPCIONES") &&
             !torneo.getEstadoTorneo().name().equals("INSCRIPCIONES_ACTIVO") &&
             !torneo.getEstadoTorneo().name().equals("INSCRIPCIONES_ACRIVO"))) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden enviar en estado de inscripciones");
        }

        if (equipo.getId() == 0) {
            equipoBase = save(equipoBase);
        }

        ParticipacionEquipoTorneo solicitud = new ParticipacionEquipoTorneo();
        solicitud.setEquipo(equipoBase);
        solicitud.setTorneo(torneo);
        solicitud.setEstado("PENDIENTE");
        solicitud.setFaseActual(null);
        solicitud.setGrupo(0);
        ParticipacionEquipoTorneo guardada = participacionEquipoDao.save(solicitud);
        equipoBase.setParticipacionTorneo(guardada);
        notificarOrganizadorSolicitudEquipo(guardada);
        return equipoBase;
    }

    @Transactional
    public DtoResultadoRegistroPlanilla registrarPlanilla(long idTorneo, DtoRegistroPlanilla planilla, long usuarioId) {
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Usuario gestor = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        boolean propietario = torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() == gestor.getId();
        boolean ayudante = ayudanteTorneoDao.findByTorneoAndUsuario(torneo, gestor).isPresent();
        if (!propietario && !ayudante) {
            throw new IllegalArgumentException("Solo el organizador o un ayudante autorizado puede registrar equipos");
        }
        if (!permiteGestionSolicitudes(torneo)) {
            throw new IllegalArgumentException("El registro de equipos solo está disponible durante las inscripciones");
        }
        if (planilla == null || planilla.getNombreEquipo() == null || planilla.getNombreEquipo().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }
        if (planilla.getDelegado() == null || planilla.getJugadores() == null || planilla.getJugadores().isEmpty()) {
            throw new IllegalArgumentException("Registra el delegado y al menos un jugador");
        }

        String nombreEquipo = planilla.getNombreEquipo().trim();
        DtoRegistroPlanilla.Perfil delegadoPerfil = validarPerfilPlanilla(planilla.getDelegado());
        List<DtoRegistroPlanilla.Perfil> perfilesJugadores = planilla.getJugadores().stream()
                .map(this::validarPerfilPlanilla)
                .toList();
        java.util.Set<String> identificaciones = new java.util.HashSet<>();
        identificaciones.add(delegadoPerfil.getIdentificacion());
        for (DtoRegistroPlanilla.Perfil jugador : perfilesJugadores) {
            if (!identificaciones.add(jugador.getIdentificacion())) {
                throw new IllegalArgumentException("No se puede repetir la identificación del delegado o de un jugador");
            }
        }

        migrarEquiposLegacy(torneo);
        boolean nombreRepetido = participacionEquipoDao.findByTorneo(torneo).stream()
                .anyMatch(item -> item.getEquipo().getNombre() != null &&
                        item.getEquipo().getNombre().equalsIgnoreCase(nombreEquipo));
        if (nombreRepetido) {
            throw new IllegalArgumentException("Ya existe un equipo con ese nombre en el torneo");
        }
        if (participacionEquipoDao.countByTorneoAndEstado(torneo, "ACEPTADO") >= capacidadDeLaFase(torneo)) {
            throw new IllegalArgumentException("El torneo ya alcanzó su capacidad de equipos");
        }

        validarPerfilDisponible(delegadoPerfil, TipoUsuario.DELEGADO);
        for (DtoRegistroPlanilla.Perfil jugador : perfilesJugadores) {
            validarPerfilDisponible(jugador, TipoUsuario.JUGADOR);
            Usuario cuentaExistente = usuarioDao.findByIdentificacion(jugador.getIdentificacion());
            Jugador jugadorExistente = jugadorDao.findByIdentificacion(jugador.getIdentificacion()).orElse(null);
            if (jugadorExistente != null && participacionDao.findByTorneoAndJugador(torneo, jugadorExistente).isPresent()) {
                throw new IllegalArgumentException("El jugador " + jugador.getIdentificacion() + " ya está inscrito en este torneo");
            }
            if (cuentaExistente != null && cuentaExistente.getTipoUsuario() != TipoUsuario.JUGADOR) {
                throw new IllegalArgumentException("La identificación " + jugador.getIdentificacion() + " ya pertenece a otro tipo de perfil");
            }
        }

        Usuario cuentaDelegado = crearOReutilizarPerfil(delegadoPerfil, TipoUsuario.DELEGADO);
        Persona delegado = personaDao.findByIdentificacion(delegadoPerfil.getIdentificacion())
                .orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setNombre(cuentaDelegado.getNombre());
                    nueva.setIdentificacion(cuentaDelegado.getIdentificacion());
                    return personaDao.save(nueva);
                });

        Equipo equipo = new Equipo();
        equipo.setNombre(nombreEquipo);
        equipo.setDelegado(delegado);
        equipo = equipoDao.save(equipo);

        ParticipacionEquipoTorneo participacionEquipo = new ParticipacionEquipoTorneo();
        participacionEquipo.setEquipo(equipo);
        participacionEquipo.setTorneo(torneo);
        participacionEquipo.setEstado("ACEPTADO");
        participacionEquipo.setFaseActual(null);
        participacionEquipo.setGrupo(0);
        participacionEquipoDao.save(participacionEquipo);

        for (DtoRegistroPlanilla.Perfil perfilJugador : perfilesJugadores) {
            Usuario cuentaJugador = crearOReutilizarPerfil(perfilJugador, TipoUsuario.JUGADOR);
            Jugador jugador = jugadorDao.findByIdentificacion(perfilJugador.getIdentificacion()).orElseGet(() -> {
                Jugador nuevo = new Jugador();
                nuevo.setNombre(cuentaJugador.getNombre());
                nuevo.setIdentificacion(cuentaJugador.getIdentificacion());
                nuevo.setEstadoJugador(com.example.torneos.enums.EstadoJugador.ACTIVO);
                return nuevo;
            });
            jugador.getEquipos().add(equipo);
            jugador.setEquipo(equipo);
            jugadorDao.save(jugador);

            ParticipacionJugadorTorneo participacionJugador = new ParticipacionJugadorTorneo();
            participacionJugador.setTorneo(torneo);
            participacionJugador.setJugador(jugador);
            participacionJugador.setEquipo(equipo);
            participacionJugador.setParticipando(false);
            participacionJugador.setJugoPartido(false);
            participacionDao.save(participacionJugador);
        }

        return new DtoResultadoRegistroPlanilla(equipo.getId(), equipo.getNombre(), perfilesJugadores.size());
    }

    private DtoRegistroPlanilla.Perfil validarPerfilPlanilla(DtoRegistroPlanilla.Perfil perfil) {
        if (perfil == null || perfil.getNombre() == null || perfil.getNombre().isBlank() ||
                perfil.getIdentificacion() == null || perfil.getIdentificacion().isBlank()) {
            throw new IllegalArgumentException("El nombre y la identificación del delegado y de cada jugador son obligatorios");
        }
        DtoRegistroPlanilla.Perfil limpio = new DtoRegistroPlanilla.Perfil();
        limpio.setNombre(perfil.getNombre().trim());
        limpio.setIdentificacion(perfil.getIdentificacion().trim());
        return limpio;
    }

    private void validarPerfilDisponible(DtoRegistroPlanilla.Perfil perfil, TipoUsuario tipoUsuario) {
        Usuario existente = usuarioDao.findByIdentificacion(perfil.getIdentificacion());
        if (existente != null && existente.getTipoUsuario() != tipoUsuario) {
            throw new IllegalArgumentException("La identificación " + perfil.getIdentificacion() + " ya pertenece a otro tipo de perfil");
        }
    }

    private Usuario crearOReutilizarPerfil(DtoRegistroPlanilla.Perfil perfil, TipoUsuario tipoUsuario) {
        Usuario existente = usuarioDao.findByIdentificacion(perfil.getIdentificacion());
        if (existente != null) return existente;
        Usuario nuevo = new Usuario();
        nuevo.setNombre(perfil.getNombre());
        nuevo.setIdentificacion(perfil.getIdentificacion());
        nuevo.setContrasena(perfil.getIdentificacion());
        nuevo.setTipoUsuario(tipoUsuario);
        return usuarioDao.save(nuevo);
    }

    @Transactional(readOnly = true)
    public List<Equipo> getByDelegadoUsuario(long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElse(null);
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no existe");
        }
        Persona delegado = personaDao.findAll().stream()
                .filter(persona -> usuario.getNumeroCelular() != null && usuario.getNumeroCelular().equals(persona.getNumeroCelular()))
                .findFirst()
                .orElse(null);
        return delegado == null ? List.of() : equipoDao.findByDelegado(delegado).stream()
            .map(this::materializarLobsEquipo).toList();
    }

    @Transactional
    public void rechazarSolicitud(long id, long usuarioId) {
        ParticipacionEquipoTorneo solicitud = participacionEquipoDao.findById(id).orElse(null);
        if (solicitud == null || !"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden rechazar solicitudes pendientes");
        }
        if (!permiteGestionSolicitudes(solicitud.getTorneo())) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        validarPropietarioTorneo(solicitud.getTorneo(), usuarioId);
        eliminarNotificacionesAccion("SOLICITUD_TORNEO", solicitud.getId());
        participacionEquipoDao.delete(solicitud);
    }

    private int capacidadDeLaFase(Torneo torneo) {
        if (torneo.getModalidadTorneo() == ModalidadTorneo.GRUPOS ||
                torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS_GRUPOS) {
            return torneo.getCantidadEquipos() * Math.max(torneo.getCantidadGrupos(), 1);
        }
        return torneo.getCantidadEquipos();
    }

    @Transactional
    public List<Equipo> getSolicitudesByTorneo(Long id, long usuarioId) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        if (!permiteGestionSolicitudes(torneo)) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        validarPropietarioTorneo(torneo, usuarioId);
        migrarEquiposLegacy(torneo);
        return participacionEquipoDao.findByTorneoAndEstado(torneo, "PENDIENTE").stream()
            .map(this::adjuntarParticipacion)
            .map(this::materializarLobsEquipo)
            .toList();
    }

    @Transactional
    public List<Equipo> getSolicitudesYAceptadasByTorneo(Long id, long usuarioId) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        if (!permiteGestionSolicitudes(torneo)) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        validarPropietarioTorneo(torneo, usuarioId);
        migrarEquiposLegacy(torneo);
        return participacionEquipoDao.findByTorneo(torneo).stream()
            .map(this::adjuntarParticipacion)
            .map(this::materializarLobsEquipo)
            .toList();
    }

    @Transactional
    public Equipo aceptarSolicitud(long id, long usuarioId) {
        ParticipacionEquipoTorneo solicitud = participacionEquipoDao.findById(id).orElse(null);
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no existe");
        }
        if (!permiteGestionSolicitudes(solicitud.getTorneo())) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        validarPropietarioTorneo(solicitud.getTorneo(), usuarioId);
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("La solicitud ya fue aceptada previamente");
        }
        long cantEquipos = participacionEquipoDao.countByTorneoAndEstado(solicitud.getTorneo(), "ACEPTADO");
        if (capacidadDeLaFase(solicitud.getTorneo()) <= cantEquipos) {
            throw new IllegalArgumentException("El torneo ya alcanzó la cantidad máxima de equipos");
        }
        solicitud.setEstado("ACEPTADO");
        solicitud.setFaseActual(solicitud.getTorneo().getFaseTorneo());
        solicitud.setGrupo(0);
        participacionEquipoDao.save(solicitud);
        eliminarNotificacionesAccion("SOLICITUD_TORNEO", solicitud.getId());
        notificarAceptacion(solicitud);
        return adjuntarParticipacion(solicitud);
    }

    private void notificarAceptacion(ParticipacionEquipoTorneo participacion) {
        Persona delegado = participacion.getEquipo().getDelegado();
        if (delegado == null || delegado.getNumeroCelular() == null) return;
        Usuario usuario = usuarioDao.findByNumeroCelular(delegado.getNumeroCelular());
        if (usuario == null) return;
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(usuario);
        notificacion.setTitulo("Equipo aceptado en el torneo");
        notificacion.setMensaje("El equipo " + participacion.getEquipo().getNombre() +
                " fue aceptado en " + participacion.getTorneo().getNombre() + ". Ya puedes inscribir jugadores al torneo.");
        notificacion.setRuta("/auth/torneos/torneo/" + participacion.getTorneo().getId() + "/equipo/" + participacion.getEquipo().getId());
        notificacion.setCreadaEn(java.time.LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionDao.save(notificacion);
    }

    private void notificarOrganizadorSolicitudEquipo(ParticipacionEquipoTorneo participacion) {
        Usuario organizador = participacion.getTorneo().getEncargadoTorneo();
        if (organizador == null) return;
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(organizador);
        notificacion.setTitulo("Nueva solicitud de equipo");
        notificacion.setMensaje("El equipo " + participacion.getEquipo().getNombre() + " solicitó inscribirse en " +
                participacion.getTorneo().getNombre() + ".");
        notificacion.setRuta("/auth/torneos/torneo/" + participacion.getTorneo().getId() + "/solicitudes");
        notificacion.setTipo("ACCION");
        notificacion.setReferenciaTipo("SOLICITUD_TORNEO");
        notificacion.setReferenciaId(participacion.getId());
        notificacion.setCreadaEn(java.time.LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionDao.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<Equipo> getAll() {
        return equipoDao.findAll().stream().map(this::materializarLobsEquipo).toList();
    }

    private Equipo materializarLobsEquipo(Equipo equipo) {
        if (equipo == null) return null;
        equipo.getEscudo();
        equipo.getBandera();
        if (equipo.getDelegado() != null) equipo.getDelegado().getFoto();
        if (equipo.getEntrenador() != null) equipo.getEntrenador().getFoto();
        if (equipo.getListaJugadoresActivos() != null) {
            equipo.getListaJugadoresActivos().forEach(jugador -> jugador.getFoto());
        }
        if (equipo.getListaJugadoresInactivos() != null) {
            equipo.getListaJugadoresInactivos().forEach(jugador -> jugador.getFoto());
        }
        return equipo;
    }

    @Transactional(readOnly = true)
    public Equipo getById(Long id) {
        Equipo equipo = equipoDao.findById(id).orElse(null);
        if (equipo == null) {
            throw  new IllegalArgumentException("El Equipo no existe");
        }
        List<Jugador> jugadores = jugadorDao.findByEquiposContaining(equipo);
        equipo.setListaJugadoresActivos(jugadores.stream()
            .filter(jugador -> jugador.getEstadoJugador() == null || jugador.getEstadoJugador().name().equals("ACTIVO"))
            .toList());
        equipo.setListaJugadoresInactivos(jugadores.stream()
            .filter(jugador -> jugador.getEstadoJugador() != null && !jugador.getEstadoJugador().name().equals("ACTIVO"))
            .toList());
        equipo.getListaJugadoresActivos().forEach(jugador -> jugador.getFoto());
        equipo.getListaJugadoresInactivos().forEach(jugador -> jugador.getFoto());
        return materializarLobsEquipo(equipo);
    }

    @Transactional
    public Equipo getByIdAndTorneo(long idEquipo, long idTorneo) {
        Equipo equipo = getById(idEquipo);
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        ParticipacionEquipoTorneo participacion = obtenerOCrearParticipacionLegacy(equipo, torneo);
        if (participacion == null || !"ACEPTADO".equals(participacion.getEstado())) {
            throw new IllegalArgumentException("El equipo no participa en este torneo");
        }
        return materializarLobsEquipo(adjuntarParticipacion(participacion));
    }

    public List<ParticipacionEquipoTorneo> participacionesTorneosEquipo(long idEquipo) {
        Equipo equipo = equipo(idEquipo);
        List<ParticipacionEquipoTorneo> participaciones = new ArrayList<>(participacionEquipoDao.findByEquipo(equipo));
        if (equipo.getTorneo() != null) {
            obtenerOCrearParticipacionLegacy(equipo, equipo.getTorneo());
            participaciones = new ArrayList<>(participacionEquipoDao.findByEquipo(equipo));
        }
        return participaciones;
    }

    private ParticipacionEquipoTorneo obtenerOCrearParticipacionLegacy(Equipo equipo, Torneo torneo) {
        Optional<ParticipacionEquipoTorneo> existente = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo);
        if (existente.isPresent() || equipo.getTorneo() == null || equipo.getTorneo().getId() != torneo.getId()) {
            return existente.orElse(null);
        }
        ParticipacionEquipoTorneo participacion = new ParticipacionEquipoTorneo();
        participacion.setEquipo(equipo);
        participacion.setTorneo(torneo);
        participacion.setEstado(equipo.getFaseActual() == null ? "PENDIENTE" : "ACEPTADO");
        participacion.setFaseActual(equipo.getFaseActual());
        participacion.setGrupo(equipo.getGrupo());
        return participacionEquipoDao.save(participacion);
    }

    private Equipo adjuntarParticipacion(ParticipacionEquipoTorneo participacion) {
        Equipo equipo = participacion.getEquipo();
        equipo.setParticipacionTorneo(participacion);
        return equipo;
    }

    private void migrarEquiposLegacy(Torneo torneo) {
        for (Equipo equipoLegacy : equipoDao.findByTorneo(torneo)) {
            obtenerOCrearParticipacionLegacy(equipoLegacy, torneo);
        }
    }

    @Transactional
    public List<Equipo> getByTorneo(Long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        migrarEquiposLegacy(torneo);
        List<Equipo> listaEquipos = new ArrayList<>(participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
            .map(this::adjuntarParticipacion)
            .map(this::materializarLobsEquipo)
                .toList());
        for (Equipo legacy : equipoDao.findByTorneo(torneo)) {
            if (listaEquipos.stream().noneMatch(equipo -> equipo.getId() == legacy.getId())) {
                ParticipacionEquipoTorneo participacion = obtenerOCrearParticipacionLegacy(legacy, torneo);
                if (participacion != null && "ACEPTADO".equals(participacion.getEstado())) {
                    listaEquipos.add(materializarLobsEquipo(adjuntarParticipacion(participacion)));
                }
            }
        }
        if (listaEquipos.isEmpty()) {
            throw  new IllegalArgumentException("No hay equipos");
        }
        return listaEquipos;
    }

    public Jugador registrarJugadorEnEquipo(long idTorneo, long idEquipo, long idUsuario) {
        Torneo torneo = torneoDao.findById(idTorneo)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Equipo equipo = equipoDao.findById(idEquipo)
                .orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
        Usuario usuario = usuarioDao.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        ParticipacionEquipoTorneo participacionEquipo = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
            .orElseGet(() -> obtenerOCrearParticipacionLegacy(equipo, torneo));
        if (participacionEquipo == null || !"ACEPTADO".equals(participacionEquipo.getEstado())) {
            throw new IllegalArgumentException("El equipo no pertenece a este torneo o todavía no está aceptado");
        }
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) {
            throw new IllegalArgumentException("Tu perfil debe tener una identificación registrada");
        }
        Jugador jugador = jugadorPorUsuario(usuario);
        ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(torneo, jugador).orElse(null);
        if (participacion != null && participacion.getEquipo().getId() != equipo.getId()) {
            throw new IllegalArgumentException("El jugador ya participa en este torneo con otro equipo");
        }
        jugador.getEquipos().add(equipo);
        jugador.setEquipo(equipo);
        if (participacion == null) {
            participacion = new ParticipacionJugadorTorneo();
            participacion.setTorneo(torneo);
            participacion.setJugador(jugador);
        }
        participacion.setEquipo(equipo);
        participacion.setParticipando(false);
        participacionDao.save(participacion);
        return jugadorDao.save(jugador);
    }

    @Transactional
    public SolicitudJugadorEquipo solicitarJugador(long idEquipo, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) throw new IllegalArgumentException("Tu perfil debe tener una identificación registrada");
        Jugador jugador = jugadorPorUsuario(usuario);
        if (jugador.getEquipos().contains(equipo)) throw new IllegalArgumentException("Ya perteneces a este equipo");
        if (solicitudJugadorDao.findByEquipoAndJugadorAndEstado(equipo, jugador, "PENDIENTE").isPresent()) throw new IllegalArgumentException("Ya tienes una solicitud pendiente");
        SolicitudJugadorEquipo solicitud = new SolicitudJugadorEquipo();
        solicitud.setEquipo(equipo);
        solicitud.setJugador(jugador);
        SolicitudJugadorEquipo guardada = solicitudJugadorDao.save(solicitud);
        notificarAccionDelegado(equipo, "Solicitud para unirse al equipo",
            jugador.getNombre() + " quiere unirse al equipo " + equipo.getNombre() + ".",
            "SOLICITUD_JUGADOR", guardada.getId());
        return guardada;
    }

    public List<SolicitudJugadorEquipo> solicitudesJugador(long idEquipo, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        return solicitudJugadorDao.findByEquipoAndEstado(equipo, "PENDIENTE");
    }

    @Transactional
    public Equipo aceptarSolicitudJugador(long idSolicitud, long idUsuario) {
        SolicitudJugadorEquipo solicitud = solicitudJugadorDao.findById(idSolicitud).orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));
        validarDelegado(solicitud.getEquipo(), idUsuario);
        Jugador jugador = solicitud.getJugador();
        jugador.getEquipos().add(solicitud.getEquipo());
        if (jugador.getEquipo() == null) jugador.setEquipo(solicitud.getEquipo());
        jugadorDao.save(jugador);
        solicitud.setEstado("ACEPTADA");
        solicitudJugadorDao.save(solicitud);
        eliminarNotificacionesAccion("SOLICITUD_JUGADOR", solicitud.getId());
        return getById(solicitud.getEquipo().getId());
    }

    @Transactional
    public void rechazarSolicitudJugador(long idSolicitud, long idUsuario) {
        SolicitudJugadorEquipo solicitud = solicitudJugadorDao.findById(idSolicitud).orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));
        validarDelegado(solicitud.getEquipo(), idUsuario);
        solicitud.setEstado("RECHAZADA");
        solicitudJugadorDao.save(solicitud);
        eliminarNotificacionesAccion("SOLICITUD_JUGADOR", solicitud.getId());
    }

    @Transactional
    public DtoInvitacionEquipo invitarJugador(long idEquipo, String identificacion, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        String identificacionLimpia = identificacion == null ? "" : identificacion.trim();
        Jugador jugador = jugadorDao.findByIdentificacion(identificacionLimpia).orElseGet(() -> {
            Usuario cuenta = usuarioDao.findByIdentificacion(identificacionLimpia);
            if (cuenta == null) {
                throw new IllegalArgumentException("No se encontró una cuenta con esa identificación");
            }
            return jugadorPorUsuario(cuenta);
        });
        if (jugador.getEquipos().contains(equipo)) throw new IllegalArgumentException("El jugador ya pertenece al equipo");
        if (invitacionEquipoDao.findByEquipoAndJugadorAndEstado(equipo, jugador, "PENDIENTE").isPresent()) {
            throw new IllegalArgumentException("Ya existe una invitación pendiente para este jugador");
        }
        InvitacionEquipo invitacion = new InvitacionEquipo();
        invitacion.setEquipo(equipo);
        invitacion.setJugador(jugador);
        invitacion.setEstado("PENDIENTE");
        invitacion.setCreadaEn(java.time.LocalDateTime.now());
        InvitacionEquipo guardada = invitacionEquipoDao.save(invitacion);
        Usuario jugadorUsuario = usuarioDao.findByIdentificacion(jugador.getIdentificacion());
        if (jugadorUsuario != null) {
            NotificacionUsuario notificacion = new NotificacionUsuario();
            notificacion.setUsuario(jugadorUsuario);
            notificacion.setTitulo("Invitación a equipo");
            notificacion.setMensaje("El equipo " + equipo.getNombre() + " te invitó a unirte a su plantel.");
            notificacion.setRuta("/auth/usuario");
            notificacion.setTipo("ACCION");
            notificacion.setReferenciaTipo("INVITACION_EQUIPO");
            notificacion.setReferenciaId(guardada.getId());
            notificacion.setCreadaEn(java.time.LocalDateTime.now());
            notificacion.setLeida(false);
            notificacionDao.save(notificacion);
        }
        return invitacionEquipoDao.buscarDto(guardada.getId())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar la invitación creada"));
    }

    @Transactional(readOnly = true)
    public List<DtoInvitacionEquipo> invitacionesJugador(long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) throw new IllegalArgumentException("El perfil debe tener una identificación registrada");
        return invitacionEquipoDao.listarDtoPorIdentificacionYEstado(usuario.getIdentificacion(), "PENDIENTE");
    }

    @Transactional
    public DtoInvitacionEquipo resolverInvitacionJugador(long idInvitacion, long idUsuario, boolean aceptar) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        InvitacionEquipo invitacion = invitacionEquipoDao.findById(idInvitacion)
                .orElseThrow(() -> new IllegalArgumentException("La invitación no existe"));
        if (usuario.getIdentificacion() == null || !usuario.getIdentificacion().equals(invitacion.getJugador().getIdentificacion())) {
            throw new IllegalArgumentException("Solo el jugador invitado puede responder esta invitación");
        }
        if (!"PENDIENTE".equals(invitacion.getEstado())) throw new IllegalArgumentException("La invitación ya fue respondida");
        if (aceptar) {
            invitacion.getJugador().getEquipos().add(invitacion.getEquipo());
            if (invitacion.getJugador().getEquipo() == null) invitacion.getJugador().setEquipo(invitacion.getEquipo());
            jugadorDao.save(invitacion.getJugador());
            invitacion.setEstado("ACEPTADA");
        } else {
            invitacion.setEstado("RECHAZADA");
        }
        invitacionEquipoDao.save(invitacion);
        eliminarNotificacionesAccion("INVITACION_EQUIPO", invitacion.getId());
        return invitacionEquipoDao.buscarDto(idInvitacion)
            .orElseThrow(() -> new IllegalStateException("No se pudo recuperar la invitación actualizada"));
    }

    private void notificarAccionDelegado(Equipo equipo, String titulo, String mensaje, String referenciaTipo, Long referenciaId) {
        if (equipo.getDelegado() == null || equipo.getDelegado().getNumeroCelular() == null) return;
        Usuario delegado = usuarioDao.findByNumeroCelular(equipo.getDelegado().getNumeroCelular());
        if (delegado == null) return;
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(delegado);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setRuta("/auth/equipo/" + equipo.getId());
        notificacion.setTipo("ACCION");
        notificacion.setReferenciaTipo(referenciaTipo);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setCreadaEn(java.time.LocalDateTime.now());
        notificacion.setLeida(false);
        notificacionDao.save(notificacion);
    }

    private void eliminarNotificacionesAccion(String referenciaTipo, Long referenciaId) {
        notificacionDao.deleteByReferenciaTipoAndReferenciaId(referenciaTipo, referenciaId);
    }

    public Jugador buscarJugadorPorIdentificacion(long idEquipo, String identificacion, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        String identificacionLimpia = identificacion == null ? "" : identificacion.trim();
        return jugadorDao.findByIdentificacion(identificacionLimpia).orElseGet(() -> {
            Usuario cuenta = usuarioDao.findByIdentificacion(identificacionLimpia);
            if (cuenta == null) {
                throw new IllegalArgumentException("No se encontró una cuenta con esa identificación");
            }
            return jugadorPorUsuario(cuenta);
        });
    }

    @Transactional
    public ParticipacionJugadorTorneo agregarJugadorATorneo(long idEquipo, long idTorneo, long idJugador, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        ParticipacionEquipoTorneo participacionEquipo = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este torneo"));
        if (!"ACEPTADO".equals(participacionEquipo.getEstado())) {
            throw new IllegalArgumentException("El equipo debe ser aceptado antes de inscribir jugadores");
        }
        Jugador jugador = jugadorDao.findById(idJugador).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        if (jugador.getEquipos() == null || jugador.getEquipos().stream().noneMatch(item -> item.getId() == equipo.getId())) {
            throw new IllegalArgumentException("El jugador debe pertenecer primero al plantel del equipo");
        }
        ParticipacionJugadorTorneo existente = participacionDao.findByTorneoAndJugador(torneo, jugador).orElse(null);
        if (existente != null) {
            if (existente.getEquipo() != null && existente.getEquipo().getId() == equipo.getId()) return existente;
            throw new IllegalArgumentException("El jugador ya pertenece a otro equipo en este torneo");
        }
        ParticipacionJugadorTorneo participacion = new ParticipacionJugadorTorneo();
        participacion.setTorneo(torneo);
        participacion.setJugador(jugador);
        participacion.setEquipo(equipo);
        participacion.setParticipando(false);
        participacion.setJugoPartido(false);
        return participacionDao.save(participacion);
    }

    @Transactional
    public void eliminarJugadorDeTorneo(long idEquipo, long idTorneo, long idJugador, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Jugador jugador = jugadorDao.findById(idJugador).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(torneo, jugador)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no está inscrito en este torneo"));
        if (participacion.getEquipo() == null || participacion.getEquipo().getId() != equipo.getId()) {
            throw new IllegalArgumentException("El jugador no está inscrito con este equipo");
        }
        if (participacion.isJugoPartido()) {
            throw new IllegalArgumentException("No se puede quitar del torneo a un jugador que ya participó en un partido");
        }
        participacionDao.delete(participacion);
    }

    public void eliminarJugador(long idEquipo, long idJugador, long idUsuario) {
        Equipo equipo = equipo(idEquipo);
        validarDelegado(equipo, idUsuario);
        Jugador jugador = jugadorDao.findById(idJugador).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        jugador.getEquipos().remove(equipo);
        if (jugador.getEquipo() != null && jugador.getEquipo().getId() == idEquipo) jugador.setEquipo(jugador.getEquipos().stream().findFirst().orElse(null));
        jugadorDao.save(jugador);
    }

    private Jugador jugadorPorUsuario(Usuario usuario) {
        return jugadorDao.findByIdentificacion(usuario.getIdentificacion()).orElseGet(() -> {
            Jugador jugador = new Jugador();
            jugador.setIdentificacion(usuario.getIdentificacion());
            jugador.setNombre(usuario.getNombre());
            jugador.setNumeroCelular(usuario.getNumeroCelular());
            jugador.setNumeroTelefono(usuario.getNumeroTelefono());
            jugador.setCorreoElectronico(usuario.getCorreoElectronico());
            jugador.setFoto(usuario.getFoto());
            jugador.setEstadoJugador(com.example.torneos.enums.EstadoJugador.ACTIVO);
            return jugadorDao.save(jugador);
        });
    }

    private void validarDelegado(Equipo equipo, long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (usuario.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.DELEGADO || equipo.getDelegado() == null || usuario.getNumeroCelular() == null || !usuario.getNumeroCelular().equals(equipo.getDelegado().getNumeroCelular())) throw new IllegalArgumentException("Solo el delegado puede gestionar los jugadores");
    }

    public ParticipacionJugadorTorneo cambiarEquipoJugador(long idTorneo, long idJugador, long idEquipoNuevo, long idUsuario) {
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Usuario organizador = usuarioDao.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (organizador.getTipoUsuario() != com.example.torneos.enums.TipoUsuario.ORGANIZADOR || torneo.getEncargadoTorneo() == null || torneo.getEncargadoTorneo().getId() != organizador.getId()) throw new IllegalArgumentException("Solo el organizador puede cambiar el equipo del jugador");
        Jugador jugador = jugadorDao.findById(idJugador).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        Equipo equipoNuevo = equipo(idEquipoNuevo);
        ParticipacionEquipoTorneo participacionEquipoNueva = participacionEquipoDao.findByEquipoAndTorneo(equipoNuevo, torneo)
            .orElseThrow(() -> new IllegalArgumentException("El equipo destino no participa en este torneo"));
        if (!"ACEPTADO".equals(participacionEquipoNueva.getEstado())) {
            throw new IllegalArgumentException("El equipo destino debe estar aceptado en este torneo");
        }
        ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(torneo, jugador).orElseThrow(() -> new IllegalArgumentException("El jugador aún no está registrado en este torneo"));
        if (participacion.isCambioEquipo()) throw new IllegalArgumentException("El jugador ya realizó su único cambio de equipo en este torneo");
        participacion.setEquipoAnterior(participacion.getEquipo());
        participacion.setEquipo(equipoNuevo);
        participacion.setCambioEquipo(true);
        jugador.getEquipos().add(equipoNuevo);
        jugador.setEquipo(equipoNuevo);
        jugadorDao.save(jugador);
        return participacionDao.save(participacion);
    }

    public List<ParticipacionJugadorTorneo> participacionesTorneo(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        return participacionDao.findByTorneo(torneo);
    }

    public List<ParticipacionJugadorTorneo> getParticipacionesEquipo(long idEquipo) {
        Equipo equipo = equipo(idEquipo);
        return participacionDao.findAll().stream()
                .filter(participacion -> participacion.getEquipo() != null && participacion.getEquipo().getId() == equipo.getId())
                .toList();
    }

    private Equipo equipo(long id) {
        return equipoDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
    }
    public List<Equipo> getByTorneoModalidad(Long id, ModalidadTorneo modalidadTorneo) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        migrarEquiposLegacy(torneo);
        List<Equipo> listaEquipos = participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
            .filter(participacion -> modalidadTorneo.equals(ModalidadTorneo.ELIMINATORIAS_GRUPOS) ||
                participacion.getFaseActual() != null)
            .map(this::adjuntarParticipacion)
            .toList();
        if (listaEquipos.size() == 0) {
            throw  new IllegalArgumentException("No hay equipos en esta fase");
        }
        return listaEquipos;
    }
    public List<Equipo> getByTorneoFase(Long id, FaseActual faseActual) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        migrarEquiposLegacy(torneo);
        List<Equipo> listaEquipos = participacionEquipoDao.findByTorneoAndEstadoAndFaseActual(torneo, "ACEPTADO", faseActual).stream()
            .map(this::adjuntarParticipacion)
            .toList();
        if (listaEquipos.size() == 0) {
            throw  new IllegalArgumentException("No hay equipos en esta fase");
        }
        return listaEquipos;
    }
    public Equipo update(Equipo equipo, Long usuarioId) {
        Optional<Equipo> optEquipo = equipoDao.findById(equipo.getId());
        if (!optEquipo.isPresent()) {
            throw  new IllegalArgumentException("El Equipo no existe");
        }
        Equipo equipoDB = optEquipo.get();
        if (usuarioId == null) {
            throw new IllegalArgumentException("No se pudo identificar al usuario que modifica el equipo");
        }
        Usuario usuario = usuarioDao.findById(usuarioId).orElse(null);
        if (usuario == null || equipoDB.getDelegado() == null ||
                usuario.getNumeroCelular() == null ||
                !usuario.getNumeroCelular().equals(equipoDB.getDelegado().getNumeroCelular())) {
            throw new IllegalArgumentException("Solo el delegado del equipo puede modificar su información");
        }
        if (equipoDB.getTorneo() != null && equipoDB.getTorneo().getEstadoTorneo() != null &&
                (!equipoDB.getTorneo().getEstadoTorneo().name().equals("INSCRIPCIONES") &&
                 !equipoDB.getTorneo().getEstadoTorneo().name().equals("INSCRIPCIONES_ACTIVO"))) {
            throw new IllegalArgumentException("El equipo no puede modificarse mientras participa en un torneo activo");
        }
        if (!equipoDB.getDelegado().equals(equipo.getDelegado()) && equipoDao.existsByDelegado(equipo.getDelegado())) {
            throw  new IllegalArgumentException("El delegado ya pertenece a un equipo");
        }
        equipoDB.setNombre(equipo.getNombre());
        equipoDB.setEntrenador(equipo.getEntrenador());
        equipoDB.setBandera(equipo.getBandera());
        equipoDB.setEscudo(equipo.getEscudo());
        return equipoDao.save(equipoDB);
    }
    public void delete(long id, long usuarioId) {
        Optional<Equipo> optEquipo = equipoDao.findById(id);
        if (optEquipo.isPresent()) {
            Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
            Equipo equipo = optEquipo.get();
            if (equipo.getDelegado() == null || usuario.getNumeroCelular() == null ||
                    !usuario.getNumeroCelular().equals(equipo.getDelegado().getNumeroCelular())) {
                throw new IllegalArgumentException("Solo el delegado del equipo puede eliminarlo");
            }
            equipoDao.delete(optEquipo.get());
        }
    }

    private void validarPropietarioTorneo(Torneo torneo, long usuarioId) {
        if (torneo.getEncargadoTorneo() == null || torneo.getEncargadoTorneo().getId() != usuarioId) {
            throw new IllegalArgumentException("Solo el organizador propietario puede gestionar solicitudes");
        }
    }
}
