package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.DTO.DtoGrupoLlave;
import com.example.torneos.DTO.DtoOptionTorneo;
import com.example.torneos.DTO.DtoDistribucionEquipo;
import com.example.torneos.DTO.DtoAyudanteTorneo;
import com.example.torneos.dao.*;
import com.example.torneos.entities.*;
import com.example.torneos.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Service
public class TorenoService {
    @Autowired
    private TorneoDao torneoDao;
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private ReglamentoDao reglamentoDao;
    @Autowired
    private PartidoDao partidoDao;
    @Autowired
    private EquipoDao equipoDao;
    @Autowired
    private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Autowired
    private DistribucionEquipoTorneoDao distribucionDao;
    @Autowired
    private AyudanteTorneoDao ayudanteTorneoDao;

    private String normalizarUbicacion(String ubicacion) {
        return Normalizer.normalize(ubicacion == null ? "" : ubicacion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    public Torneo save(Torneo torneo, Reglamento reglamento, long usuarioAutenticadoId) {
        Usuario organizadorAutenticado = usuarioDao.findById(usuarioAutenticadoId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario autenticado no existe"));
        if (organizadorAutenticado.getTipoUsuario() != TipoUsuario.ORGANIZADOR) {
            throw new IllegalArgumentException("Solo un organizador puede crear torneos");
        }
        if (torneo == null) {
            throw new IllegalArgumentException("El torneo es obligatorio");
        }
        torneo.setEncargadoTorneo(organizadorAutenticado);
        if (torneo.getFaseTorneo() == null) {
            torneo.setFaseTorneo(FaseActual.FASE_GRUPOS);
        }
        if (torneo.getUbicacion() == null || torneo.getUbicacion().isBlank()) {
            if (torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() > 0) {
                Usuario encargado = usuarioDao.findById(torneo.getEncargadoTorneo().getId()).orElse(null);
                if (encargado != null && encargado.getUbicacion() != null && !encargado.getUbicacion().isBlank()) {
                    torneo.setUbicacion(encargado.getUbicacion());
                }
            }
        }
        validarDatosObligatoriosTorneo(torneo);
        if (torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() > 0) {
            Usuario encargado = usuarioDao.findById(torneo.getEncargadoTorneo().getId()).orElse(null);
            if (encargado == null || encargado.getUbicacion() == null || encargado.getUbicacion().isBlank()) {
                throw new IllegalArgumentException("El organizador debe tener una ubicación registrada");
            }
            String ubicacionOrganizador = normalizarUbicacion(encargado.getUbicacion());
            String ubicacionTorneo = normalizarUbicacion(torneo.getUbicacion());
            String ciudadTorneo = torneo.getCiudad() == null ? "" : normalizarUbicacion(torneo.getCiudad().getNombre());
            boolean mismaCiudad = !ciudadTorneo.isBlank() && ubicacionOrganizador.contains(ciudadTorneo);
            if (torneo.getUbicacion() == null || torneo.getUbicacion().isBlank() ||
                    (!mismaCiudad && !ubicacionOrganizador.contains(ubicacionTorneo) &&
                     !ubicacionTorneo.contains(ubicacionOrganizador))) {
                throw new IllegalArgumentException("El torneo debe crearse en la misma ubicación del usuario organizador");
            }
        }
        torneo.setEstadoTorneo(EstadoTorneo.INSCRIPCIONES);
        torneo = torneoDao.save(torneo);
        //reglamento.setTorneo(torneo);
        //reglamentoDao.save(reglamento);
        return torneo;
    }
    public List<Torneo> getAll() {
        return torneoDao.findAll();
    }
    @Transactional(readOnly = true)
    public List<Torneo> getListado(String departamento, Long usuarioId) {
        String filtroDepartamento = departamento == null || departamento.isBlank() || departamento.equalsIgnoreCase("Todos")
                || departamento.equalsIgnoreCase("Seleccione un departamento") ? null : departamento.trim();
        return torneoDao.buscarListado(filtroDepartamento, usuarioId);
    }
    public List<DtoOptionTorneo> getOptionAll() {
        List<DtoOptionTorneo> torneoList = torneoDao.findAll().stream().map(t -> new DtoOptionTorneo(t.getNombre(), t.getId())).toList();

        return torneoList;
    }
    public List<DtoOptionTorneo> getOptionByCiudad(long ciudadId) {
        return torneoDao.findByCiudadId(ciudadId).stream()
                .map(torneo -> new DtoOptionTorneo(torneo.getNombre(), torneo.getId()))
                .toList();
    }
    @Transactional(readOnly = true)
    public List<DtoAyudanteTorneo> listarAyudantes(long torneoId, long usuarioId) {
        Torneo torneo = torneoDao.findById(torneoId).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        validarPropietario(torneo, usuarioId);
        return ayudanteTorneoDao.findByTorneo(torneo).stream()
                .map(item -> new DtoAyudanteTorneo(item.getId(), item.getUsuario().getId(),
                        item.getUsuario().getNombre(), item.getUsuario().getIdentificacion()))
                .toList();
    }

    @Transactional
    public DtoAyudanteTorneo agregarAyudante(long torneoId, long usuarioId, String identificacion) {
        Torneo torneo = torneoDao.findById(torneoId).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Usuario propietario = validarPropietario(torneo, usuarioId);
        Usuario ayudante = usuarioDao.findByIdentificacion(identificacion == null ? "" : identificacion.trim());
        if (ayudante == null) throw new IllegalArgumentException("No existe un perfil con esa identificación");
        if (ayudante.getId() == propietario.getId()) throw new IllegalArgumentException("El organizador ya tiene permisos de gestión");
        AyudanteTorneo relacion = ayudanteTorneoDao.findByTorneoAndUsuario(torneo, ayudante).orElseGet(() -> {
            AyudanteTorneo nueva = new AyudanteTorneo();
            nueva.setTorneo(torneo);
            nueva.setUsuario(ayudante);
            return ayudanteTorneoDao.save(nueva);
        });
        return new DtoAyudanteTorneo(relacion.getId(), ayudante.getId(), ayudante.getNombre(), ayudante.getIdentificacion());
    }

    @Transactional(readOnly = true)
    public boolean puedeRegistrarEquipos(long torneoId, long usuarioId) {
        Torneo torneo = torneoDao.findById(torneoId).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        return torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() == usuario.getId() ||
                ayudanteTorneoDao.findByTorneoAndUsuario(torneo, usuario).isPresent();
    }

    @Transactional
    public void eliminarAyudante(long torneoId, long ayudanteUsuarioId, long usuarioId) {
        Torneo torneo = torneoDao.findById(torneoId).orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        validarPropietario(torneo, usuarioId);
        Usuario ayudante = usuarioDao.findById(ayudanteUsuarioId).orElseThrow(() -> new IllegalArgumentException("El perfil ayudante no existe"));
        ayudanteTorneoDao.deleteByTorneoAndUsuario(torneo, ayudante);
    }

    private Usuario validarPropietario(Torneo torneo, long usuarioId) {
        Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (torneo.getEncargadoTorneo() == null || torneo.getEncargadoTorneo().getId() != usuario.getId()) {
            throw new IllegalArgumentException("Solo el organizador propietario puede administrar ayudantes");
        }
        return usuario;
    }
    public Torneo getById(long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw  new IllegalArgumentException("No existen Torneo con el id:" + id);
        }
        return torneo;
    }
    public List<Torneo> getByUsuarioId(long id) {
        Usuario usuario = usuarioDao.findById(id).orElse(null);
        if (usuario == null) {
            throw  new IllegalArgumentException("El usuario no existe");
        }
        return torneoDao.findByEncargadoTorneo(usuario);
    }
    @Transactional
    public void cabiarFaseTorneo(List<DtoGrupoEquipo> listGrupoEquipo, long idTorneo, long usuarioId) {
        if (listGrupoEquipo == null || listGrupoEquipo.isEmpty()) {
            throw new IllegalArgumentException("Debes organizar al menos un equipo");
        }
        Torneo torneo = torneoDao.findById(idTorneo).get();
        validarPropietario(torneo, usuarioId);
        boolean organizacionInicial = torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES ||
                torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES_ACTIVO;
        FaseActual faseInicial = torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS &&
            torneo.getFaseInicioEliminatorias() != null
            ? torneo.getFaseInicioEliminatorias()
            : torneo.getFaseTorneo();
        FaseActual faseDestino = organizacionInicial
            ? faseInicial
                : listGrupoEquipo.get(0).getFaseActual();
        if (faseDestino == null) {
            faseDestino = FaseActual.FASE_GRUPOS;
        }
        if (!organizacionInicial && esFaseEliminatoria(torneo.getFaseTorneo())) {
            validarClasificadosEliminatorias(torneo, listGrupoEquipo);
        }
        ModalidadFase modalidadFase = obtenerModalidadFase(torneo, faseDestino);
        distribucionDao.deleteAll(distribucionDao.findByTorneoAndFase(torneo, faseDestino));
        torneo.setFaseTorneo(faseDestino);
        for (int i = 0; i < listGrupoEquipo.size(); i++) {
            DtoGrupoEquipo grupoEquipo = listGrupoEquipo.get(i);
            Equipo equipo = equipoDao.findById(grupoEquipo.getIdEquipo()).get();
            ParticipacionEquipoTorneo participacion = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                    .orElseThrow(() -> new IllegalArgumentException("El equipo no está inscrito en este torneo"));
            if (!"ACEPTADO".equals(participacion.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden organizar equipos aceptados");
            }
            if (participacion.getFaseActual() != faseDestino) {
                reiniciarEstadisticas(participacion);
            }
            participacion.setGrupo(grupoEquipo.getGrupo());
            participacion.setFaseActual(faseDestino);
            participacionEquipoDao.save(participacion);
            if (!torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS)) {
                for (int j = i + 1; j < listGrupoEquipo.size(); j++) {
                    DtoGrupoEquipo grupoEquipo2 = listGrupoEquipo.get(j);
                    if (grupoEquipo.getIdEquipo() != grupoEquipo2.getIdEquipo() &&
                            grupoEquipo.getGrupo() == grupoEquipo2.getGrupo()) {
                        Equipo equipo2 = equipoDao.findById(grupoEquipo2.getIdEquipo()).get();
                        crearPartidoSiNoExiste(torneo, faseDestino, equipo, equipo2, grupoEquipo.getGrupo());
                        if (modalidadFase == ModalidadFase.IDA_VUELTA) {
                            crearPartidoSiNoExiste(torneo, faseDestino, equipo2, equipo, grupoEquipo2.getGrupo());
                        }
                    }
                }
            }
            DistribucionEquipoTorneo distribucion = new DistribucionEquipoTorneo();
            distribucion.setTorneo(torneo);
            distribucion.setEquipo(equipo);
            distribucion.setFase(faseDestino);
            distribucion.setGrupo(grupoEquipo.getGrupo());
            distribucionDao.save(distribucion);
        }
        if (organizacionInicial) {
            generarPartidosDeFase(torneo, listGrupoEquipo, faseDestino);
            torneo.setEstadoTorneo(EstadoTorneo.ACTIVO);
        }
        torneoDao.save(torneo);
    }

    private boolean esFaseEliminatoria(FaseActual fase) {
        return fase == FaseActual.FINAL || fase == FaseActual.SEMIFINAL || fase == FaseActual.CUARTOS ||
                fase == FaseActual.OCTAVOS || fase == FaseActual.DIECISEISAVOS ||
                fase == FaseActual.TREINTAIDOSAVOS;
    }

    private void validarClasificadosEliminatorias(Torneo torneo, List<DtoGrupoEquipo> equiposSiguienteFase) {
        FaseActual faseAnterior = torneo.getFaseTorneo();
        List<Partido> partidos = partidoDao.findByTorneoAndFaseEncuentro(torneo, faseAnterior);
        if (partidos.isEmpty()) {
            throw new IllegalArgumentException("No hay partidos registrados en la fase que se intenta cerrar");
        }
        if (partidos.stream().anyMatch(partido -> partido.getEstadoPartido() != EstadoPartido.TERMINADO)) {
            throw new IllegalArgumentException("Todos los partidos de la llave deben estar terminados antes de avanzar");
        }

        Set<Long> ganadores = obtenerGanadoresFase(torneo, partidos, faseAnterior);
        Set<Long> equiposEnviados = new HashSet<>();
        for (DtoGrupoEquipo equipo : equiposSiguienteFase) {
            if (equipo.getIdEquipo() <= 0 || !equiposEnviados.add(equipo.getIdEquipo())) {
                throw new IllegalArgumentException("La siguiente fase debe incluir equipos válidos sin repetir");
            }
        }
        if (!equiposEnviados.equals(ganadores)) {
            throw new IllegalArgumentException("Solo pueden avanzar los ganadores de todos los enfrentamientos de la fase");
        }
    }

    private Set<Long> obtenerGanadoresFase(Torneo torneo, List<Partido> partidos, FaseActual fase) {
        Set<Long> ganadores = new HashSet<>();
        ModalidadFase modalidad = obtenerModalidadFase(torneo, fase);
        if (modalidad == ModalidadFase.PARTIDO_UNICO) {
            for (Partido partido : partidos) {
                ganadores.add(obtenerGanadorPartido(partido));
            }
            return ganadores;
        }

        Map<Set<Long>, List<Partido>> partidosPorEnfrentamiento = new HashMap<>();
        for (Partido partido : partidos) {
            if (partido.getEquipoLocal() == null || partido.getEquipoVisitante() == null ||
                    partido.getEquipoLocal().getId() == partido.getEquipoVisitante().getId()) {
                throw new IllegalArgumentException("Hay un partido inválido en la fase eliminatoria");
            }
            Set<Long> equipos = Set.of(partido.getEquipoLocal().getId(), partido.getEquipoVisitante().getId());
            partidosPorEnfrentamiento.computeIfAbsent(equipos, llave -> new ArrayList<>()).add(partido);
        }

        for (List<Partido> idaYVuelta : partidosPorEnfrentamiento.values()) {
            if (idaYVuelta.size() != 2) {
                throw new IllegalArgumentException("Cada llave de ida y vuelta debe tener sus dos partidos terminados");
            }
            Partido primero = idaYVuelta.get(0);
            Partido segundo = idaYVuelta.get(1);
            if (primero.getEquipoLocal().getId() != segundo.getEquipoVisitante().getId() ||
                    primero.getEquipoVisitante().getId() != segundo.getEquipoLocal().getId()) {
                throw new IllegalArgumentException("Los partidos de ida y vuelta de una llave no coinciden");
            }
            long equipoPrimero = primero.getEquipoLocal().getId();
            long equipoSegundo = primero.getEquipoVisitante().getId();
            int golesPrimero = primero.getAnotacionesEquipoLocal() + segundo.getAnotacionesEquipoVisitante();
            int golesSegundo = primero.getAnotacionesEquipoVisitante() + segundo.getAnotacionesEquipoLocal();
            if (golesPrimero > golesSegundo) {
                ganadores.add(equipoPrimero);
            } else if (golesSegundo > golesPrimero) {
                ganadores.add(equipoSegundo);
            } else {
                ganadores.add(obtenerGanadorPenaltis(primero, segundo));
            }
        }
        return ganadores;
    }

    private long obtenerGanadorPartido(Partido partido) {
        if (partido.getEquipoLocal() == null || partido.getEquipoVisitante() == null) {
            throw new IllegalArgumentException("Hay un partido sin equipos en la fase eliminatoria");
        }
        if (partido.getAnotacionesEquipoLocal() > partido.getAnotacionesEquipoVisitante()) {
            return partido.getEquipoLocal().getId();
        }
        if (partido.getAnotacionesEquipoVisitante() > partido.getAnotacionesEquipoLocal()) {
            return partido.getEquipoVisitante().getId();
        }
        return obtenerGanadorPenaltis(partido);
    }

    private long obtenerGanadorPenaltis(Partido... partidos) {
        Long ganador = null;
        for (Partido partido : partidos) {
            boolean localGana = partido.getPenaltisEquipoLocal() > partido.getPenaltisEquipoVisitante();
            boolean visitanteGana = partido.getPenaltisEquipoVisitante() > partido.getPenaltisEquipoLocal();
            if (!localGana && !visitanteGana) continue;
            long ganadorPartido = localGana ? partido.getEquipoLocal().getId() : partido.getEquipoVisitante().getId();
            if (ganador != null && ganador != ganadorPartido) {
                throw new IllegalArgumentException("Los penales registrados para la llave son inconsistentes");
            }
            ganador = ganadorPartido;
        }
        if (ganador == null) {
            throw new IllegalArgumentException("La llave está empatada y debe definirse con penales antes de avanzar");
        }
        return ganador;
    }

    @Transactional(readOnly = true)
    public List<GrupoLlave> getGrupoLlave(long idTorneo, FaseActual faseTorneo) {
        Torneo torneo = getById(idTorneo);
        boolean organizacionInicial = torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES ||
                torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES_ACTIVO;
        return participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
                .filter(participacion -> organizacionInicial || participacion.getFaseActual() == faseTorneo)
                .map(participacion -> {
                    GrupoLlave grupo = new GrupoLlave();
                    grupo.setId(participacion.getId());
                    grupo.setEquipo(participacion.getEquipo());
                    grupo.setGrupoLlave(participacion.getFaseActual() == faseTorneo ? participacion.getGrupo() : 0);
                grupo.setGoles(participacion.getGolesFavor());
                grupo.setPartidosJugados(participacion.getPartidosJugados());
                grupo.setPartidosGanados(participacion.getPartidosGanados());
                grupo.setPartidosPerdidos(participacion.getPartidosPerdidos());
                grupo.setPartidosEmpatados(participacion.getPartidosEmpatados());
                grupo.setGolesFavor(participacion.getGolesFavor());
                grupo.setGolesContra(participacion.getGolesContra());
                grupo.setPuntos(participacion.getPuntos());
                grupo.setTorneo(torneo);
                grupo.setFaseTorneo(faseTorneo);
                return grupo;
            }).toList();
    }

    @Transactional
    public List<GrupoLlave> guardarGrupoLlave(long idTorneo, List<DtoGrupoLlave> grupos, long usuarioId) {
        if (grupos == null || grupos.isEmpty() || grupos.stream().anyMatch(grupo -> grupo.getIdEquipo() <= 0 || grupo.getGrupoLlave() <= 0)) {
            throw new IllegalArgumentException("Todos los equipos deben pertenecer a un grupo o llave");
        }
        if (grupos.stream().map(DtoGrupoLlave::getIdEquipo).distinct().count() != grupos.size()) {
            throw new IllegalArgumentException("No se puede asignar el mismo equipo más de una vez");
        }
        Torneo torneo = getById(idTorneo);
        validarPropietario(torneo, usuarioId);
        FaseActual fase = grupos.get(0).getFaseTorneo();
        if (fase == null) {
            fase = torneo.getFaseTorneo();
        }
        boolean primeraFase = torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES ||
                torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES_ACTIVO;
        if (primeraFase && torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS && torneo.getFaseInicioEliminatorias() != null) {
            fase = torneo.getFaseInicioEliminatorias();
        }
        int maximoPorGrupo = fase == FaseActual.FASE_GRUPOS || fase == FaseActual.ELIMINATORIAS_GRUPOS
                ? torneo.getCantidadEquipos() : 2;
        if (grupos.stream().collect(Collectors.groupingBy(DtoGrupoLlave::getGrupoLlave, Collectors.counting()))
                .values().stream().anyMatch(cantidad -> cantidad > maximoPorGrupo)) {
            throw new IllegalArgumentException("La distribución supera la capacidad permitida del grupo o llave");
        }
        List<GrupoLlave> guardados = new ArrayList<>();
        for (DtoGrupoLlave grupo : grupos) {
            Equipo equipo = equipoDao.findById(grupo.getIdEquipo()).orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
            GrupoLlave grupoLlave = new GrupoLlave();
            grupoLlave.setEquipo(equipo);
            grupoLlave.setGrupoLlave(grupo.getGrupoLlave());
            grupoLlave.setTorneo(torneo);
            grupoLlave.setFaseTorneo(fase);
            ParticipacionEquipoTorneo participacion = participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                    .orElseThrow(() -> new IllegalArgumentException("El equipo no está inscrito en este torneo"));
            if (!"ACEPTADO".equals(participacion.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden organizar equipos aceptados");
            }
            if (participacion.getFaseActual() != fase || participacion.getGrupo() != grupo.getGrupoLlave()) {
                reiniciarEstadisticas(participacion);
            }
            participacion.setGrupo(grupo.getGrupoLlave());
            participacion.setFaseActual(fase);
            participacionEquipoDao.save(participacion);
            guardados.add(grupoLlave);
        }
        long equiposAceptados = participacionEquipoDao.countByTorneoAndEstado(torneo, "ACEPTADO");
        boolean distribucionCompleta = guardados.size() == equiposAceptados;
        if (primeraFase) {
            if (distribucionCompleta) crearPartidosDesdeGrupoLlave(torneo, guardados, fase);
            torneo.setFaseTorneo(fase);
            torneo.setEstadoTorneo(distribucionCompleta ? EstadoTorneo.ACTIVO : EstadoTorneo.INSCRIPCIONES_ACTIVO);
            torneoDao.save(torneo);
        }
        return guardados;
    }

    private void crearPartidosDesdeGrupoLlave(Torneo torneo, List<GrupoLlave> grupos, FaseActual fase) {
        for (int i = 0; i < grupos.size(); i++) {
            for (int j = i + 1; j < grupos.size(); j++) {
                GrupoLlave primero = grupos.get(i);
                GrupoLlave segundo = grupos.get(j);
                if (primero.getGrupoLlave() != segundo.getGrupoLlave()) {
                    continue;
                }
                crearPartidoSiNoExiste(torneo, fase, primero.getEquipo(), segundo.getEquipo(), primero.getGrupoLlave());
                ModalidadFase modalidad = fase == FaseActual.FASE_GRUPOS || fase == FaseActual.ELIMINATORIAS_GRUPOS
                        ? torneo.getModalidadGrupos() : torneo.getModalidadEliminatorias();
                if (modalidad == ModalidadFase.IDA_VUELTA) {
                    crearPartidoSiNoExiste(torneo, fase, segundo.getEquipo(), primero.getEquipo(), segundo.getGrupoLlave());
                }
            }
        }
    }

    private void generarPartidosDeFase(Torneo torneo, List<DtoGrupoEquipo> equipos, FaseActual fase) {
        for (int i = 0; i < equipos.size(); i++) {
            for (int j = i + 1; j < equipos.size(); j++) {
                DtoGrupoEquipo primero = equipos.get(i);
                DtoGrupoEquipo segundo = equipos.get(j);
                boolean mismoGrupo = primero.getGrupo() == segundo.getGrupo();
                if (!mismoGrupo) {
                    continue;
                }
                Equipo equipoLocal = equipoDao.findById(primero.getIdEquipo()).orElseThrow();
                Equipo equipoVisitante = equipoDao.findById(segundo.getIdEquipo()).orElseThrow();
                crearPartidoSiNoExiste(torneo, fase, equipoLocal, equipoVisitante, primero.getGrupo());
                if (obtenerModalidadFase(torneo, fase) == ModalidadFase.IDA_VUELTA) {
                    crearPartidoSiNoExiste(torneo, fase, equipoVisitante, equipoLocal, segundo.getGrupo());
                }
            }
        }
    }

    private ModalidadFase obtenerModalidadFase(Torneo torneo, FaseActual fase) {
        ModalidadFase modalidad = fase == FaseActual.FASE_GRUPOS || fase == FaseActual.ELIMINATORIAS_GRUPOS
                ? torneo.getModalidadGrupos()
                : torneo.getModalidadEliminatorias();
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad de la fase del torneo es obligatoria");
        }
        return modalidad;
    }

    private void crearPartido(Torneo torneo, FaseActual fase, Equipo local, Equipo visitante, int grupo) {
        Partido partido = new Partido();
        partido.setTorneo(torneo);
        partido.setGrupo(grupo);
        partido.setEstadoPartido(EstadoPartido.PENDIENTE);
        partido.setFaseEncuentro(fase);
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partidoDao.save(partido);
    }

    private void crearPartidoSiNoExiste(Torneo torneo, FaseActual fase, Equipo local, Equipo visitante, int grupo) {
        if (partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo, local, visitante, fase) == null) {
            crearPartido(torneo, fase, local, visitante, grupo);
        }
    }

    private void reiniciarEstadisticas(ParticipacionEquipoTorneo participacion) {
        participacion.setPuntos(0);
        participacion.setPartidosJugados(0);
        participacion.setPartidosGanados(0);
        participacion.setPartidosPerdidos(0);
        participacion.setPartidosEmpatados(0);
        participacion.setGolesFavor(0);
        participacion.setGolesContra(0);
    }

    private void validarDatosObligatoriosTorneo(Torneo torneo) {
        if (torneo == null) {
            throw new IllegalArgumentException("El torneo es obligatorio");
        }
        if (torneo.getNombre() == null || torneo.getNombre().isBlank() || torneo.getNombre().trim().length() < 6) {
            throw new IllegalArgumentException("El nombre del torneo debe tener al menos 6 caracteres");
        }
        if (torneo.getUbicacion() == null || torneo.getUbicacion().isBlank()) {
            throw new IllegalArgumentException("La ubicación del torneo es obligatoria");
        }
        if (torneo.getCiudad() == null || torneo.getCiudad().getId() <= 0) {
            throw new IllegalArgumentException("La ciudad del torneo es obligatoria");
        }
        if (torneo.getDeporte() == null || torneo.getDeporte().getId() <= 0) {
            throw new IllegalArgumentException("El deporte del torneo es obligatorio");
        }
        if (torneo.getCantidadEquipos() <= 0) {
            throw new IllegalArgumentException("La cantidad de equipos es obligatoria");
        }
        if (torneo.getModalidadTorneo() == null) {
            throw new IllegalArgumentException("La modalidad del torneo es obligatoria");
        }
        boolean tieneFaseGrupos = torneo.getModalidadTorneo() == ModalidadTorneo.GRUPOS ||
                torneo.getModalidadTorneo() == ModalidadTorneo.LIGA ||
                torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS_GRUPOS;
        if (tieneFaseGrupos && torneo.getModalidadGrupos() == null) {
            throw new IllegalArgumentException("La modalidad de grupos es obligatoria");
        }
        if (tieneFaseGrupos && torneo.getCantidadGrupos() <= 0) {
            throw new IllegalArgumentException("La cantidad de grupos es obligatoria");
        }
        if (tieneFaseGrupos && torneo.getCantidadGrupos() > torneo.getCantidadEquipos()) {
            throw new IllegalArgumentException("La cantidad de grupos no puede superar los equipos por grupo");
        }
        if (torneo.getDuracionMinutos() <= 0) {
            throw new IllegalArgumentException("La duración del partido es obligatoria");
        }
        if (torneo.getValorInscripcion() < 0) {
            throw new IllegalArgumentException("El valor de inscripción no puede ser negativo");
        }
        if (torneo.getModoCambioJugador() == null) {
            throw new IllegalArgumentException("El modo de cambio de jugadores es obligatorio");
        }
        if (torneo.getModoCambioJugador() == ModoCambioJugador.LIMITADOS && torneo.getMaximoCambios() <= 0) {
            throw new IllegalArgumentException("La cantidad máxima de cambios debe ser mayor que cero");
        }
        if (torneo.getModalidadEliminatorias() == null) {
            throw new IllegalArgumentException("La modalidad de eliminatorias es obligatoria");
        }
        if (torneo.getFaseTorneo() == null) {
            throw new IllegalArgumentException("La fase actual del torneo es obligatoria");
        }
        if (torneo.getModalidadTorneo() != ModalidadTorneo.LIGA && torneo.getFaseInicioEliminatorias() == null) {
            throw new IllegalArgumentException("La fase inicial de eliminatorias es obligatoria");
        }
        if (torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS_GRUPOS &&
                (torneo.getCantidadGruposEliminatoriaGrupos() <= 0 || torneo.getCantidadEquiposEliminatoriaGrupos() <= 0)) {
            throw new IllegalArgumentException("La cantidad de grupos y equipos de la fase previa es obligatoria");
        }
    }

    public List<DtoDistribucionEquipo> getDistribucion(long idTorneo) {
        Torneo torneo = getById(idTorneo);
        List<DistribucionEquipoTorneo> distribuciones = distribucionDao.findByTorneoAndFase(torneo, torneo.getFaseTorneo());
        Map<Long, Integer> gruposPorEquipo = distribuciones.stream()
                .collect(Collectors.toMap(
                        distribucion -> distribucion.getEquipo().getId(),
                        DistribucionEquipoTorneo::getGrupo,
                        (grupoAnterior, grupoActual) -> grupoActual));
        return participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
                .filter(participacion -> participacion.getFaseActual() != null)
                .map(equipo -> {
            DtoDistribucionEquipo dto = new DtoDistribucionEquipo();
            dto.setIdEquipo(equipo.getEquipo().getId());
            dto.setNombreEquipo(equipo.getEquipo().getNombre());
            dto.setFase(torneo.getFaseTorneo());
            dto.setGrupo(gruposPorEquipo.getOrDefault(equipo.getEquipo().getId(), 0));
            return dto;
        }).toList();
    }
    public void cambiarFaseTorneo(long idTorneo, long usuarioId) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        validarPropietario(torneo, usuarioId);
        List<EstadoPartido> listEP = new ArrayList<>();
        listEP.add(EstadoPartido.APLASADO);
        listEP.add(EstadoPartido.PENDIENTE);
        listEP.add(EstadoPartido.PROGRAMADO);
        listEP.add(EstadoPartido.EN_PROCESO);
        listEP.add(EstadoPartido.SUSPENDIDO);
        int partidosFase = partidoDao.countByTorneoAndFaseEncuentro(torneo, torneo.getFaseTorneo());
        int partidosSinTerminar = partidoDao.countByEstadoPartidoInAndTorneoAndFaseEncuentro(listEP, torneo, torneo.getFaseTorneo());
        if (partidosFase == 0) {
            throw  new IllegalArgumentException("No hay partidos en esta fase");
        } else if (partidosSinTerminar == 0 && !torneo.getFaseTorneo().equals(FaseActual.FINAL) && !torneo.getModalidadTorneo().equals(ModalidadTorneo.LIGA)) {
            return;
        }
        throw  new IllegalArgumentException("Todavia hay partidos sin terminar");
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reglamento getReglamento(long id) {
        Torneo torneo = torneoDao.findById(id).orElse(null);
        if (torneo == null) {
            throw  new IllegalArgumentException("El torneo no existe");
        }
        Reglamento reglamento = reglamentoDao.findByTorneo(torneo);
        if (reglamento == null) {
            throw  new IllegalArgumentException("El reglamento no existe");
        }
        return reglamento;
    }
    private void validarFinalizacionTorneo(Torneo torneoDB) {
        List<EstadoPartido> estadosAbiertos = List.of(
                EstadoPartido.PENDIENTE,
                EstadoPartido.PROGRAMADO,
                EstadoPartido.EN_PROCESO,
                EstadoPartido.APLASADO,
                EstadoPartido.SUSPENDIDO
        );

        boolean ultimaFase = torneoDB.getModalidadTorneo() == ModalidadTorneo.LIGA
                ? torneoDB.getFaseTorneo() == FaseActual.FASE_GRUPOS
                : torneoDB.getFaseTorneo() == FaseActual.FINAL;
        if (!ultimaFase) {
            throw new IllegalArgumentException("El torneo debe completar todas las fases de su modalidad antes de finalizar");
        }

        int partidosFase = partidoDao.countByTorneoAndFaseEncuentro(torneoDB, torneoDB.getFaseTorneo());
        int partidosAbiertos = partidoDao.countByTorneoAndEstadoPartidoIn(torneoDB, estadosAbiertos);
        if (partidosFase == 0 || partidosAbiertos > 0) {
            throw new IllegalArgumentException("Solo se puede finalizar el torneo cuando la última fase ya fue jugada completamente");
        }
    }

    public Torneo update(Torneo torneo, long usuarioId) {
        if (torneo == null || torneo.getId() == null) {
            throw new IllegalArgumentException("Debe indicar el torneo a actualizar");
        }
        Optional<Torneo> optTorneo = torneoDao.findById(torneo.getId());
        if (!optTorneo.isPresent()) {
            throw  new IllegalArgumentException("No existe Torneo con id: " + torneo.getId());
        }
        Torneo torneoDB = optTorneo.get();
        validarPropietario(torneoDB, usuarioId);
        if (torneoDB.getEstadoTorneo() == EstadoTorneo.FINALIZADO) {
            throw new IllegalArgumentException("El torneo finalizado no puede modificarse");
        }
        if (torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES && torneoDB.getEstadoTorneo() != EstadoTorneo.INSCRIPCIONES) {
            throw new IllegalArgumentException("No se puede devolver un torneo a INSCRIPCIONES desde otro estado");
        }
        if (torneo.getEstadoTorneo() == EstadoTorneo.FINALIZADO) {
            validarFinalizacionTorneo(torneoDB);
        }
        validarDatosObligatoriosTorneo(torneo);
        torneoDB.setEstadoTorneo(torneo.getEstadoTorneo());
        torneoDB.setModalidadTorneo(torneo.getModalidadTorneo());
        torneoDB.setNombre(torneo.getNombre());
        torneoDB.setCantidadEquipos(torneo.getCantidadEquipos());
        torneoDB.setCantidadGrupos(torneo.getCantidadGrupos());
        torneoDB.setValorInscripcion(torneo.getValorInscripcion());
        torneoDB.setUbicacion(torneo.getUbicacion());
        torneoDB.setCiudad(torneo.getCiudad());
        torneoDB.setDeporte(torneo.getDeporte());
        torneoDB.setModalidadGrupos(torneo.getModalidadGrupos());
        torneoDB.setDuracionMinutos(torneo.getDuracionMinutos());
        torneoDB.setFaseTorneo(torneo.getFaseTorneo());
        torneoDB.setFaseInicioEliminatorias(torneo.getFaseInicioEliminatorias());
        torneoDB.setModalidadEliminatorias(torneo.getModalidadEliminatorias());
        torneoDB.setCantidadGruposEliminatoriaGrupos(torneo.getCantidadGruposEliminatoriaGrupos());
        torneoDB.setCantidadEquiposEliminatoriaGrupos(torneo.getCantidadEquiposEliminatoriaGrupos());
        torneoDB.setModalidadEliminatoriasGrupos(torneo.getModalidadEliminatoriasGrupos());
        torneoDB.setModoCambioJugador(torneo.getModoCambioJugador());
        torneoDB.setMaximoCambios(torneo.getMaximoCambios());
        torneoDB.setAmarillasParaSuspension(torneo.getAmarillasParaSuspension());
        torneoDB.setPartidosSuspensionRoja(torneo.getPartidosSuspensionRoja());
        torneoDB.setExpulsionPermanenteTorneo(torneo.isExpulsionPermanenteTorneo());
        return torneoDao.save(torneoDB);
    }
    public void delete(long id, long usuarioId) {
        Optional<Torneo> optTorneo = torneoDao.findById(id);
        if (optTorneo.isPresent()) {
            validarPropietario(optTorneo.get(), usuarioId);
            torneoDao.delete(optTorneo.get());
        }
    }
}
