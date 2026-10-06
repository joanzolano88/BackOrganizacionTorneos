package com.example.torneos.services;

import com.example.torneos.DTO.DtoResulatoLlave;
import com.example.torneos.DTO.DtoPartidoResumen;
import com.example.torneos.DTO.DtoDelegadoPartido;
import com.example.torneos.DTO.DtoEquipoPartido;
import com.example.torneos.DTO.DtoCanchaPartido;
import com.example.torneos.dao.CanchaDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadFase;
import com.example.torneos.enums.ModalidadTorneo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.dao.ParticipacionEquipoTorneoDao;
import com.example.torneos.entities.ParticipacionEquipoTorneo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PartidoService {
    @Autowired
    private PartidoDao partidoDao;
    @Autowired
    private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired
    private ParticipacionEquipoTorneoDao participacionEquipoDao;
    @Autowired
    private TorneoDao torneoDao;
    @Autowired
    private CanchaDao canchaDao;
    @Autowired
    private SancionJugadorTorneoService sancionService;
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private NotificacionUsuarioDao notificacionDao;
    @Autowired
    private ConvocatoriaPartidoDao convocatoriaDao;
    @Autowired
    private AyudanteTorneoDao ayudanteTorneoDao;
    @Autowired
    private JugadorDao jugadorDao;

    public Partido save(Partido partido, long usuarioId) {
        validarPropietarioTorneo(partido.getTorneo(), usuarioId);
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
                partidosGrupos = partidoDao.findByTorneoAndFaseEncuentro(torneo, faseActual);
                asignarGruposFaltantes(partidosGrupos, torneo, faseActual);
                for (int i = 1; i <= torneo.getCantidadGrupos(); i++) {
                    int grupoActual = i;
                    List<Partido> partidosGrupo = partidosGrupos.stream()
                            .filter(partido -> partido.getGrupo() == grupoActual)
                            .sorted(Comparator.comparing(Partido::getEstadoPartido))
                            .toList();
                    partidosTorneo.add(partidosGrupo);
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

    @Transactional(readOnly = true)
    public List<List<DtoPartidoResumen>> getTorneoFaseFiltrada(long idTorneo, FaseActual faseActual, String estado, boolean sinProgramar, String equipo) {
        Torneo torneo = torneoDao.findById(idTorneo).orElseThrow();
        List<Partido> partidos;
        if (sinProgramar) {
            partidos = partidoDao.findByTorneoAndFaseEncuentroAndFechaPartidoIsNull(torneo, faseActual);
        } else if (estado != null && !estado.isBlank() && !estado.equals("TODOS")) {
            partidos = partidoDao.findByTorneoAndFaseEncuentroAndEstadoPartido(torneo, faseActual, EstadoPartido.valueOf(estado));
        } else {
            partidos = partidoDao.findByTorneoAndFaseEncuentro(torneo, faseActual);
        }
        String texto = equipo == null ? "" : equipo.trim().toLowerCase();
        if (!texto.isEmpty()) {
            partidos = partidos.stream().filter(partido ->
                    (partido.getEquipoLocal() != null && partido.getEquipoLocal().getNombre().toLowerCase().contains(texto)) ||
                    (partido.getEquipoVisitante() != null && partido.getEquipoVisitante().getNombre().toLowerCase().contains(texto))).toList();
        }
        partidos.sort(Comparator.comparing(Partido::getGrupo).thenComparing(Partido::getEstadoPartido));
        List<List<DtoPartidoResumen>> resultado = new ArrayList<>();
        if (faseActual == FaseActual.FASE_GRUPOS || faseActual == FaseActual.ELIMINATORIAS_GRUPOS) {
            for (int grupo = 1; grupo <= torneo.getCantidadGrupos(); grupo++) {
                int grupoActual = grupo;
                resultado.add(partidos.stream()
                        .filter(partido -> partido.getGrupo() == grupoActual)
                        .map(this::resumenPartido)
                        .toList());
            }
        } else if (!partidos.isEmpty()) {
            resultado.add(partidos.stream().map(this::resumenPartido).toList());
        }
        return resultado;
    }

    private void asignarGruposFaltantes(List<Partido> partidos, Torneo torneo, FaseActual fase) {
        List<ParticipacionEquipoTorneo> participaciones = participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
            .filter(participacion -> participacion.getFaseActual() == fase)
            .toList();
        if (participaciones.isEmpty()) {
            return;
        }
        Map<Long, Integer> grupoPorEquipo = participaciones.stream()
            .collect(Collectors.toMap(participacion -> participacion.getEquipo().getId(), ParticipacionEquipoTorneo::getGrupo));
        boolean cambio = false;
        for (Partido partido : partidos) {
            if (partido.getGrupo() <= 0) {
                Integer grupo = grupoPorEquipo.get(partido.getEquipoLocal().getId());
                if (grupo == null) {
                    grupo = grupoPorEquipo.get(partido.getEquipoVisitante().getId());
                }
                if (grupo != null) {
                    partido.setGrupo(grupo);
                    cambio = true;
                }
            }
        }
        if (cambio) {
            partidoDao.saveAll(partidos);
        }
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
    @Transactional(readOnly = true)
    public List<DtoPartidoResumen> getPartidosFechaTorneo(long fecha, long idTorneo) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fecha), ZoneId.systemDefault());
        List<EstadoPartido> listEP = new ArrayList<>();
        listEP.add(EstadoPartido.PROGRAMADO);
        listEP.add(EstadoPartido.EN_PROCESO);
        listEP.add(EstadoPartido.TERMINADO);
        List<Partido> partidos = partidoDao.findByEstadoPartidoInAndTorneoAndFechaPartidoBetween(listEP, torneo,
                localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0),
                localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(0));
        return partidos.stream().map(this::resumenPartido).toList();
    }

    private DtoPartidoResumen resumenPartido(Partido partido) {
        Equipo local = partido.getEquipoLocal();
        Equipo visitante = partido.getEquipoVisitante();
        DtoEquipoPartido dtoLocal = local == null ? null : new DtoEquipoPartido(local.getId(), local.getNombre(), local.getEscudo(),
            local.getDelegado() == null ? null : new DtoDelegadoPartido(local.getDelegado().getNumeroCelular()));
        DtoEquipoPartido dtoVisitante = visitante == null ? null : new DtoEquipoPartido(visitante.getId(), visitante.getNombre(), visitante.getEscudo(),
            visitante.getDelegado() == null ? null : new DtoDelegadoPartido(visitante.getDelegado().getNumeroCelular()));
        com.example.torneos.entities.Cancha cancha = partido.getCancha();
        DtoCanchaPartido dtoCancha = cancha == null ? null : new DtoCanchaPartido(cancha.getId(), cancha.getNombre(), cancha.getDireccion());
        return new DtoPartidoResumen(partido.getId(), partido.getGrupo(), partido.getFaseEncuentro(), partido.getFechaPartido(),
                partido.getEstadoPartido(), partido.getAnotacionesEquipoLocal(), partido.getAnotacionesEquipoVisitante(),
                partido.getPenaltisEquipoLocal(), partido.getPenaltisEquipoVisitante(), dtoLocal, dtoVisitante, dtoCancha);
    }
    public List<Partido> generarPartidos(long idTorneo, long usuarioId) {
        Torneo torneo = torneoDao.findById(idTorneo).get();
        validarPropietarioTorneo(torneo, usuarioId);
        List<Equipo> equipoList = participacionEquipoDao.findByTorneoAndEstadoAndFaseActual(torneo, "ACEPTADO", torneo.getFaseTorneo()).stream()
            .map(ParticipacionEquipoTorneo::getEquipo)
            .toList();
        ModalidadFase modalidadFase = modalidadDeFase(torneo);
        List<Partido> partidoList = new ArrayList<>();
        for (int i = 0; i < (equipoList.size() - 1); i++) {
            for (int j = (i + 1); j < equipoList.size(); j++) {
                if ((torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && (torneo.getModalidadTorneo().equals(ModalidadTorneo.GRUPOS) || torneo.getModalidadTorneo().equals(ModalidadTorneo.ELIMINATORIAS_GRUPOS)))
                        || torneo.getFaseTorneo().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                    if (grupoEnTorneo(torneo, equipoList.get(i)) == grupoEnTorneo(torneo, equipoList.get(j))) {
                        establecerPartido(torneo, equipoList, partidoList, modalidadFase, i, j);
                    }
                } else if (torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && torneo.getModalidadTorneo().equals(ModalidadTorneo.LIGA)) {
                    establecerPartido(torneo, equipoList, partidoList, modalidadFase, i, j);
                } else if (!torneo.getFaseTorneo().equals(FaseActual.FASE_GRUPOS) && !torneo.getFaseTorneo().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
                    establecerPartido(torneo, equipoList, partidoList, modalidadFase, i, j);
                }
            }
        }
        return partidoList;
    }

    private ModalidadFase modalidadDeFase(Torneo torneo) {
        FaseActual fase = torneo.getFaseTorneo();
        ModalidadFase modalidad = fase == FaseActual.FASE_GRUPOS || fase == FaseActual.ELIMINATORIAS_GRUPOS
                ? torneo.getModalidadGrupos()
                : torneo.getModalidadEliminatorias();
        if (modalidad == null) {
            throw new IllegalArgumentException("La modalidad de la fase actual no está configurada");
        }
        return modalidad;
    }
    private int grupoEnTorneo(Torneo torneo, Equipo equipo) {
        return participacionEquipoDao.findByEquipoAndTorneo(equipo, torneo)
                .map(ParticipacionEquipoTorneo::getGrupo)
                .orElse(0);
    }
    private void establecerPartido(Torneo torneo, List<Equipo> equipoList, List<Partido> partidoList,
                                   ModalidadFase modalidad, int equipo1, int equipo2) {
        Partido partidoExiste = partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo,equipoList.get(equipo1), equipoList.get(equipo2), torneo.getFaseTorneo());
        Partido partidoExiste2 = partidoDao.findByTorneoAndEquipoLocalAndEquipoVisitanteAndFaseEncuentro(torneo,equipoList.get(equipo2), equipoList.get(equipo1), torneo.getFaseTorneo());
        Partido partido = new Partido();
        if (modalidad == ModalidadFase.PARTIDO_UNICO) {
            if (partidoExiste == null && partidoExiste2 == null) {
                partido.setEquipoLocal(equipoList.get(equipo1));
                partido.setEquipoVisitante(equipoList.get(equipo2));
                partido.setTorneo(torneo);
                partido.setGrupo(grupoEnTorneo(torneo, partido.getEquipoLocal()));
                partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                partido.setFaseEncuentro(torneo.getFaseTorneo());
                partido = partidoDao.save(partido);
                partidoList.add(partido);
            }
        } else if (modalidad == ModalidadFase.IDA_VUELTA) {
            if (partidoExiste == null) {
                partido.setEquipoLocal(equipoList.get(equipo1));
                partido.setEquipoVisitante(equipoList.get(equipo2));
                partido.setTorneo(torneo);
                partido.setGrupo(grupoEnTorneo(torneo, partido.getEquipoLocal()));
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
                partido.setGrupo(grupoEnTorneo(torneo, partido.getEquipoLocal()));
                partido.setEstadoPartido(EstadoPartido.PENDIENTE);
                partido.setFaseEncuentro(torneo.getFaseTorneo());
                partido = partidoDao.save(partido);
                partidoList.add(partido);
            }
        }
    }
    public Partido asignarFecha(Partido partido, long usuarioId) {
        if (partido == null || partido.getId() == 0 || partido.getFechaPartido() == null || partido.getCancha() == null) {
            throw new IllegalArgumentException("El partido, la fecha y la cancha son obligatorios");
        }
        Partido partidoProgramado = partidoDao.findById(partido.getId()).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(partidoProgramado, usuarioId);
        LocalDateTime fechaAnterior = partidoProgramado.getFechaPartido();
        Long canchaAnterior = partidoProgramado.getCancha() == null ? null : partidoProgramado.getCancha().getId();
        partidoProgramado.setFechaPartido(partido.getFechaPartido());
        partidoProgramado.setCancha(canchaDao.findById(partido.getCancha().getId()).orElseThrow(() -> new IllegalArgumentException("La cancha no existe")));
        LocalDateTime inicio = partidoProgramado.getFechaPartido();
        int duracionPartido = duracionTorneo(partidoProgramado.getTorneo());
        LocalDateTime fin = inicio.plusMinutes(duracionPartido);
        List<Partido> partidosCancha = partidoDao.findAll().stream()
                .filter(otro -> otro.getId() != partidoProgramado.getId())
                .filter(otro -> otro.getCancha() != null && otro.getCancha().getId() == partidoProgramado.getCancha().getId())
                .filter(otro -> otro.getFechaPartido() != null)
                .filter(otro -> otro.getEstadoPartido() == EstadoPartido.PROGRAMADO ||
                        otro.getEstadoPartido() == EstadoPartido.EN_PROCESO)
                .toList();
        for (Partido otro : partidosCancha) {
            LocalDateTime inicioOtro = otro.getFechaPartido();
            LocalDateTime finOtro = inicioOtro.plusMinutes(duracionTorneo(otro.getTorneo()));
            if (inicio.isBefore(finOtro) && inicioOtro.isBefore(fin)) {
                throw new IllegalArgumentException("Ya hay un partido en " + partidoProgramado.getCancha().getNombre() + " durante ese horario");
            }
        }

        List<Partido> partidosEquipo = partidoDao.findAll().stream()
                .filter(otro -> otro.getId() != partidoProgramado.getId())
                .filter(otro -> otro.getFechaPartido() != null)
                .filter(otro -> otro.getEstadoPartido() == EstadoPartido.PROGRAMADO ||
                        otro.getEstadoPartido() == EstadoPartido.EN_PROCESO)
                .filter(otro -> mismoEquipo(partidoProgramado, otro))
                .toList();
        for (Partido otro : partidosEquipo) {
            int duracionOtro = duracionTorneo(otro.getTorneo());
            LocalDateTime inicioOtro = otro.getFechaPartido();
            boolean torneosDiferentes = partidoProgramado.getTorneo() != null && otro.getTorneo() != null
                    && partidoProgramado.getTorneo().getId() != otro.getTorneo().getId();
            long descansoEntreTorneos = torneosDiferentes ? 60L : 0L;
            LocalDateTime finOtro = inicioOtro.plusMinutes(duracionOtro + descansoEntreTorneos);
            LocalDateTime finPartido = fin.plusMinutes(descansoEntreTorneos);
            boolean seCruza = inicio.isBefore(finOtro) && inicioOtro.isBefore(finPartido);
            if (seCruza) {
                String detalle = torneosDiferentes ? " entre torneos diferentes" : " dentro del mismo torneo";
                throw new IllegalArgumentException("El equipo " + nombreEquipoComun(partidoProgramado, otro) + " tiene otro partido durante ese horario" + detalle);
            }
        }
        partidoProgramado.setEstadoPartido(EstadoPartido.PROGRAMADO);
        Partido guardado = partidoDao.save(partidoProgramado);
        if (!inicio.equals(fechaAnterior) || !java.util.Objects.equals(Long.valueOf(partidoProgramado.getCancha().getId()), canchaAnterior)) {
            notificarDelegadosProgramacion(guardado);
        }
        return guardado;

    }

    private void notificarDelegadosProgramacion(Partido partido) {
        java.util.Set<String> celularesNotificados = new java.util.HashSet<>();
        for (Equipo equipo : List.of(partido.getEquipoLocal(), partido.getEquipoVisitante())) {
            if (equipo.getDelegado() == null || equipo.getDelegado().getNumeroCelular() == null ||
                    !celularesNotificados.add(equipo.getDelegado().getNumeroCelular())) continue;
            Usuario delegado = usuarioDao.findByNumeroCelular(equipo.getDelegado().getNumeroCelular());
            if (delegado == null) continue;
            NotificacionUsuario notificacion = new NotificacionUsuario();
            notificacion.setUsuario(delegado);
            notificacion.setTitulo("Partido programado");
            notificacion.setMensaje("" + partido.getEquipoLocal().getNombre() + " vs " + partido.getEquipoVisitante().getNombre() +
                    " quedó programado para " + partido.getFechaPartido() + " en " + partido.getCancha().getNombre() + ".");
                notificacion.setRuta("/auth/partidos/partido/" + partido.getId());
            notificacion.setCreadaEn(LocalDateTime.now());
            notificacion.setLeida(false);
            notificacionDao.save(notificacion);
        }
    }

    private int duracionTorneo(Torneo torneo) {
        return torneo != null && torneo.getDuracionMinutos() > 0 ? torneo.getDuracionMinutos() : 90;
    }

    private boolean mismoEquipo(Partido primero, Partido segundo) {
        long local = primero.getEquipoLocal() == null ? 0 : primero.getEquipoLocal().getId();
        long visitante = primero.getEquipoVisitante() == null ? 0 : primero.getEquipoVisitante().getId();
        long otroLocal = segundo.getEquipoLocal() == null ? 0 : segundo.getEquipoLocal().getId();
        long otroVisitante = segundo.getEquipoVisitante() == null ? 0 : segundo.getEquipoVisitante().getId();
        return local == otroLocal || local == otroVisitante || visitante == otroLocal || visitante == otroVisitante;
    }

    private String nombreEquipoComun(Partido primero, Partido segundo) {
        if (primero.getEquipoLocal() != null && (primero.getEquipoLocal().equals(segundo.getEquipoLocal()) || primero.getEquipoLocal().equals(segundo.getEquipoVisitante()))) {
            return primero.getEquipoLocal().getNombre();
        }
        return primero.getEquipoVisitante() == null ? "seleccionado" : primero.getEquipoVisitante().getNombre();
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
    public Partido update(Partido partido, long usuarioId) {
        Partido existente = partidoDao.findById(partido.getId()).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(existente, usuarioId);
        partido.setTorneo(existente.getTorneo());
        partido.setFaseEncuentro(existente.getFaseEncuentro());
        return partidoDao.save(partido);
    }
    public Partido sumarGol(Partido partido, long usuarioId) {
        Partido existente = partidoDao.findById(partido.getId()).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(existente, usuarioId);
        if (partido.getEstadoPartido().equals(EstadoPartido.EN_PROCESO)) {
            partido.setTorneo(existente.getTorneo());
            partido.setFaseEncuentro(existente.getFaseEncuentro());
            return partidoDao.save(partido);
        }
        throw new IllegalArgumentException("El partido no esta en juego");
    }
    public Partido iniciarPartido(long id, long usuarioId) {
        Partido partido = partidoDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(partido, usuarioId);
        if (partido.getEstadoPartido() != EstadoPartido.PROGRAMADO) {
            throw new IllegalArgumentException("Solo se puede iniciar un partido programado");
        }
        int minimoTitulares = partido.getTorneo().getDeporte() == null ? 7 : partido.getTorneo().getDeporte().getMinimoTitulares();
        for (Equipo equipo : List.of(partido.getEquipoLocal(), partido.getEquipoVisitante())) {
            if (convocatoriaDao.countByPartidoAndJugadorEquipoAndTitularTrue(partido, equipo) < minimoTitulares) {
                throw new IllegalArgumentException("El equipo " + equipo.getNombre() + " no cumple el mínimo de titulares");
            }
        }
        partido.setEstadoPartido(EstadoPartido.EN_PROCESO);
        return partidoDao.save(partido);
    }
    public Partido terminarPartido(long id, long usuarioId) {
        Partido partido = partidoDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(partido, usuarioId);
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Solo se puede terminar un partido en proceso");
        }
        partido.setEstadoPartido(EstadoPartido.TERMINADO);
        marcarParticipantes(partido);
        sancionService.cumplirSanciones(partido);
        Partido guardado = partidoDao.save(partido);
        if (guardado.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS) || guardado.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS)) {
            recalcularParticipacionesEquipo(guardado.getTorneo(), guardado.getFaseEncuentro());
        }
        return guardado;
    }
    public Partido terminarPartidoPenaltis(long id, String gandor, long usuarioId) {
        Partido partido = partidoDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(partido, usuarioId);
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Solo se pueden registrar penaltis en un partido en proceso");
        }
        if (!gandor.equals("L") && !gandor.equals("V")) {
            throw new IllegalArgumentException("El ganador de los penaltis no es válido");
        }
        if (partido.getAnotacionesEquipoLocal() != partido.getAnotacionesEquipoVisitante()) {
            throw new IllegalArgumentException("Solo se pueden jugar penaltis si el partido termina empatado");
        }
        partido.setEstadoPartido(EstadoPartido.TERMINADO);
        marcarParticipantes(partido);
        sancionService.cumplirSanciones(partido);
        if (gandor.equals("L") ) {
            partido.setPenaltisEquipoLocal(1);
            partido.setPenaltisEquipoVisitante(0);
        } else if (gandor.equals("V")) {
            partido.setPenaltisEquipoLocal(0);
            partido.setPenaltisEquipoVisitante(1);
        }
        return partidoDao.save(partido);
    }

    private void marcarParticipantes(Partido partido) {
        if (partido.getConvocatorias() == null) return;
        partido.getConvocatorias().forEach(convocatoria -> {
            ParticipacionJugadorTorneo participacion = participacionDao.findByTorneoAndJugador(partido.getTorneo(), convocatoria.getJugador()).orElse(null);
            if (participacion == null) {
                participacion = new ParticipacionJugadorTorneo();
                participacion.setTorneo(partido.getTorneo());
                participacion.setJugador(convocatoria.getJugador());
            }
            participacion.setEquipo(convocatoria.getJugador().getEquipo());
            participacion.setParticipando(true);
            participacion.setJugoPartido(true);
            participacionDao.save(participacion);
        });
    }
    public Partido cancelarPartido(long id, long usuarioId) {
        Partido partido = partidoDao.findById(id).get();
        validarGestorPartido(partido, usuarioId);
        partido.setFechaPartido(null);
        partido.setCancha(null);
        partido.setEstadoPartido(EstadoPartido.PENDIENTE);
        return partidoDao.save(partido);
    }
    public Partido cambiarMarcador(Partido partido, long usuarioId) {
        Partido partidoAntiguo = partidoDao.findById(partido.getId()).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        validarGestorPartido(partidoAntiguo, usuarioId);
        partido.setTorneo(partidoAntiguo.getTorneo());
        partido.setFaseEncuentro(partidoAntiguo.getFaseEncuentro());
        Partido guardado = partidoDao.save(partido);
        if (guardado.getEstadoPartido() == EstadoPartido.TERMINADO &&
                (guardado.getFaseEncuentro().equals(FaseActual.FASE_GRUPOS) || guardado.getFaseEncuentro().equals(FaseActual.ELIMINATORIAS_GRUPOS))) {
            recalcularParticipacionesEquipo(guardado.getTorneo(), guardado.getFaseEncuentro());
        }
        return guardado;
    }

    public boolean puedeGestionarPartido(long partidoId, long usuarioId) {
        Partido partido = partidoDao.findById(partidoId).orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        try {
            validarGestorPartido(partido, usuarioId);
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private void validarGestorPartido(Partido partido, long usuarioId) {
        Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        Torneo torneo = partido.getTorneo();
        boolean propietario = torneo.getEncargadoTorneo() != null && torneo.getEncargadoTorneo().getId() == usuarioId;
        boolean ayudante = ayudanteTorneoDao.findByTorneoAndUsuario(torneo, usuario).isPresent();
        if (!propietario && !ayudante) throw new IllegalArgumentException("Solo el organizador o un ayudante autorizado puede gestionar partidos");
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) return;
        jugadorDao.findByIdentificacion(usuario.getIdentificacion()).ifPresent(jugador -> {
            boolean porParticipacion = participacionDao.findByTorneoAndJugador(torneo, jugador)
                    .map(item -> partido.getEquipoLocal() != null && item.getEquipo().getId() == partido.getEquipoLocal().getId()
                            || partido.getEquipoVisitante() != null && item.getEquipo().getId() == partido.getEquipoVisitante().getId())
                    .orElse(false);
            boolean porRelacionEquipo = jugador.getEquipos() != null && jugador.getEquipos().stream()
                    .anyMatch(equipo -> partido.getEquipoLocal() != null && equipo.getId() == partido.getEquipoLocal().getId()
                            || partido.getEquipoVisitante() != null && equipo.getId() == partido.getEquipoVisitante().getId());
            boolean convocado = convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent();
            if (porParticipacion || porRelacionEquipo || convocado) {
                throw new IllegalArgumentException("Un jugador de este partido no puede gestionarlo; debe hacerlo otro ayudante");
            }
        });
    }

    private void validarPropietarioTorneo(Torneo torneo, long usuarioId) {
        Usuario usuario = usuarioDao.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        if (torneo == null || torneo.getEncargadoTorneo() == null || torneo.getEncargadoTorneo().getId() != usuario.getId()) {
            throw new IllegalArgumentException("Solo el organizador propietario puede generar partidos");
        }
    }

    private void recalcularParticipacionesEquipo(Torneo torneo, FaseActual fase) {
        List<ParticipacionEquipoTorneo> participaciones = participacionEquipoDao.findByTorneoAndEstado(torneo, "ACEPTADO").stream()
                .filter(participacion -> participacion.getFaseActual() == fase)
                .toList();
        if (participaciones.isEmpty()) return;
        Map<Long, ParticipacionEquipoTorneo> porEquipo = participaciones.stream()
                .collect(Collectors.toMap(participacion -> participacion.getEquipo().getId(), participacion -> participacion));
        for (ParticipacionEquipoTorneo participacion : participaciones) {
            participacion.setPuntos(0);
            participacion.setPartidosJugados(0);
            participacion.setPartidosGanados(0);
            participacion.setPartidosPerdidos(0);
            participacion.setPartidosEmpatados(0);
            participacion.setGolesFavor(0);
            participacion.setGolesContra(0);
        }
        List<Partido> partidos = partidoDao.findByTorneoAndFaseEncuentro(torneo, fase).stream()
                .filter(partido -> partido.getEstadoPartido() == EstadoPartido.TERMINADO)
                .toList();
        for (Partido partido : partidos) {
            ParticipacionEquipoTorneo local = porEquipo.get(partido.getEquipoLocal().getId());
            ParticipacionEquipoTorneo visitante = porEquipo.get(partido.getEquipoVisitante().getId());
            if (local == null || visitante == null) {
                continue;
            }
            int golesLocal = partido.getAnotacionesEquipoLocal();
            int golesVisitante = partido.getAnotacionesEquipoVisitante();
            local.setPartidosJugados(local.getPartidosJugados() + 1);
            visitante.setPartidosJugados(visitante.getPartidosJugados() + 1);
            local.setGolesFavor(local.getGolesFavor() + golesLocal);
            local.setGolesContra(local.getGolesContra() + golesVisitante);
            visitante.setGolesFavor(visitante.getGolesFavor() + golesVisitante);
            visitante.setGolesContra(visitante.getGolesContra() + golesLocal);
            if (golesLocal > golesVisitante) {
                local.setPartidosGanados(local.getPartidosGanados() + 1);
                local.setPuntos(local.getPuntos() + 3);
                visitante.setPartidosPerdidos(visitante.getPartidosPerdidos() + 1);
            } else if (golesLocal < golesVisitante) {
                visitante.setPartidosGanados(visitante.getPartidosGanados() + 1);
                visitante.setPuntos(visitante.getPuntos() + 3);
                local.setPartidosPerdidos(local.getPartidosPerdidos() + 1);
            } else {
                local.setPartidosEmpatados(local.getPartidosEmpatados() + 1);
                visitante.setPartidosEmpatados(visitante.getPartidosEmpatados() + 1);
                local.setPuntos(local.getPuntos() + 1);
                visitante.setPuntos(visitante.getPuntos() + 1);
            }
        }
        participacionEquipoDao.saveAll(participaciones);
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