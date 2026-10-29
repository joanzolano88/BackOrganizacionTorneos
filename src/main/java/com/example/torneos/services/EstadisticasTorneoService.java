package com.example.torneos.services;

import com.example.torneos.DTO.DtoEstadisticasTorneo;
import com.example.torneos.DTO.DtoGoleadorTorneo;
import com.example.torneos.DTO.DtoTablaEstadisticasTorneo;
import com.example.torneos.DTO.DtoVallaMenosVencida;
import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.enums.TipoEventoPartido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EstadisticasTorneoService {
    private final TorneoDao torneoDao;
    private final PartidoDao partidoDao;
    private final EventoPartidoDao eventoDao;
    private final ConvocatoriaPartidoDao convocatoriaDao;
    private final ParticipacionJugadorTorneoDao participacionDao;

    public EstadisticasTorneoService(
            TorneoDao torneoDao,
            PartidoDao partidoDao,
            EventoPartidoDao eventoDao,
            ConvocatoriaPartidoDao convocatoriaDao,
            ParticipacionJugadorTorneoDao participacionDao) {
        this.torneoDao = torneoDao;
        this.partidoDao = partidoDao;
        this.eventoDao = eventoDao;
        this.convocatoriaDao = convocatoriaDao;
        this.participacionDao = participacionDao;
    }

    @Transactional(readOnly = true)
    public DtoEstadisticasTorneo obtener(long torneoId) {
        Torneo torneo = torneoDao.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe"));
        List<Partido> partidos = partidoDao.findByTorneo(torneo).stream()
                .filter(partido -> partido.getEstadoPartido() == EstadoPartido.TERMINADO)
                .toList();
        Map<Long, Equipo> equipoPorJugador = new HashMap<>();
        participacionDao.findByTorneo(torneo).forEach(participacion ->
                equipoPorJugador.put(participacion.getJugador().getId(), participacion.getEquipo()));
        List<EventoPartido> eventos = partidos.isEmpty()
                ? List.of()
                : eventoDao.findByPartidoInOrderByPartidoIdAscMinutoAscIdAsc(partidos);
        Map<Long, List<EventoPartido>> eventosPorPartido = new HashMap<>();
        eventos.forEach(evento -> eventosPorPartido
                .computeIfAbsent(evento.getPartido().getId(), ignored -> new ArrayList<>())
                .add(evento));
        Map<Long, List<ConvocatoriaPartido>> convocatoriasPorPartido = new HashMap<>();
        if (!partidos.isEmpty()) {
            convocatoriaDao.findByPartidoIn(partidos).forEach(convocatoria -> convocatoriasPorPartido
                    .computeIfAbsent(convocatoria.getPartido().getId(), ignored -> new ArrayList<>())
                    .add(convocatoria));
        }

        List<Partido> partidosGrupos = partidos.stream().filter(this::esPartidoDeGrupos).toList();
        List<Partido> partidosEliminatorias = partidos.stream().filter(partido -> !esPartidoDeGrupos(partido)).toList();
        DtoTablaEstadisticasTorneo todos = calcular(partidos, eventosPorPartido, convocatoriasPorPartido, equipoPorJugador);
        boolean formatoConGrupos = torneo.getModalidadTorneo() == ModalidadTorneo.GRUPOS
                || torneo.getModalidadTorneo() == ModalidadTorneo.ELIMINATORIAS_GRUPOS;

        return new DtoEstadisticasTorneo(
                formatoConGrupos
                        ? calcular(partidosGrupos, eventosPorPartido, convocatoriasPorPartido, equipoPorJugador)
                        : null,
                formatoConGrupos
                        ? calcular(partidosEliminatorias, eventosPorPartido, convocatoriasPorPartido, equipoPorJugador)
                        : null,
                todos);
    }

    private DtoTablaEstadisticasTorneo calcular(
            List<Partido> partidos,
            Map<Long, List<EventoPartido>> eventosPorPartido,
            Map<Long, List<ConvocatoriaPartido>> convocatoriasPorPartido,
            Map<Long, Equipo> equipoPorJugador) {
        Map<Long, GoleadorAcumulado> goleadores = new HashMap<>();
        Map<Long, ArqueroAcumulado> arqueros = new HashMap<>();
        for (Partido partido : partidos) {
            List<EventoPartido> eventos = eventosPorPartido.getOrDefault(partido.getId(), List.of());
            for (EventoPartido evento : eventos) {
                if (evento.getTipo() != TipoEventoPartido.GOL || evento.getJugador() == null) continue;
                Jugador jugador = evento.getJugador();
                Equipo equipo = equipoPorJugador.getOrDefault(jugador.getId(), jugador.getEquipo());
                GoleadorAcumulado acumulado = goleadores.computeIfAbsent(jugador.getId(),
                        ignored -> new GoleadorAcumulado(jugador, equipo));
                acumulado.goles++;
            }
            acumularArqueros(partido, eventos, convocatoriasPorPartido.getOrDefault(partido.getId(), List.of()),
                    equipoPorJugador, arqueros);
        }

        List<DtoGoleadorTorneo> tablaGoleadores = goleadores.values().stream()
                .map(item -> new DtoGoleadorTorneo(
                        item.jugador.getId(), item.jugador.getNombre(),
                        item.equipo == null ? null : item.equipo.getId(),
                        item.equipo == null ? null : item.equipo.getNombre(),
                        item.goles))
                .sorted(Comparator.comparingLong(DtoGoleadorTorneo::goles).reversed()
                        .thenComparing(DtoGoleadorTorneo::jugador, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<DtoVallaMenosVencida> tablaArqueros = arqueros.values().stream()
                .map(item -> new DtoVallaMenosVencida(
                        item.jugador.getId(), item.jugador.getNombre(),
                        item.equipo == null ? null : item.equipo.getId(),
                        item.equipo == null ? null : item.equipo.getNombre(),
                        item.partidosJugados, item.golesRecibidos))
                .sorted(Comparator.comparingLong(DtoVallaMenosVencida::golesRecibidos)
                        .thenComparing(Comparator.comparingLong(DtoVallaMenosVencida::partidosJugados).reversed())
                        .thenComparing(DtoVallaMenosVencida::arquero, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return new DtoTablaEstadisticasTorneo(tablaGoleadores, tablaArqueros);
    }

    private void acumularArqueros(
            Partido partido,
            List<EventoPartido> eventos,
            List<ConvocatoriaPartido> convocatorias,
            Map<Long, Equipo> equipoPorJugador,
            Map<Long, ArqueroAcumulado> arqueros) {
        Map<Long, Long> equipoFinalPorEquipo = new LinkedHashMap<>();
        convocatorias.stream()
                .filter(ConvocatoriaPartido::isTitular)
                .filter(item -> Objects.equals(item.getPosicionAlineacion(), 0))
                .forEach(item -> {
                    Equipo equipo = equipoPorJugador.getOrDefault(item.getJugador().getId(), item.getJugador().getEquipo());
                    if (equipo != null) equipoFinalPorEquipo.put(equipo.getId(), item.getJugador().getId());
                });
        if (equipoFinalPorEquipo.isEmpty()) return;

        Map<Long, EventoPartido> entradaPorSalida = emparejarSustituciones(eventos, equipoPorJugador);
        Map<Long, Long> arqueroInicialPorEquipo = new HashMap<>(equipoFinalPorEquipo);
        List<EventoPartido> salidasReversas = eventos.stream()
                .filter(evento -> evento.getTipo() == TipoEventoPartido.SALE_TITULAR)
                .sorted(Comparator.comparing(EventoPartido::getMinuto).thenComparing(EventoPartido::getId).reversed())
                .toList();
        for (EventoPartido salida : salidasReversas) {
            EventoPartido entrada = entradaPorSalida.get(salida.getId());
            if (entrada == null || salida.getJugador() == null || entrada.getJugador() == null) continue;
            Equipo equipo = equipoJugador(salida.getJugador(), equipoPorJugador);
            if (equipo != null && Objects.equals(arqueroInicialPorEquipo.get(equipo.getId()), entrada.getJugador().getId())) {
                arqueroInicialPorEquipo.put(equipo.getId(), salida.getJugador().getId());
            }
        }

        Map<Long, Long> arqueroActivoPorEquipo = new HashMap<>(arqueroInicialPorEquipo);
        Set<Long> arquerosQueJugaron = new HashSet<>(arqueroInicialPorEquipo.values());
        for (EventoPartido evento : eventos) {
            if (evento.getTipo() == TipoEventoPartido.SALE_TITULAR && evento.getJugador() != null) {
                EventoPartido entrada = entradaPorSalida.get(evento.getId());
                Equipo equipo = equipoJugador(evento.getJugador(), equipoPorJugador);
                if (entrada != null && entrada.getJugador() != null && equipo != null
                        && Objects.equals(arqueroActivoPorEquipo.get(equipo.getId()), evento.getJugador().getId())) {
                    arqueroActivoPorEquipo.put(equipo.getId(), entrada.getJugador().getId());
                    arquerosQueJugaron.add(entrada.getJugador().getId());
                }
            }
            if (evento.getTipo() != TipoEventoPartido.GOL && evento.getTipo() != TipoEventoPartido.AUTOGOL) continue;
            Equipo anotador = equipoAnotador(evento, equipoPorJugador);
            if (anotador == null) continue;
            Long equipoLocalId = partido.getEquipoLocal() == null ? null : partido.getEquipoLocal().getId();
            Long equipoVisitanteId = partido.getEquipoVisitante() == null ? null : partido.getEquipoVisitante().getId();
            Long equipoQueRecibe = Objects.equals(anotador.getId(), equipoLocalId)
                    ? equipoVisitanteId
                    : Objects.equals(anotador.getId(), equipoVisitanteId) ? equipoLocalId : null;
            Long arqueroId = equipoQueRecibe == null ? null : arqueroActivoPorEquipo.get(equipoQueRecibe);
            if (arqueroId != null) {
                arqueros.computeIfAbsent(arqueroId, ignored ->
                        new ArqueroAcumulado(jugadorPorId(arqueroId, convocatorias), equipoDeArquero(arqueroId, convocatorias, equipoPorJugador)))
                        .golesRecibidos++;
            }
        }
        for (Long arqueroId : arquerosQueJugaron) {
            Jugador jugador = jugadorPorId(arqueroId, convocatorias);
            if (jugador == null) continue;
            Equipo equipo = equipoJugador(jugador, equipoPorJugador);
            arqueros.computeIfAbsent(arqueroId, ignored -> new ArqueroAcumulado(jugador, equipo)).partidosJugados++;
        }
    }

    private Map<Long, EventoPartido> emparejarSustituciones(
            List<EventoPartido> eventos, Map<Long, Equipo> equipoPorJugador) {
        Map<Long, EventoPartido> entradasPorSalida = new HashMap<>();
        Map<String, List<EventoPartido>> entradasPorEquipoMinuto = new HashMap<>();
        eventos.stream()
                .filter(evento -> evento.getTipo() == TipoEventoPartido.ENTRA_TITULAR && evento.getJugador() != null)
                .forEach(evento -> {
                    Equipo equipo = equipoJugador(evento.getJugador(), equipoPorJugador);
                    if (equipo != null) entradasPorEquipoMinuto
                            .computeIfAbsent(equipo.getId() + ":" + evento.getMinuto(), ignored -> new ArrayList<>())
                            .add(evento);
                });
        Map<String, Integer> entradaActual = new HashMap<>();
        eventos.stream()
                .filter(evento -> evento.getTipo() == TipoEventoPartido.SALE_TITULAR && evento.getJugador() != null)
                .forEach(salida -> {
                    Equipo equipo = equipoJugador(salida.getJugador(), equipoPorJugador);
                    if (equipo == null) return;
                    String clave = equipo.getId() + ":" + salida.getMinuto();
                    List<EventoPartido> entradas = entradasPorEquipoMinuto.getOrDefault(clave, List.of());
                    int indice = entradaActual.getOrDefault(clave, 0);
                    if (indice < entradas.size()) {
                        entradasPorSalida.put(salida.getId(), entradas.get(indice));
                        entradaActual.put(clave, indice + 1);
                    }
                });
        return entradasPorSalida;
    }

    private Equipo equipoAnotador(EventoPartido evento, Map<Long, Equipo> equipoPorJugador) {
        if (evento.getEquipoAnotador() != null) return evento.getEquipoAnotador();
        return evento.getJugador() == null ? null : equipoJugador(evento.getJugador(), equipoPorJugador);
    }

    private Equipo equipoJugador(Jugador jugador, Map<Long, Equipo> equipoPorJugador) {
        return equipoPorJugador.getOrDefault(jugador.getId(), jugador.getEquipo());
    }

    private Jugador jugadorPorId(Long jugadorId, List<ConvocatoriaPartido> convocatorias) {
        return convocatorias.stream().map(ConvocatoriaPartido::getJugador)
                .filter(Objects::nonNull).filter(jugador -> Objects.equals(jugador.getId(), jugadorId))
                .findFirst().orElse(null);
    }

    private Equipo equipoDeArquero(
            Long jugadorId, List<ConvocatoriaPartido> convocatorias, Map<Long, Equipo> equipoPorJugador) {
        Jugador jugador = jugadorPorId(jugadorId, convocatorias);
        return jugador == null ? null : equipoJugador(jugador, equipoPorJugador);
    }

    private boolean esPartidoDeGrupos(Partido partido) {
        return partido.getFaseEncuentro() == FaseActual.FASE_GRUPOS
                || partido.getFaseEncuentro() == FaseActual.ELIMINATORIAS_GRUPOS;
    }

    private static class GoleadorAcumulado {
        private final Jugador jugador;
        private final Equipo equipo;
        private long goles;

        private GoleadorAcumulado(Jugador jugador, Equipo equipo) {
            this.jugador = jugador;
            this.equipo = equipo;
        }
    }

    private static class ArqueroAcumulado {
        private final Jugador jugador;
        private final Equipo equipo;
        private long partidosJugados;
        private long golesRecibidos;

        private ArqueroAcumulado(Jugador jugador, Equipo equipo) {
            this.jugador = jugador;
            this.equipo = equipo;
        }
    }
}
