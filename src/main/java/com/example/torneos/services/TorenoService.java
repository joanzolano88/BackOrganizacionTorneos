package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
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
import java.util.Optional;
import java.text.Normalizer;

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
        Torneo torneo = torneoDao.findById(idTorneo).get();
        distribucionDao.deleteAll(distribucionDao.findByTorneoAndFase(torneo, listGrupoEquipo.get(0).getFaseActual()));
        torneo.setFaseTorneo(listGrupoEquipo.get(0).getFaseActual());
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
            equipo.setFaseActual(grupoEquipo.getFaseActual());
            equipoDao.save(equipo);
            DistribucionEquipoTorneo distribucion = new DistribucionEquipoTorneo();
            distribucion.setTorneo(torneo);
            distribucion.setEquipo(equipo);
            distribucion.setFase(grupoEquipo.getFaseActual());
            distribucion.setGrupo(grupoEquipo.getGrupo());
            distribucionDao.save(distribucion);
        }
        torneoDao.save(torneo);
    }

    public List<DtoDistribucionEquipo> getDistribucion(long idTorneo) {
        Torneo torneo = getById(idTorneo);
        List<DistribucionEquipoTorneo> distribuciones = distribucionDao.findByTorneoAndFase(torneo, torneo.getFaseTorneo());
        if (distribuciones.isEmpty()) {
            return equipoDao.findByTorneoAndFaseActual(torneo, torneo.getFaseTorneo()).stream().map(equipo -> {
                DtoDistribucionEquipo dto = new DtoDistribucionEquipo();
                dto.setIdEquipo(equipo.getId());
                dto.setNombreEquipo(equipo.getNombre());
                dto.setFase(torneo.getFaseTorneo());
                dto.setGrupo(equipo.getGrupo());
                return dto;
            }).toList();
        }
        return distribuciones.stream().map(distribucion -> {
            DtoDistribucionEquipo dto = new DtoDistribucionEquipo();
            dto.setIdEquipo(distribucion.getEquipo().getId());
            dto.setNombreEquipo(distribucion.getEquipo().getNombre());
            dto.setFase(distribucion.getFase());
            dto.setGrupo(distribucion.getGrupo());
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
