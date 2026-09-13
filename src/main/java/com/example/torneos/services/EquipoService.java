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
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.enums.EstadoTorneo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        if (delegado.getId() > 0 && personaDao.existsById(delegado.getId())) {
            return personaDao.findById(delegado.getId()).orElse(delegado);
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
        if (equipo == null || equipo.getTorneo() == null) {
            throw new IllegalArgumentException("Torneo nulo");
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
        List<Equipo> listaEquiposDelegado = equipoDao.findByDelegado(equipo.getDelegado());
        int cantEquipos = equipoDao.countByTorneoAndGrupoAndFaseActual(equipo.getTorneo(), equipo.getGrupo(), equipo.getTorneo().getFaseTorneo());
        if (equipo.getTorneo().getCantidadEquipos() == cantEquipos) {
            throw new IllegalArgumentException("EL grupo esta completo");
        }
        for (Equipo equipoDelegado : listaEquiposDelegado) {
            if (equipoDelegado != null && equipoDelegado.getTorneo() != null && equipoDelegado.getTorneo().equals(equipo.getTorneo()) && equipoDelegado.getFaseActual() != null) {
                throw new IllegalArgumentException("El delegado ya pertenece a un equipo");
            }
        }
        equipo.setFaseActual(equipo.getTorneo().getFaseTorneo());
        return equipoDao.save(equipo);
    }

    public Equipo saveSolicitud(Equipo equipo) {
        if (equipo == null || equipo.getTorneo() == null) {
            throw new IllegalArgumentException("Torneo nulo");
        }
        if (equipo.getDelegado() == null) {
            throw new IllegalArgumentException("El delegado es obligatorio");
        }
        equipo.setDelegado(resolverDelegado(equipo.getDelegado()));
        if (equipo.getNombre() == null || equipo.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }

        Optional<Equipo> solicitudPendiente = equipoDao.findByDelegadoAndTorneoAndFaseActualIsNull(equipo.getDelegado(), equipo.getTorneo());
        List<Equipo> equiposDelDelegado = equipoDao.findByDelegadoAndTorneo(equipo.getDelegado(), equipo.getTorneo());
        if ((solicitudPendiente != null && solicitudPendiente.isPresent()) || !equiposDelDelegado.isEmpty()) {
            throw new IllegalArgumentException("El delegado ya tiene una solicitud o un equipo en este torneo");
        }
        boolean equipoRepetido = equipoDao.findByTorneo(equipo.getTorneo()).stream()
                .anyMatch(equipoRegistrado -> equipoRegistrado.getNombre() != null &&
                        equipoRegistrado.getNombre().equalsIgnoreCase(equipo.getNombre()));
        if (equipoRepetido) {
            throw new IllegalArgumentException("Ya existe un equipo con ese nombre en el torneo");
        }

        if (equipo.getTorneo().getUbicacion() == null || equipo.getTorneo().getUbicacion().isBlank()) {
            throw new IllegalArgumentException("El torneo no tiene ubicación registrada");
        }
        if (equipo.getDelegado().getNumeroCelular() == null || equipo.getDelegado().getNumeroCelular().isBlank()) {
            throw new IllegalArgumentException("El delegado debe tener un número de celular registrado");
        }
        Usuario usuarioDelegado = usuarioDao.findByNumeroCelular(equipo.getDelegado().getNumeroCelular());
        if (usuarioDelegado == null || usuarioDelegado.getUbicacion() == null || usuarioDelegado.getUbicacion().isBlank()) {
            throw new IllegalArgumentException("El delegado debe tener una ubicación registrada para enviar solicitudes");
        }
        if (!usuarioDelegado.getUbicacion().equalsIgnoreCase(equipo.getTorneo().getUbicacion())) {
            throw new IllegalArgumentException("Solo puedes enviar solicitudes en la misma ubicación del usuario");
        }
        if (equipo.getTorneo().getEstadoTorneo() == null ||
                (!equipo.getTorneo().getEstadoTorneo().name().equals("INSCRIPCIONES") &&
                 !equipo.getTorneo().getEstadoTorneo().name().equals("INSCRIPCIONES_ACTIVO") &&
                 !equipo.getTorneo().getEstadoTorneo().name().equals("INSCRIPCIONES_ACRIVO"))) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden enviar en estado de inscripciones");
        }

        equipo.setFaseActual(null);
        equipo.setGrupo(0);
        equipo.setId(0);
        return equipoDao.save(equipo);
    }

    public List<Equipo> getByDelegadoUsuario(long idUsuario) {
        Usuario usuario = usuarioDao.findById(idUsuario).orElse(null);
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no existe");
        }
        Persona delegado = personaDao.findAll().stream()
                .filter(persona -> usuario.getNumeroCelular() != null && usuario.getNumeroCelular().equals(persona.getNumeroCelular()))
                .findFirst()
                .orElse(null);
        return delegado == null ? List.of() : equipoDao.findByDelegado(delegado);
    }

    public void rechazarSolicitud(long id) {
        Equipo solicitud = equipoDao.findById(id).orElse(null);
        if (solicitud == null || solicitud.getFaseActual() != null) {
            throw new IllegalArgumentException("Solo se pueden rechazar solicitudes pendientes");
        }
        if (!permiteGestionSolicitudes(solicitud.getTorneo())) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        equipoDao.delete(solicitud);
    }

    private int capacidadDeLaFase(Torneo torneo) {
        if (torneo.getModalidadTorneo() == ModalidadTorneo.GRUPOS ||
                torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS_GRUPOS) {
            return torneo.getCantidadEquipos() * Math.max(torneo.getCantidadGrupos(), 1);
        }
        return torneo.getCantidadEquipos();
    }

    public List<Equipo> getSolicitudesByTorneo(Long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        if (!permiteGestionSolicitudes(torneo)) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        List<Equipo> listaSolicitudes = equipoDao.findByTorneoAndFaseActualIsNull(torneo);
        return listaSolicitudes;
    }

    public List<Equipo> getSolicitudesYAceptadasByTorneo(Long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        if (!permiteGestionSolicitudes(torneo)) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        return equipoDao.findByTorneo(torneo);
    }

    public Equipo aceptarSolicitud(Long id) {
        Equipo solicitud = equipoDao.findById(id).orElse(null);
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no existe");
        }
        if (solicitud.getTorneo() == null) {
            throw new IllegalArgumentException("La solicitud no tiene torneo asociado");
        }
        if (!permiteGestionSolicitudes(solicitud.getTorneo())) {
            throw new IllegalArgumentException("Las solicitudes solo se pueden gestionar durante las inscripciones");
        }
        if (solicitud.getFaseActual() != null) {
            throw new IllegalArgumentException("La solicitud ya fue aceptada previamente");
        }
        int cantEquipos = equipoDao.countByTorneoAndFaseActual(solicitud.getTorneo(), solicitud.getTorneo().getFaseTorneo());
        if (capacidadDeLaFase(solicitud.getTorneo()) <= cantEquipos) {
            throw new IllegalArgumentException("El torneo ya alcanzó la cantidad máxima de equipos");
        }

        solicitud.setFaseActual(solicitud.getTorneo().getFaseTorneo());
        solicitud.setGrupo(0);
        return equipoDao.save(solicitud);
    }

    public List<Equipo> getAll() {
        return equipoDao.findAll();
    }
    public Equipo getById(Long id) {
        Equipo equipo = equipoDao.findById(id).orElse(null);
        if (equipo == null) {
            throw  new IllegalArgumentException("El Equipo no existe");
        }
        return equipo;
    }
    public List<Equipo> getByTorneo(Long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        List<Equipo> listaEquipos = equipoDao.findByTorneo(torneo);
        if (listaEquipos.isEmpty()) {
            throw  new IllegalArgumentException("No hay equipos");
        }
        return listaEquipos;
    }
    public List<Equipo> getByTorneoModalidad(Long id, ModalidadTorneo modalidadTorneo) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw new IllegalArgumentException("No existe Torneo con id: " + id);
        }
        List<Equipo> listaEquipos = new ArrayList<>();
        if (modalidadTorneo.equals(ModalidadTorneo.ELIMINATORIAS_GRUPOS)) {
            listaEquipos = equipoDao.findByTorneo(torneo);
        } else if (modalidadTorneo.equals(ModalidadTorneo.GRUPOS) || modalidadTorneo.equals(ModalidadTorneo.LIGA)) {
            List<FaseActual> listFA = new ArrayList<>();
            listFA.add(FaseActual.FASE_GRUPOS);
            listFA.add(FaseActual.TREINTAIDOSAVOS);
            listFA.add(FaseActual.DIECISEISAVOS);
            listFA.add(FaseActual.OCTAVOS);
            listFA.add(FaseActual.CUARTOS);
            listFA.add(FaseActual.SEMIFINAL);
            listFA.add(FaseActual.FINAL);
            listaEquipos = equipoDao.findByTorneoAndFaseActualIn(torneo, listFA);
        }
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
        List<Equipo> listaEquipos = equipoDao.findByTorneoAndFaseActual(torneo, faseActual);
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
    public void delete(long id) {
        Optional<Equipo> optEquipo = equipoDao.findById(id);
        if (optEquipo.isPresent()) {
            equipoDao.delete(optEquipo.get());
        }
    }
}
