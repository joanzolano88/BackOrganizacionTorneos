package com.example.torneos.services;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
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

    public Equipo save(Equipo equipo) {
        List<Equipo> listaEquiposDelegado = equipoDao.findByDelegado(equipo.getDelegado());

        if (equipo.getTorneo().equals(null)) {
            throw  new IllegalArgumentException("Torneo nulo");
        }
        int cantEquipos = equipoDao.countByTorneoAndGrupoAndFaseActual(equipo.getTorneo(), equipo.getGrupo(), equipo.getTorneo().getFaseTorneo());
        if (equipo.getTorneo().getCantidadEquipos() == cantEquipos) {
            throw  new IllegalArgumentException("EL grupo esta completo");
        }
        for (Equipo equipoDelegado : listaEquiposDelegado) {
            if (equipoDelegado != null && equipoDelegado.getTorneo().equals(equipo.getTorneo())) {
                throw  new IllegalArgumentException("El delegado ya pertenece a un equipo");
            }
        }
        equipo.setFaseActual(equipo.getTorneo().getFaseTorneo());
        return equipoDao.save(equipo);
    }
    public List<Equipo> getAll() {
        return equipoDao.findAll();
    }
    public Equipo getById(Long id) {
        Equipo equipo = equipoDao.findById(id).orElse(null);
        if (equipo.equals(null)) {
            throw  new IllegalArgumentException("El Equipo no existe");
        }
        return equipo;
    }
    public List<Equipo> getByTorneo(Long id) {
        Torneo torneo = torneoDao.findById(id).get();
        List<Equipo> listaEquipos = equipoDao.findByTorneo(torneo);
        if (listaEquipos.equals(null)) {
            throw  new IllegalArgumentException("No hay equipos");
        }
        return listaEquipos;
    }
    public List<Equipo> getByTorneoModalidad(Long id, ModalidadTorneo modalidadTorneo) {
        Torneo torneo = torneoDao.findById(id).get();
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
        Torneo torneo = torneoDao.findById(id).get();
        List<Equipo> listaEquipos = equipoDao.findByTorneoAndFaseActual(torneo, faseActual);
        if (listaEquipos.size() == 0) {
            throw  new IllegalArgumentException("No hay equipos en esta fase");
        }
        return listaEquipos;
    }
    public Equipo update(Equipo equipo) {
        Optional<Equipo> optEquipo = equipoDao.findById(equipo.getId());
        if (!optEquipo.isPresent()) {
            throw  new IllegalArgumentException("El Equipo no existe");
        }
        Equipo equipoDB = optEquipo.get();
        if (!equipoDB.getDelegado().equals(equipo.getDelegado()) && equipoDao.existsByDelegado(equipo.getDelegado())) {
            throw  new IllegalArgumentException("El delegado ya pertenece a un equipo");
        }
        equipoDB.setNombre(equipo.getNombre());
        equipoDB.setDelegado(equipo.getDelegado());
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
