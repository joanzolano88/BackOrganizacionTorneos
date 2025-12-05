package com.example.torneos.services;

import com.example.torneos.DTO.DtoResulatoLlave;
import com.example.torneos.dao.CanchaDao;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadFase;
import com.example.torneos.enums.ModalidadTorneo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartidoService {
    @Autowired
    private PartidoDao partidoDao;
    @Autowired
    private EquipoDao equipoDao;
    @Autowired
    private TorneoDao torneoDao;
    @Autowired
    private CanchaDao canchaDao;

    public Partido save(Partido partido) {
        return partidoDao.save(partido);
    }
    public List<List<Partido>> getTorneo(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        List<List<Partido>> partidosTorneo = new ArrayList<>();
        for (int i = 1; i <= torneo.getCantidadGrupos(); i++) {
            List<Partido> partidosGrupos = partidoDao.findByGrupoAndTorneo(i, torneo);
            partidosGrupos.sort(Comparator.comparing(Partido::getEstadoPartido));
            partidosTorneo.add(partidosGrupos);
        }
        return partidosTorneo;
    }
    public List<List<Partido>> getTorneoFaseActual(long idTorneo, FaseActual faseActual) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        List<List<Partido>> partidosTorneo = new ArrayList<>();

        List<Partido> partidosGrupos;
            if (faseActual.equals(FaseActual.FASE_GRUPOS) || faseActual.equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                for (int i = 1; i <= torneo.getCantidadGrupos(); i++) {
                    partidosGrupos = partidoDao.findByGrupoAndTorneoAndFaseEncuentro(i, torneo, faseActual);
                    partidosGrupos.sort(Comparator.comparing(Partido::getEstadoPartido));
                    partidosTorneo.add(partidosGrupos);
                }
            } else {
                partidosGrupos = partidoDao.findByTorneoAndFaseEncuentro(torneo, faseActual);
                for (int i = 0; i < faseActual.ordinal(); i++) {
                    Partido partido = partidosGrupos.get(i);
                    List<Partido> listPartido = new ArrayList<>();
                    listPartido.add(partido);
                    if (torneo.getModalidadGrupos().equals(ModalidadFase.IDA_VUELTA)) {
                        for (int j = i; j < partidosGrupos.size(); j++) {
                            Partido partido2 = partidosGrupos.get(j);
                            if (partido.getEquipoLocal().equals(partido2.getEquipoVisitante()) &&
                                    partido2.getEquipoVisitante().equals(partido.getEquipoLocal())) {
                                listPartido.add(partido2);
                                partidosGrupos.remove(j);
                            }
                        }
                    }
                    partidosTorneo.add(listPartido);
                }
            }
        List<Partido> p = partidoDao.findByTorneo(torneo);
        return partidosTorneo;
    }
    public List<Partido> getPartidosEstado(EstadoPartido estado) {
        List<Partido> listaPartidos = partidoDao.findByEstadoPartido(estado);
        return listaPartidos;
    }
    public List<Partido> getPartidosEliminatorias(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        List<FaseActual> listaFaseActual = new ArrayList<>();
        listaFaseActual.add(FaseActual.TREINTAIDOSAVOS);
        listaFaseActual.add(FaseActual.DIECISEISAVOS);
        listaFaseActual.add(FaseActual.OCTAVOS);
        listaFaseActual.add(FaseActual.CUARTOS);
        listaFaseActual.add(FaseActual.SEMIFINAL);
        listaFaseActual.add(FaseActual.FINAL);
        List<Partido> listaPartidos = partidoDao.findByTorneoAndFaseEncuentroIn(torneo, listaFaseActual);
        if (listaPartidos.size() == 0) {
            throw  new IllegalArgumentException("No hay equipos en esta fase");
        }
        return listaPartidos;
    }
    public List<Partido> getPartidosFechaTorneo(long fecha, long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fecha), ZoneId.systemDefault());
        List<EstadoPartido> listEP = new ArrayList<>();
        listEP.add(EstadoPartido.PROGRAMADO);
        listEP.add(EstadoPartido.EN_PROCESO);
        listEP.add(EstadoPartido.TERMINADO);
        List<Partido> listaPartidos = partidoDao.findByEstadoPartidoInAndTorneoAndFechaPartidoBetween(listEP,torneo, localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0), localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(0));

        return listaPartidos;
    }
    public List<Partido> generarPartidos(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        List<Equipo> equipoList = equipoDao.findByTorneoAndFaseActual(torneo, torneo.getFaseTorneo());
        List<Partido> partidoList = new ArrayList<>();
        for (int i = 0; i < (equipoList.size() - 1); i++) {
            for (int j = (i + 1); j < equipoList.size(); j++) {
                if ((torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && (torneo.getModalidadTorneo().equals(ModalidadTorneo.GRUPOS) || torneo.getModalidadTorneo().equals(ModalidadTorneo.ELIMINATORIAS_GRUPOS)))
                        || torneo.getFaseTorneo().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                    if (equipoList.get(i).getGrupo() == equipoList.get(j).getGrupo()) {
                        establecerPartido(torneo, equipoList, partidoList, i, j);
                    }
                } else if (torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && torneo.getModalidadTorneo().equals(ModalidadTorneo.LIGA)) {
                    establecerPartido(torneo, equipoList, partidoList, i, j);
                } else if (!torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && !torneo.getFaseTorneo().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                    establecerPartido(torneo, equipoList, partidoList, i, j);
                }
            }
        }
        return partidoDao.findAll();
    }
    private void establecerPartido(Torneo torneo,List<Equipo> equipoList,List<Partido> partidoList, int equipo1, int equipo2) {
        Partido partidoExiste = partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo,equipoList.get(equipo1), equipoList.get(equipo2), torneo.getFaseTorneo());
        Partido partidoExiste2 = partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo,equipoList.get(equipo2), equipoList.get(equipo1), torneo.getFaseTorneo());
        Partido partido = new Partido();
        if (torneo.getModalidadGrupos().equals(ModalidadFase.PARTIDO_UNICO)) {
            if (partidoExiste == null && partidoExiste2 == null) {
                partido.setEquipoLocal(equipoList.get(equipo1));
                partido.setEquipoVisitante(equipoList.get(equipo2));
                partido.setTorneo(torneo);
                partido.setGrupo(partido.getEquipoLocal().getGrupo());
                partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                partido.setFaseEncuentro(torneo.getFaseTorneo());
                partido = partidoDao.save(partido);
                partidoList.add(partido);
            }
        } else if (torneo.getModalidadGrupos().equals(ModalidadFase.IDA_VUELTA)) {
            if (partidoExiste == null) {
                partido.setEquipoLocal(equipoList.get(equipo1));
                partido.setEquipoVisitante(equipoList.get(equipo2));
                partido.setTorneo(torneo);
                partido.setGrupo(partido.getEquipoLocal().getGrupo());
                partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                partido.setFaseEncuentro(torneo.getFaseTorneo());
                partido = partidoDao.save(partido);
                partidoList.add(partido);
            }
            if (partidoExiste2 == null) {
                partido = new Partido();
                partido.setEquipoLocal(equipoList.get(equipo2));
                partido.setEquipoVisitante(equipoList.get(equipo1));
                partido.setTorneo(torneo);
                partido.setGrupo(partido.getEquipoLocal().getGrupo());
                partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                partido.setFaseEncuentro(torneo.getFaseTorneo());
                partido = partidoDao.save(partido);
                partidoList.add(partido);
            }
        }
    }
    public Partido asignarFecha(Partido partido) {
        partido.setCancha(canchaDao.findById(partido.getCancha().getId()).get());

        List<Partido> listaPartido = partidoDao.findByCanchaAndFechaPartidoBetween(partido.getCancha(), partido.getFechaPartido().minusHours(1).minusMinutes(59), partido.getFechaPartido().plusHours(1).minusMinutes(59));
        if(listaPartido.isEmpty()){
            partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
            return partidoDao.save(partido);
        }
        throw new IllegalArgumentException("Ya hay partido programado en " + partido.getCancha().getNombre() + " en esa fecha y hora");

    }
    public Partido getById(long idParido) {
        Partido partido = partidoDao.findById(idParido).get();
        return partido;
    }
    public List<DtoResulatoLlave> getEliminatoriasTorneo(long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        List<Partido> partidoList = partidoDao.findByTorneoAndFaseEncuentro(torneo, torneo.getFaseTorneo());
        List<DtoResulatoLlave> resulatoLlaveList = new ArrayList<>();
        for (int i = 0; i < partidoList.size() ; i++) {
            DtoResulatoLlave resulatoLlave = new DtoResulatoLlave();
            if (torneo.getModalidadEliminatorias().equals(ModalidadFase.IDA_VUELTA)) {
                for (int j = i; j < partidoList.size(); j++) {
                    if (partidoList.get(i).getEquipoLocal().equals(partidoList.get(j).getEquipoVisitante()) &&
                            partidoList.get(i).getEquipoVisitante().equals(partidoList.get(j).getEquipoLocal())) {
                        if ((partidoList.get(i).getAnotacionesEquipoLocal() + partidoList.get(j).getAnotacionesEquipoVisitante()) >
                                (partidoList.get(j).getAnotacionesEquipoLocal() + partidoList.get(i).getAnotacionesEquipoVisitante()) ||
                                (partidoList.get(i).getPenaltisEquipoLocal() > partidoList.get(i).getPenaltisEquipoVisitante() ||
                                partidoList.get(j).getPenaltisEquipoVisitante() > partidoList.get(j).getPenaltisEquipoLocal())) {
                            resulatoLlave.setIdGanador(partidoList.get(i).getEquipoLocal().getId());
                            resulatoLlave.setNombreGanador(partidoList.get(i).getEquipoLocal().getNombre());
                            resulatoLlave.setResultadoGanador(partidoList.get(i).getAnotacionesEquipoLocal() + partidoList.get(j).getAnotacionesEquipoVisitante() +
                                    ((partidoList.get(i).getPenaltisEquipoLocal() == 1 || partidoList.get(j).getPenaltisEquipoLocal() == 1)? "P": ""));
                            resulatoLlave.setIdPerdedor(partidoList.get(j).getEquipoLocal().getId());
                            resulatoLlave.setNombrePerdedor(partidoList.get(j).getEquipoLocal().getNombre());
                            resulatoLlave.setResultadoPerdedor(partidoList.get(j).getAnotacionesEquipoLocal() + partidoList.get(i).getAnotacionesEquipoVisitante() + "");

                        } else if ((partidoList.get(j).getAnotacionesEquipoLocal() + partidoList.get(i).getAnotacionesEquipoVisitante()) >
                                (partidoList.get(i).getAnotacionesEquipoLocal() + partidoList.get(j).getAnotacionesEquipoVisitante()) ||
                                partidoList.get(j).getPenaltisEquipoLocal() > partidoList.get(j).getPenaltisEquipoVisitante() ||
                                partidoList.get(i).getPenaltisEquipoVisitante() > partidoList.get(i).getPenaltisEquipoLocal()) {
                            resulatoLlave.setIdGanador(partidoList.get(j).getEquipoLocal().getId());
                            resulatoLlave.setNombreGanador(partidoList.get(j).getEquipoLocal().getNombre());
                            resulatoLlave.setResultadoGanador(partidoList.get(j).getAnotacionesEquipoLocal() + partidoList.get(i).getAnotacionesEquipoVisitante() +
                                    ((partidoList.get(i).getPenaltisEquipoLocal() == 1 || partidoList.get(j).getPenaltisEquipoLocal() == 1)? "P": ""));
                            resulatoLlave.setIdPerdedor(partidoList.get(i).getEquipoLocal().getId());
                            resulatoLlave.setNombrePerdedor(partidoList.get(i).getEquipoLocal().getNombre());
                            resulatoLlave.setResultadoPerdedor(partidoList.get(i).getAnotacionesEquipoLocal() + partidoList.get(j).getAnotacionesEquipoVisitante() + "");

                        }
                        resulatoLlaveList.add(resulatoLlave);
                    }
                }
            } else {
                if ((partidoList.get(i).getAnotacionesEquipoLocal() > partidoList.get(i).getAnotacionesEquipoVisitante()) ||
                        partidoList.get(i).getPenaltisEquipoLocal() > partidoList.get(i).getPenaltisEquipoVisitante()) {
                    resulatoLlave.setIdGanador(partidoList.get(i).getEquipoLocal().getId());
                    resulatoLlave.setNombreGanador(partidoList.get(i).getEquipoLocal().getNombre());
                    resulatoLlave.setResultadoGanador(partidoList.get(i).getAnotacionesEquipoLocal() + ((partidoList.get(i).getPenaltisEquipoLocal() == 1)? "P": ""));
                    resulatoLlave.setIdPerdedor(partidoList.get(i).getEquipoVisitante().getId());
                    resulatoLlave.setNombrePerdedor(partidoList.get(i).getEquipoVisitante().getNombre());
                    resulatoLlave.setResultadoPerdedor(partidoList.get(i).getAnotacionesEquipoVisitante() + "");
                } else if (partidoList.get(i).getAnotacionesEquipoVisitante() >  partidoList.get(i).getAnotacionesEquipoLocal() ||
                        partidoList.get(i).getPenaltisEquipoVisitante() > partidoList.get(i).getPenaltisEquipoLocal()) {
                    resulatoLlave.setIdGanador(partidoList.get(i).getEquipoVisitante().getId());
                    resulatoLlave.setNombreGanador(partidoList.get(i).getEquipoVisitante().getNombre());
                    resulatoLlave.setResultadoGanador(partidoList.get(i).getAnotacionesEquipoVisitante() + ((partidoList.get(i).getPenaltisEquipoVisitante() == 1)? "P": ""));
                    resulatoLlave.setIdPerdedor(partidoList.get(i).getEquipoLocal().getId());
                    resulatoLlave.setNombrePerdedor(partidoList.get(i).getEquipoLocal().getNombre());
                    resulatoLlave.setResultadoPerdedor(partidoList.get(i).getAnotacionesEquipoLocal() + "");

                }
                resulatoLlaveList.add(resulatoLlave);
            }

        }
        return resulatoLlaveList;
    }
    public Partido update(Partido partido) {
        return partidoDao.save(partido);
    }
    public Partido sumarGol(Partido partido) {
        if (partido.getEstadoPartido().equals(EstadoPartido.EN_PROCESO)) {
            return partidoDao.save(partido);
        }
        throw new IllegalArgumentException("El partido no esta en juego");
    }
    public Partido iniciarPartido(long id) {
        Partido partido = partidoDao.findById(id).get();
        partido.setEstadoPartido(EstadoPartido.EN_PROCESO);
        return partidoDao.save(partido);
    }
    public Partido terminarPartido(long id) {
        Partido partido = partidoDao.findById(id).get();
        partido.setEstadoPartido(EstadoPartido.TERMINADO);
        if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS)) {
            partido.getEquipoLocal().setAnotacionesAFavor(partido.getEquipoLocal().getAnotacionesAFavor() + partido.getAnotacionesEquipoLocal());
            partido.getEquipoLocal().setAnotacionesEnContra(partido.getEquipoLocal().getAnotacionesEnContra() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesAFavor(partido.getEquipoVisitante().getAnotacionesAFavor() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesEnContra(partido.getEquipoVisitante().getAnotacionesEnContra() + partido.getAnotacionesEquipoLocal());
            partido.getEquipoLocal().setPartidosJugados(partido.getEquipoLocal().getPartidosJugados() + 1);
            partido.getEquipoVisitante().setPartidosJugados(partido.getEquipoVisitante().getPartidosJugados() + 1);
        } else if (partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
            partido.getEquipoLocal().setAnotacionesAFavorEliminatoria(partido.getEquipoLocal().getAnotacionesAFavorEliminatoria() + partido.getAnotacionesEquipoLocal());
            partido.getEquipoLocal().setAnotacionesEnContraEliminatoria(partido.getEquipoLocal().getAnotacionesEnContraEliminatoria() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesAFavorEliminatoria(partido.getEquipoVisitante().getAnotacionesAFavorEliminatoria() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesEnContraEliminatoria(partido.getEquipoVisitante().getAnotacionesEnContraEliminatoria() + partido.getAnotacionesEquipoLocal());
            partido.getEquipoLocal().setPartidosJugadosEliminatoria(partido.getEquipoLocal().getPartidosJugadosEliminatoria() + 1);
            partido.getEquipoVisitante().setPartidosJugadosEliminatoria(partido.getEquipoVisitante().getPartidosJugadosEliminatoria() + 1);
        }
        if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS) || partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
            establecerGanador(partido, 1, partido);
        }
        return partidoDao.save(partido);
    }
    public Partido terminarPartidoPenaltis(long id, String gandor) {
        Partido partido = partidoDao.findById(id).get();
        partido.setEstadoPartido(EstadoPartido.TERMINADO);
        if (gandor.equals("L") ) {
            partido.setPenaltisEquipoLocal(1);
            partido.setPenaltisEquipoVisitante(0);
        } else if (gandor.equals("V")) {
            partido.setPenaltisEquipoLocal(0);
            partido.setPenaltisEquipoVisitante(1);
        }
        return partidoDao.save(partido);
    }
    public Partido cancelarPartido(long id) {
        Partido partido = partidoDao.findById(id).get();
        partido.setFechaPartido(null);
        partido.setCancha(null);
        partido.setEstadoPartido(EstadoPartido.PENDIENTE);
        return partidoDao.save(partido);
    }
    public Partido cambiarMarcador(Partido partido) {
        Partido partidoAntiguo = partidoDao.findById(partido.getId()).get();
        Equipo equipoLocal = partido.getEquipoLocal();
        Equipo equipoVisitante = partido.getEquipoVisitante();
        if (partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
            equipoLocal.setAnotacionesAFavorEliminatoria(equipoLocal.getAnotacionesAFavorEliminatoria() - partidoAntiguo.getAnotacionesEquipoLocal() + partido.getAnotacionesEquipoLocal());
            equipoLocal.setAnotacionesEnContraEliminatoria(equipoLocal.getAnotacionesEnContraEliminatoria() - partidoAntiguo.getAnotacionesEquipoVisitante() + partido.getAnotacionesEquipoVisitante());
            equipoVisitante.setAnotacionesAFavorEliminatoria(equipoVisitante.getAnotacionesAFavorEliminatoria() - partidoAntiguo.getAnotacionesEquipoVisitante() + partido.getAnotacionesEquipoVisitante());
            equipoVisitante.setAnotacionesEnContraEliminatoria(equipoVisitante.getAnotacionesEnContraEliminatoria() - partidoAntiguo.getAnotacionesEquipoLocal() + partido.getAnotacionesEquipoLocal());
        } else if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS)) {
            partido.getEquipoLocal().setAnotacionesAFavor(equipoLocal.getAnotacionesAFavor() - partidoAntiguo.getAnotacionesEquipoLocal() + partido.getAnotacionesEquipoLocal());
            partido.getEquipoLocal().setAnotacionesEnContra(equipoLocal.getAnotacionesEnContra() - partidoAntiguo.getAnotacionesEquipoVisitante() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesAFavor(equipoVisitante.getAnotacionesAFavor() - partidoAntiguo.getAnotacionesEquipoVisitante() + partido.getAnotacionesEquipoVisitante());
            partido.getEquipoVisitante().setAnotacionesEnContra(equipoVisitante.getAnotacionesEnContra() - partidoAntiguo.getAnotacionesEquipoLocal() + partido.getAnotacionesEquipoLocal());
        }
        if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS) || partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
            establecerGanador(partido, -1, partidoAntiguo);
            establecerGanador(partido, 1, partido);
        }
        equipoDao.save(equipoLocal);
        equipoDao.save(equipoVisitante);
        return partidoDao.save(partido);
    }
    private void establecerGanador(Partido partido, int multiplicador, Partido partidoComparador) {
        if (partidoComparador.getAnotacionesEquipoLocal() > partidoComparador.getAnotacionesEquipoVisitante()) {
            if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS)) {
                partido.getEquipoLocal().setPartidosGanados(partido.getEquipoLocal().getPartidosGanados() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosPerdidos(partido.getEquipoVisitante().getPartidosPerdidos() + 1 * multiplicador);
                partido.getEquipoLocal().setPuntos(partido.getEquipoLocal().getPuntos() + 3 * multiplicador);
            } else if (partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                partido.getEquipoLocal().setPartidosGanadosEliminatoria(partido.getEquipoLocal().getPartidosGanadosEliminatoria() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosPerdidosEliminatoria(partido.getEquipoVisitante().getPartidosPerdidosEliminatoria() + 1 * multiplicador);
                partido.getEquipoLocal().setPuntosEliminatoria(partido.getEquipoLocal().getPuntosEliminatoria() + 3 * multiplicador);
            }
        } else if (partidoComparador.getAnotacionesEquipoLocal() < partidoComparador.getAnotacionesEquipoVisitante()) {
            if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS)) {
                partido.getEquipoLocal().setPartidosPerdidos(partido.getEquipoLocal().getPartidosPerdidos() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosGanados(partido.getEquipoVisitante().getPartidosGanados() + 1 * multiplicador);
                partido.getEquipoVisitante().setPuntos(partido.getEquipoVisitante().getPuntos() + 3 * multiplicador);
            } else if (partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                partido.getEquipoLocal().setPartidosPerdidosEliminatoria(partido.getEquipoLocal().getPartidosPerdidosEliminatoria() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosGanadosEliminatoria(partido.getEquipoVisitante().getPartidosGanadosEliminatoria() + 1 * multiplicador);
                partido.getEquipoVisitante().setPuntosEliminatoria(partido.getEquipoVisitante().getPuntosEliminatoria() + 3 * multiplicador);
            }
        } else if (partidoComparador.getAnotacionesEquipoLocal() == partidoComparador.getAnotacionesEquipoVisitante()) {
            if (partido.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS)) {
                partido.getEquipoLocal().setPartidosEmpatados(partido.getEquipoLocal().getPartidosEmpatados() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosEmpatados(partido.getEquipoVisitante().getPartidosEmpatados() + 1 * multiplicador);
                partido.getEquipoLocal().setPuntos(partido.getEquipoLocal().getPuntos() + 1 * multiplicador);
                partido.getEquipoVisitante().setPuntos(partido.getEquipoVisitante().getPuntos() + 1 * multiplicador);
            } else if (partido.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                partido.getEquipoLocal().setPartidosEmpatadosEliminatoria(partido.getEquipoLocal().getPartidosEmpatadosEliminatoria() + 1 * multiplicador);
                partido.getEquipoVisitante().setPartidosEmpatadosEliminatoria(partido.getEquipoVisitante().getPartidosEmpatadosEliminatoria() + 1 * multiplicador);
                partido.getEquipoLocal().setPuntosEliminatoria(partido.getEquipoLocal().getPuntosEliminatoria() + 1 * multiplicador);
                partido.getEquipoVisitante().setPuntosEliminatoria(partido.getEquipoVisitante().getPuntosEliminatoria() + 1 * multiplicador);
            }
        }
    }
    public List<Partido> getByTorneoModalidad(Long id, ModalidadTorneo modalidadTorneo) {
        Torneo torneo = torneoDao.findById(id).get();
        List<Partido> listaPartidos = new ArrayList<>();
        if (modalidadTorneo.equals(ModalidadTorneo.ELIMINATORIAS)){
            List<FaseActual> listFA = new ArrayList<>();
            listFA.add(FaseActual.TREINTAIDOSAVOS);
            listFA.add(FaseActual.DIECISEISAVOS);
            listFA.add(FaseActual.OCTAVOS);
            listFA.add(FaseActual.CUARTOS);
            listFA.add(FaseActual.SEMIFINAL);
            listFA.add(FaseActual.FINAL);
            listaPartidos = partidoDao.findByTorneoAndFaseEncuentroIn(torneo, listFA);
        }
        if (listaPartidos.size() == 0) {
            throw  new IllegalArgumentException("No hay equipos en esta fase");
        }
        return listaPartidos;
    }
}