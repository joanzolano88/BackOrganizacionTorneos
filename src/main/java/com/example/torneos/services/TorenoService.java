package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.DTO.DtoOptionTorneo;
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

    public Torneo save(Torneo torneo, Reglamento reglamento) {
        torneo.setEstadoTorneo(EstadoTorneo.INSCRIPCIONES);
        FaseActual[] faseActuals = FaseActual.values();
        if (torneo.getCantidadEquipos() >= Math.pow(2,torneo.getFaseInicioEliminatorias().ordinal())) {

        }
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
        }
        torneoDao.save(torneo);
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
    public Torneo update(Torneo torneo) {
        Optional<Torneo> optTorneo = torneoDao.findById(torneo.getId());
        if (!optTorneo.isPresent()) {
            throw  new IllegalArgumentException("No existe Torneo con id: " + torneo.getId());
        }
        Torneo torneoDB = optTorneo.get();
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
