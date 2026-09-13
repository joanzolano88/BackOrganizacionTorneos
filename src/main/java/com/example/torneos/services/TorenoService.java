package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.DTO.DtoGrupoLlave;
import com.example.torneos.DTO.DtoOptionTorneo;
import com.example.torneos.DTO.DtoDistribucionEquipo;
import com.example.torneos.dao.*;
import com.example.torneos.entities.*;
import com.example.torneos.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private DistribucionEquipoTorneoDao distribucionDao;
    @Autowired
    private GrupoLlaveDao grupoLlaveDao;

    private String normalizarUbicacion(String ubicacion) {
        return Normalizer.normalize(ubicacion == null ? "" : ubicacion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    public Torneo save(Torneo torneo, Reglamento reglamento) {
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
    public List<DtoOptionTorneo> getOptionAll() {
        List<DtoOptionTorneo> torneoList = torneoDao.findAll().stream().map(t -> new DtoOptionTorneo(t.getNombre(), t.getId())).toList();

        return torneoList;
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
        if (usuario.equals(null)) {
            throw  new IllegalArgumentException("El Usuario no esxiste");
        }
        List<Torneo> torneoList = torneoDao.findByEncargadoTorneo(usuario);
        return torneoList;
    }
    public void cabiarFaseTorneo(List<DtoGrupoEquipo> listGrupoEquipo, long idTorneo) {
        if (listGrupoEquipo == null || listGrupoEquipo.isEmpty()) {
            throw new IllegalArgumentException("Debes organizar al menos un equipo");
        }
        Torneo torneo = torneoDao.findById(idTorneo).get();
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
        distribucionDao.deleteAll(distribucionDao.findByTorneoAndFase(torneo, faseDestino));
        torneo.setFaseTorneo(faseDestino);
        for (int i = 0; i < listGrupoEquipo.size(); i++) {
            DtoGrupoEquipo grupoEquipo = listGrupoEquipo.get(i);
            Equipo equipo = equipoDao.findById(grupoEquipo.getIdEquipo()).get();
            if (torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS)) {
                equipo.setGrupo(grupoEquipo.getGrupo());
            } else {
                for (int j = i + 1; j < listGrupoEquipo.size(); j++) {
                    DtoGrupoEquipo grupoEquipo2 = listGrupoEquipo.get(j);
                    if (grupoEquipo.getIdEquipo() != grupoEquipo2.getIdEquipo() &&
                            grupoEquipo.getGrupo() == grupoEquipo2.getGrupo()) {
                        Partido partido = new Partido();
                        Equipo equipo2 = equipoDao.findById(grupoEquipo2.getIdEquipo()).get();
                        partido.setTorneo(torneo);
                        partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                        partido.setFaseEncuentro(torneo.getFaseTorneo());
                        partido.setEquipoLocal(equipo);
                        partido.setEquipoVisitante(equipo2);
                        partidoDao.save(partido);
                        if (torneo.getModalidadGrupos().equals(ModalidadFase.IDA_VUELTA)) {
                            Partido partido2 = new Partido();
                            partido2.setTorneo(torneo);
                            partido2.setEstadoPartido(EstadoPartido.PENDIENTE);
                            partido2.setFaseEncuentro(torneo.getFaseTorneo());
                            partido2.setEquipoLocal(equipo2);
                            partido2.setEquipoVisitante(equipo);
                            partidoDao.save(partido2);
                        }
                    }
                }
            }
            equipo.setFaseActual(faseDestino);
            equipoDao.save(equipo);
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

    public List<GrupoLlave> getGrupoLlave(long idTorneo, FaseActual faseTorneo) {
        Torneo torneo = getById(idTorneo);
        List<GrupoLlave> grupos = grupoLlaveDao.findByTorneoAndFaseTorneoOrderByGrupoLlaveAscPuntosDescGolesFavorDesc(torneo, faseTorneo);
        Map<Long, GrupoLlave> gruposPorEquipo = grupos.stream()
                .collect(Collectors.toMap(grupo -> grupo.getEquipo().getId(), grupo -> grupo, (anterior, actual) -> actual));
        return equipoDao.findByTorneo(torneo).stream()
                .filter(equipo -> equipo.getFaseActual() != null)
                .map(equipo -> {
                    GrupoLlave grupo = gruposPorEquipo.getOrDefault(equipo.getId(), new GrupoLlave());
                    grupo.setEquipo(equipo);
                    if (!gruposPorEquipo.containsKey(equipo.getId())) {
                        grupo.setGrupoLlave(0);
                    }
                    grupo.setTorneo(torneo);
                    grupo.setFaseTorneo(faseTorneo);
                    return grupo;
                }).toList();
    }

    @Transactional
    public List<GrupoLlave> guardarGrupoLlave(long idTorneo, List<DtoGrupoLlave> grupos) {
        if (grupos == null || grupos.isEmpty() || grupos.stream().anyMatch(grupo -> grupo.getIdEquipo() <= 0 || grupo.getGrupoLlave() <= 0)) {
            throw new IllegalArgumentException("Todos los equipos deben pertenecer a un grupo o llave");
        }
        Torneo torneo = getById(idTorneo);
        FaseActual fase = grupos.get(0).getFaseTorneo();
        if (fase == null) {
            fase = torneo.getFaseTorneo();
        }
        boolean primeraFase = torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES ||
                torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES_ACTIVO;
        if (primeraFase && torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS && torneo.getFaseInicioEliminatorias() != null) {
            fase = torneo.getFaseInicioEliminatorias();
        }
        grupoLlaveDao.deleteByTorneoAndFaseTorneo(torneo, fase);
        List<GrupoLlave> guardados = new ArrayList<>();
        for (DtoGrupoLlave grupo : grupos) {
            Equipo equipo = equipoDao.findById(grupo.getIdEquipo()).orElseThrow(() -> new IllegalArgumentException("El equipo no existe"));
            GrupoLlave grupoLlave = new GrupoLlave();
            grupoLlave.setEquipo(equipo);
            grupoLlave.setGrupoLlave(grupo.getGrupoLlave());
            grupoLlave.setTorneo(torneo);
            grupoLlave.setFaseTorneo(fase);
            guardados.add(grupoLlaveDao.save(grupoLlave));
        }
        long equiposAceptados = equipoDao.findByTorneo(torneo).stream()
            .filter(equipo -> equipo.getFaseActual() != null)
            .count();
        boolean distribucionCompleta = guardados.size() == equiposAceptados;
        if (primeraFase) {
            crearPartidosDesdeGrupoLlave(torneo, guardados, fase);
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
                crearPartido(torneo, fase, equipoLocal, equipoVisitante);
                ModalidadFase modalidad = fase == FaseActual.FASE_GRUPOS || fase == FaseActual.ELIMINATORIAS_GRUPOS
                        ? torneo.getModalidadGrupos()
                        : torneo.getModalidadEliminatorias();
                if (modalidad == ModalidadFase.IDA_VUELTA) {
                    crearPartido(torneo, fase, equipoVisitante, equipoLocal);
                }
            }
        }
    }

    private void crearPartido(Torneo torneo, FaseActual fase, Equipo local, Equipo visitante) {
        crearPartido(torneo, fase, local, visitante, local.getGrupo());
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

    public List<DtoDistribucionEquipo> getDistribucion(long idTorneo) {
        Torneo torneo = getById(idTorneo);
        List<DistribucionEquipoTorneo> distribuciones = distribucionDao.findByTorneoAndFase(torneo, torneo.getFaseTorneo());
        Map<Long, Integer> gruposPorEquipo = distribuciones.stream()
                .collect(Collectors.toMap(
                        distribucion -> distribucion.getEquipo().getId(),
                        DistribucionEquipoTorneo::getGrupo,
                        (grupoAnterior, grupoActual) -> grupoActual));
        return equipoDao.findByTorneo(torneo).stream()
                .filter(equipo -> equipo.getFaseActual() != null)
                .map(equipo -> {
            DtoDistribucionEquipo dto = new DtoDistribucionEquipo();
            dto.setIdEquipo(equipo.getId());
            dto.setNombreEquipo(equipo.getNombre());
            dto.setFase(torneo.getFaseTorneo());
            dto.setGrupo(gruposPorEquipo.getOrDefault(equipo.getId(), 0));
            return dto;
        }).toList();
    }
    public void cambiarFaseTorneo(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
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
        if (torneo.equals(null)) {
            throw  new IllegalArgumentException("El Torneo no esxiste");
        }
        Reglamento reglamento = reglamentoDao.findByTorneo(torneo);
        /*if (reglamento.equals(null)) {
            throw  new IllegalArgumentException("El Reglamento no esxiste");
        }*/
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

    public Torneo update(Torneo torneo) {
        Optional<Torneo> optTorneo = torneoDao.findById(torneo.getId());
        if (!optTorneo.isPresent()) {
            throw  new IllegalArgumentException("No existe Torneo con id: " + torneo.getId());
        }
        Torneo torneoDB = optTorneo.get();
        if (torneoDB.getEstadoTorneo() == EstadoTorneo.FINALIZADO) {
            throw new IllegalArgumentException("El torneo finalizado no puede modificarse");
        }
        if (torneo.getEstadoTorneo() == EstadoTorneo.INSCRIPCIONES && torneoDB.getEstadoTorneo() != EstadoTorneo.INSCRIPCIONES) {
            throw new IllegalArgumentException("No se puede devolver un torneo a INSCRIPCIONES desde otro estado");
        }
        if (torneo.getEstadoTorneo() == EstadoTorneo.FINALIZADO) {
            validarFinalizacionTorneo(torneoDB);
        }
        torneoDB.setEstadoTorneo(torneo.getEstadoTorneo());
        torneoDB.setModalidadTorneo(torneo.getModalidadTorneo());
        torneoDB.setNombre(torneo.getNombre());
        torneoDB.setCantidadEquipos(torneo.getCantidadEquipos());
        torneoDB.setCantidadGrupos(torneo.getCantidadGrupos());
        torneoDB.setValorInscripcion(torneo.getValorInscripcion());
        return torneoDao.save(torneoDB);
    }
    public void delete(long id) {
        Optional<Torneo> optTorneo = torneoDao.findById(id);
        if (optTorneo.isPresent()) {
            torneoDao.delete(optTorneo.get());
        }
    }
}
