package com.example.torneos.services;

import com.example.torneos.DTO.DtoRegistrarGol;
import com.example.torneos.DTO.DtoConvocatoriaPartido;
import com.example.torneos.DTO.DtoFormacionPartido;
import com.example.torneos.DTO.DtoJugadorDisponiblePartido;
import com.example.torneos.DTO.DtoSancionJugadorPartido;
import com.example.torneos.DTO.DtoCrearSolicitudSustitucion;
import com.example.torneos.DTO.DtoSolicitudSustitucion;
import com.example.torneos.DTO.DtoEventoPartido;
import com.example.torneos.dao.AlineacionEquipoPartidoDao;
import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.InvitacionPartidoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.SolicitudSustitucionPartidoDao;
import com.example.torneos.dao.PagoInscripcionDao;
import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.AlineacionEquipoPartido;
import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.InvitacionPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.entities.Usuario;
import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.entities.SolicitudSustitucionPartido;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.EstadoSolicitudSustitucion;
import com.example.torneos.enums.EstadoJugador;
import com.example.torneos.enums.TipoUsuario;
import com.example.torneos.enums.TipoEventoPartido;
import com.example.torneos.enums.ModoCambioJugador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ConvocatoriaPartidoService {
    @Autowired private PartidoDao partidoDao;
    @Autowired private JugadorDao jugadorDao;
    @Autowired private ConvocatoriaPartidoDao convocatoriaDao;
    @Autowired private InvitacionPartidoDao invitacionDao;
    @Autowired private EquipoDao equipoDao;
    @Autowired private UsuarioDao usuarioDao;
    @Autowired private AyudanteTorneoDao ayudanteTorneoDao;
    @Autowired private EventoPartidoDao eventoDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired private SancionJugadorTorneoService sancionService;
    @Autowired private AlineacionEquipoPartidoDao alineacionDao;
    @Autowired private SolicitudSustitucionPartidoDao solicitudSustitucionDao;
    @Autowired private NotificacionUsuarioDao notificacionDao;
    @Autowired private PagoInscripcionDao pagoInscripcionDao;

    private static final Map<String, List<Integer>> FORMACIONES = Map.of(
            "4-4-2", List.of(1, 4, 4, 2),
            "4-3-3", List.of(1, 4, 3, 3),
            "3-5-2", List.of(1, 3, 5, 2),
            "4-2-3-1", List.of(1, 4, 2, 3, 1),
            "4-1-4-1", List.of(1, 4, 1, 4, 1),
            "4-5-1", List.of(1, 4, 5, 1));

    private static final String FORMACION_INICIAL = "4-4-2";

    @Transactional(readOnly = true)
    public List<DtoConvocatoriaPartido> listar(long partidoId) {
        Partido partido = partido(partidoId);
        Map<Long, Equipo> equipoPorJugador = new HashMap<>();
        participacionDao.findByTorneo(partido.getTorneo()).forEach(participacion ->
                equipoPorJugador.put(participacion.getJugador().getId(), participacion.getEquipo()));
        return convocatoriaDao.findByPartido(partido).stream()
                .map(convocatoria -> {
            Jugador jugador = convocatoria.getJugador();
            Equipo equipo = jugador == null
                    ? null
                    : equipoPorJugador.getOrDefault(jugador.getId(), jugador.getEquipo());
            DtoConvocatoriaPartido.DtoEquipoConvocatoria equipoDto = equipo == null
                    ? null
                    : new DtoConvocatoriaPartido.DtoEquipoConvocatoria(equipo.getId(), equipo.getNombre());
            DtoConvocatoriaPartido.DtoJugadorConvocatoria jugadorDto = jugador == null
                    ? null
                    : new DtoConvocatoriaPartido.DtoJugadorConvocatoria(
                            jugador.getId(),
                            jugador.getNombre(),
                            jugador.getIdentificacion(),
                            jugador.getNumeroCamiseta());
            return new DtoConvocatoriaPartido(
                    convocatoria.getId(),
                    jugadorDto,
                    equipoDto,
                    convocatoria.isTitular(),
                    convocatoria.getNumeroUniforme(),
                    convocatoria.getPosicionAlineacion());
        })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoFormacionPartido> listarFormaciones(long partidoId) {
        Partido partido = partido(partidoId);
        Map<Long, String> formacionPorEquipo = new HashMap<>();
        alineacionDao.findByPartido(partido).forEach(alineacion ->
                formacionPorEquipo.put(alineacion.getEquipo().getId(), alineacion.getFormacion()));
        return equiposDelPartido(partido).stream()
                .map(equipo -> new DtoFormacionPartido(
                        equipo.getId(),
                        equipo.getNombre(),
                        formacionPorEquipo.getOrDefault(equipo.getId(), FORMACION_INICIAL)))
                .toList();
    }

    @Transactional
    public DtoFormacionPartido cambiarFormacion(long partidoId, long equipoId, String formacion, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        Equipo equipo = equipoDelPartido(partido, equipoId);
        validarEquipoDelDelegado(partido, equipo, usuario);
        validarPermisoAlineacionDurantePartido(partido, usuario);
        List<Integer> lineas = FORMACIONES.get(formacion);
        if (lineas == null) {
            throw new IllegalArgumentException("La formación seleccionada no está disponible");
        }
        List<ConvocatoriaPartido> titulares = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(ConvocatoriaPartido::isTitular)
                .sorted(Comparator.comparing(ConvocatoriaPartido::getPosicionAlineacion,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ConvocatoriaPartido::getId))
                .toList();
        if (titulares.size() > lineas.stream().mapToInt(Integer::intValue).sum()) {
            throw new IllegalArgumentException("La formación no tiene suficientes posiciones para todos los titulares");
        }

        AlineacionEquipoPartido alineacion = alineacionDao.findByPartidoAndEquipo(partido, equipo)
                .orElseGet(() -> {
                    AlineacionEquipoPartido nueva = new AlineacionEquipoPartido();
                    nueva.setPartido(partido);
                    nueva.setEquipo(equipo);
                    return nueva;
                });
        alineacion.setFormacion(formacion);
        alineacionDao.save(alineacion);
        for (int index = 0; index < titulares.size(); index++) {
            titulares.get(index).setPosicionAlineacion(index);
            convocatoriaDao.save(titulares.get(index));
        }
        return new DtoFormacionPartido(equipo.getId(), equipo.getNombre(), formacion);
    }

    @Transactional
    public void intercambiarPosiciones(long partidoId, long convocatoriaId1, long convocatoriaId2, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        ConvocatoriaPartido primera = convocatoria(partidoId, convocatoriaId1);
        ConvocatoriaPartido segunda = convocatoria(partidoId, convocatoriaId2);
        if (!primera.isTitular() || !segunda.isTitular()) {
            throw new IllegalArgumentException("Solo puedes intercambiar la posición de dos titulares");
        }
        Equipo equipoPrimero = equipoDeJugadorEnPartido(partido, primera.getJugador());
        Equipo equipoSegundo = equipoDeJugadorEnPartido(partido, segunda.getJugador());
        if (equipoPrimero == null || equipoSegundo == null ||
                !Objects.equals(equipoPrimero.getId(), equipoSegundo.getId())) {
            throw new IllegalArgumentException("Los titulares deben pertenecer al mismo equipo");
        }
        validarEquipoDelDelegado(partido, equipoPrimero, usuario);
        validarPermisoAlineacionDurantePartido(partido, usuario);
        int totalPosiciones = alineacionDao.findByPartidoAndEquipo(partido, equipoPrimero)
                .map(AlineacionEquipoPartido::getFormacion)
                .map(FORMACIONES::get)
                .orElse(FORMACIONES.get(FORMACION_INICIAL)).stream()
                .mapToInt(Integer::intValue)
                .sum();
        Map<Long, Integer> posiciones = posicionesEfectivas(partido, equipoPrimero, totalPosiciones);
        Integer posicionPrimera = posiciones.get(primera.getId());
        Integer posicionSegunda = posiciones.get(segunda.getId());
        if (posicionPrimera == null || posicionSegunda == null) {
            throw new IllegalArgumentException("No se pudieron determinar las posiciones actuales de ambos titulares");
        }
        primera.setPosicionAlineacion(posicionSegunda);
        segunda.setPosicionAlineacion(posicionPrimera);
        convocatoriaDao.save(primera);
        convocatoriaDao.save(segunda);
    }

    @Transactional
    public void moverPosicionAlineacion(long partidoId, long convocatoriaId, int posicion, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        ConvocatoriaPartido convocatoria = convocatoria(partidoId, convocatoriaId);
        if (!convocatoria.isTitular()) {
            throw new IllegalArgumentException("Solo puedes mover a una posición vacía a un titular");
        }
        Equipo equipo = equipoDeJugadorEnPartido(partido, convocatoria.getJugador());
        validarEquipoDelDelegado(partido, equipo, usuario);
        validarPermisoAlineacionDurantePartido(partido, usuario);
        String formacion = alineacionDao.findByPartidoAndEquipo(partido, equipo)
                .map(AlineacionEquipoPartido::getFormacion)
                .orElse(FORMACION_INICIAL);
        List<Integer> lineas = FORMACIONES.get(formacion);
        int posicionesTotales = lineas.stream().mapToInt(Integer::intValue).sum();
        if (posicion < 0 || posicion >= posicionesTotales) {
            throw new IllegalArgumentException("La posición seleccionada no pertenece a la formación");
        }
        boolean ocupada = posicionesEfectivas(partido, equipo, posicionesTotales).entrySet().stream()
                .anyMatch(entry -> !Objects.equals(entry.getKey(), convocatoriaId) && entry.getValue() == posicion);
        if (ocupada) {
            throw new IllegalArgumentException("La posición seleccionada ya está ocupada");
        }
        convocatoria.setPosicionAlineacion(posicion);
        convocatoriaDao.save(convocatoria);
    }

    @Transactional
    public ConvocatoriaPartido agregarEnPosicionVacia(
            long partidoId, long jugadorId, int posicion, int numeroUniforme, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        if (partido.getEstadoPartido() != EstadoPartido.PROGRAMADO) {
            throw new IllegalArgumentException("Los titulares solo se pueden agregar antes de iniciar el partido");
        }
        if (numeroUniforme < 0 || numeroUniforme > 99) {
            throw new IllegalArgumentException("El número de uniforme debe estar entre 0 y 99");
        }

        Jugador jugador = jugadorDao.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        sancionService.validarElegibilidad(partido, jugador);
        Equipo equipo = equipoDeJugadorEnPartido(partido, jugador);
        validarEquipoDelDelegado(partido, equipo, usuario);
        String formacion = alineacionDao.findByPartidoAndEquipo(partido, equipo)
                .map(AlineacionEquipoPartido::getFormacion)
                .orElse(FORMACION_INICIAL);
        int posicionesTotales = FORMACIONES.get(formacion).stream().mapToInt(Integer::intValue).sum();
        if (posicion < 0 || posicion >= posicionesTotales) {
            throw new IllegalArgumentException("La posición seleccionada no pertenece a la formación actual");
        }
        boolean posicionOcupada = posicionesEfectivas(partido, equipo, posicionesTotales).values().stream()
                .anyMatch(posicionActual -> posicionActual == posicion);
        if (posicionOcupada) {
            throw new IllegalArgumentException("La posición ya está ocupada por otro jugador");
        }

        ConvocatoriaPartido convocatoria = convocatoriaDao.findByPartidoAndJugador(partido, jugador).orElse(null);
        if (convocatoria != null && convocatoria.isTitular()) {
            throw new IllegalArgumentException("El jugador ya está en la alineación");
        }
        if (convocatoria == null &&
                convocatoriaDao.countByPartidoAndJugadorEquipo(partido, equipo) >= maximoConvocados(partido)) {
            throw new IllegalArgumentException("Se alcanzó el máximo de jugadores convocados para " + equipo.getNombre());
        }
        if (convocatoria != null && convocatoria.isFueTitular() &&
                partido.getTorneo().getModoCambioJugador() != ModoCambioJugador.SALIR_ENTRAR) {
            throw new IllegalArgumentException("Este jugador ya salió del partido y no puede volver a entrar");
        }
        Long convocatoriaIdExenta = convocatoria == null ? null : convocatoria.getId();
        boolean numeroEnUso = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(item -> !Objects.equals(item.getId(), convocatoriaIdExenta))
                .anyMatch(item -> Objects.equals(
                        item.getNumeroUniforme() == null && item.getJugador() != null
                                ? item.getJugador().getNumeroCamiseta()
                                : item.getNumeroUniforme(),
                        numeroUniforme));
        if (numeroEnUso) {
            throw new IllegalArgumentException("El número de uniforme ya está asignado a otro jugador de este equipo en el partido");
        }
        validarMaxTitulares(partido, equipo);
        if (convocatoria == null) {
            convocatoria = new ConvocatoriaPartido();
            convocatoria.setPartido(partido);
            convocatoria.setJugador(jugador);
        }
        convocatoria.setTitular(true);
        convocatoria.setFueTitular(true);
        convocatoria.setPosicionAlineacion(posicion);
        convocatoria.setNumeroUniforme(numeroUniforme);
        ConvocatoriaPartido guardada = convocatoriaDao.save(convocatoria);
        registrarEvento(partido, jugador, TipoEventoPartido.ENTRA_TITULAR);
        return guardada;
    }

    public List<DtoJugadorDisponiblePartido> jugadoresDelEquipo(long partidoId, long equipoId) {
        Partido partido = partido(partidoId);
        Equipo equipo = equiposDelPartido(partido).stream()
                .filter(item -> item.getId() == equipoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este partido"));
        return participacionDao.findByTorneo(partido.getTorneo()).stream()
                .filter(participacion -> participacion.getEquipo().getId() == equipo.getId())
                .map(participacion -> participacion.getJugador())
                .filter(jugador -> jugador.getEstadoJugador() == null || jugador.getEstadoJugador() == EstadoJugador.ACTIVO)
                .map(jugador -> new DtoJugadorDisponiblePartido(
                        jugador.getId(),
                        jugador.getNombre(),
                        jugador.getIdentificacion(),
                        jugador.getNumeroCamiseta()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoSancionJugadorPartido> sancionesActivasDelPartido(long partidoId) {
        Partido partido = partido(partidoId);
        return equiposDelPartido(partido).stream()
                .flatMap(equipo -> sancionService.listarActivasDelEquipo(partido.getTorneo(), equipo).stream())
                .map(sancion -> new DtoSancionJugadorPartido(
                        sancion.getId(),
                        sancion.getJugador() == null ? null : new DtoSancionJugadorPartido.DtoJugador(
                                sancion.getJugador().getId(),
                                sancion.getJugador().getNombre()),
                        sancion.isActiva(),
                        sancion.getPartidosPendientes(),
                        sancion.isPermanente(),
                        sancion.getExencionPartidoId()))
                .toList();
    }

    public Jugador buscarPorIdentificacion(long partidoId, String identificacion, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        Jugador jugador = jugadorDao.findByIdentificacion(identificacion.trim())
            .orElseThrow(() -> new IllegalArgumentException("No se encontró un jugador con esa identificación"));
        validarEquipoDelDelegado(partido, equipoDeJugadorEnPartido(partido, jugador), usuario);
        return jugador;
    }

    @Transactional
    public ConvocatoriaPartido agregar(
            long partidoId, long jugadorId, boolean titular, Integer numeroUniforme, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        if (partido.getEstadoPartido() == EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Durante el partido solo puedes agregar un jugador para ocupar una posición vacía");
        }
        Jugador jugador = jugadorDao.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        sancionService.validarElegibilidad(partido, jugador);
        Equipo equipo = equipoDeJugadorEnPartido(partido, jugador);
        validarEquipoDelDelegado(partido, equipo, usuario);
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent()) {
            throw new IllegalArgumentException("El jugador ya está agregado a este partido");
        }
        int maxConvocados = maximoConvocados(partido);
        if (convocatoriaDao.countByPartidoAndJugadorEquipo(partido, equipo) >= maxConvocados) {
            throw new IllegalArgumentException("Se alcanzó el máximo de jugadores convocados para " + equipo.getNombre());
        }
        if (titular) {
            validarMaxTitulares(partido, equipo);
        }
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        if (titular) {
            validarNumeroUniforme(partido, equipo, convocatoria, numeroUniforme == null ? -1 : numeroUniforme);
            convocatoria.setNumeroUniforme(numeroUniforme);
        }
        convocatoria.setTitular(titular);
        convocatoria.setFueTitular(titular);
        if (titular) {
            convocatoria.setPosicionAlineacion(siguientePosicionAlineacion(partido, equipo));
        }
        return convocatoriaDao.save(convocatoria);
    }

    @Transactional
    public ConvocatoriaPartido agregarPorIdentificacion(
            long partidoId, String identificacion, boolean titular, Integer numeroUniforme, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        if (partido.getEstadoPartido() == EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Durante el partido solo puedes agregar un jugador para ocupar una posición vacía");
        }
        Jugador jugador = jugadorDao.findByIdentificacion(identificacion.trim()).orElse(null);
        if (jugador == null) {
            Equipo equipo = equipoDelGestor(partido, usuario);
            if (equipo.getDelegado() == null || !identificacion.trim().equals(equipo.getDelegado().getIdentificacion())) {
                throw new IllegalArgumentException("No se encontró un jugador con esa identificación");
            }
            jugador = new Jugador();
            jugador.setIdentificacion(equipo.getDelegado().getIdentificacion());
            jugador.setNombre(equipo.getDelegado().getNombre());
            jugador.setNumeroCelular(equipo.getDelegado().getNumeroCelular());
            jugador.setEquipo(equipo);
            jugador = jugadorDao.save(jugador);
        }
        return agregar(partidoId, jugador.getId(), titular, numeroUniforme, usuarioId);
    }

    @Transactional
    public ConvocatoriaPartido cambiarTitular(
            long convocatoriaId, boolean titular, Integer numeroUniforme, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        Usuario usuario = usuario(usuarioId);
        validarGestor(convocatoria.getPartido(), usuario);
        validarEstadoEditable(convocatoria.getPartido());
        if (convocatoria.getPartido().getEstadoPartido() == EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Durante el partido debes registrar una sustitución seleccionando quién sale y quién entra");
        }
        if (titular && !convocatoria.isTitular()) {
            Partido partido = convocatoria.getPartido();
            Equipo equipo = equipoDeJugadorEnPartido(partido, convocatoria.getJugador());
            validarMaxTitulares(partido, equipo);
            validarNumeroUniforme(partido, equipo, convocatoria, numeroUniforme == null ? -1 : numeroUniforme);
            convocatoria.setNumeroUniforme(numeroUniforme);
        }
        convocatoria.setTitular(titular);
        if (titular) {
            Equipo equipo = equipoDeJugadorEnPartido(convocatoria.getPartido(), convocatoria.getJugador());
            convocatoria.setPosicionAlineacion(siguientePosicionAlineacion(convocatoria.getPartido(), equipo));
            convocatoria.setFueTitular(true);
        } else {
            convocatoria.setPosicionAlineacion(null);
        }
        registrarEvento(convocatoria.getPartido(), convocatoria.getJugador(), titular ? TipoEventoPartido.ENTRA_TITULAR : TipoEventoPartido.SALE_TITULAR);
        return convocatoriaDao.save(convocatoria);
    }

    @Transactional
    public void sustituir(long partidoId, long titularId, long suplenteId, int numeroUniforme, long usuarioId) {
        sustituir(partidoId, titularId, suplenteId, numeroUniforme, 0, usuarioId);
    }

    @Transactional
    public void sustituir(
            long partidoId, long titularId, long suplenteId, int numeroUniforme, int minuto, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestorOrganizador(partido, usuario);
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO || minuto < 0) {
            throw new IllegalArgumentException("Las sustituciones solo se pueden registrar durante el partido y con minuto no negativo");
        }
        ConvocatoriaPartido sale = convocatoriaDao.findById(titularId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador titular no existe en este partido"));
        ConvocatoriaPartido entra = convocatoriaDao.findById(suplenteId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador suplente no existe en este partido"));
        Equipo equipoEntra = equipoDeJugadorEnPartido(partido, entra.getJugador());
        validarElegibilidadParaSustitucion(partido, equipoEntra, entra.getJugador());
        validarNumeroUniforme(
                partido,
                equipoEntra,
                entra,
                numeroUniforme);
        entra.setNumeroUniforme(numeroUniforme);
        sustituirConvocados(partido, sale, entra, minuto);
    }

    @Transactional
    public void sustituirJugadorInscrito(
            long partidoId, long titularId, long jugadorId, int numeroUniforme, long usuarioId) {
        sustituirJugadorInscrito(partidoId, titularId, jugadorId, numeroUniforme, 0, usuarioId);
    }

    @Transactional
    public void sustituirJugadorInscrito(
            long partidoId, long titularId, long jugadorId, int numeroUniforme, int minuto, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestorOrganizador(partido, usuario(usuarioId));
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO || minuto < 0) {
            throw new IllegalArgumentException("Las sustituciones solo se pueden registrar durante el partido y con minuto no negativo");
        }
        ConvocatoriaPartido sale = convocatoriaDao.findById(titularId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador titular no existe en este partido"));
        if (sale.getPartido().getId() != partidoId || !sale.isTitular()) {
            throw new IllegalArgumentException("Selecciona un titular de este partido");
        }

        Jugador jugador = jugadorDao.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException("El suplente no existe"));
        Equipo equipoSale = equipoDeJugadorEnPartido(partido, sale.getJugador());
        Equipo equipoEntra = equipoDeJugadorEnPartido(partido, jugador);
        if (!Objects.equals(equipoSale.getId(), equipoEntra.getId())) {
            throw new IllegalArgumentException("Los jugadores deben pertenecer al mismo equipo");
        }
        validarElegibilidadParaSustitucion(partido, equipoEntra, jugador);

        ConvocatoriaPartido entra = convocatoriaDao.findByPartidoAndJugador(partido, jugador).orElse(null);
        if (entra == null) {
            if (convocatoriaDao.countByPartidoAndJugadorEquipo(partido, equipoEntra) >= maximoConvocados(partido)) {
                throw new IllegalArgumentException("Se alcanzó el máximo de jugadores convocados para " + equipoEntra.getNombre());
            }
            entra = new ConvocatoriaPartido();
            entra.setPartido(partido);
            entra.setJugador(jugador);
        }
        validarNumeroUniforme(partido, equipoEntra, entra, numeroUniforme);
        entra.setNumeroUniforme(numeroUniforme);
        if (entra.getId() == null) {
            entra = convocatoriaDao.save(entra);
        }
        sustituirConvocados(partido, sale, entra, minuto);
    }

    /**
     * Angular API: POST /partido/{partidoId}/solicitudes-sustitucion
     * Body: { titularId, suplenteId | jugadorId, numeroUniforme }.
     */
    @Transactional
    public DtoSolicitudSustitucion solicitarSustitucion(
            long partidoId, DtoCrearSolicitudSustitucion solicitud, long usuarioId) {
        Partido partido = partidoDao.findByIdForUpdate(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Solo se pueden solicitar sustituciones durante un partido en proceso");
        }
        if (solicitud == null || solicitud.titularId() == null || solicitud.numeroUniforme() == null
                || solicitud.minuto() == null || solicitud.minuto() < 0
                || ((solicitud.suplenteId() == null) == (solicitud.jugadorId() == null))) {
            throw new IllegalArgumentException("Indica el titular, un suplente, su número de uniforme y el minuto de juego");
        }
        if (solicitud.numeroUniforme() < 0 || solicitud.numeroUniforme() > 99) {
            throw new IllegalArgumentException("El número de uniforme debe estar entre 0 y 99");
        }

        Usuario usuario = usuario(usuarioId);
        if (usuario.getTipoUsuario() != TipoUsuario.DELEGADO) {
            throw new IllegalArgumentException("Solo el delegado de un equipo puede solicitar sustituciones");
        }
        validarGestor(partido, usuario);
        Equipo equipo = equipoDelGestor(partido, usuario);
        if (solicitudSustitucionDao.existsByPartido_IdAndEquipo_IdAndEstado(
                partidoId, equipo.getId(), EstadoSolicitudSustitucion.PENDIENTE)) {
            throw new IllegalArgumentException("Este equipo ya tiene una solicitud de sustitución pendiente");
        }

        ConvocatoriaPartido titular = convocatoria(partidoId, solicitud.titularId());
        if (!titular.isTitular()
                || !Objects.equals(equipoDeJugadorEnPartido(partido, titular.getJugador()).getId(), equipo.getId())) {
            throw new IllegalArgumentException("Selecciona un titular de tu equipo en este partido");
        }

        ConvocatoriaPartido suplente = null;
        Jugador jugadorEntrante;
        if (solicitud.suplenteId() != null) {
            suplente = convocatoria(partidoId, solicitud.suplenteId());
            if (suplente.isTitular()) {
                throw new IllegalArgumentException("El jugador suplente ya es titular");
            }
            jugadorEntrante = suplente.getJugador();
        } else {
            jugadorEntrante = jugadorDao.findById(solicitud.jugadorId())
                    .orElseThrow(() -> new IllegalArgumentException("El suplente no existe"));
            ConvocatoriaPartido convocatoriaExistente =
                    convocatoriaDao.findByPartidoAndJugador(partido, jugadorEntrante).orElse(null);
            if (convocatoriaExistente != null && convocatoriaExistente.isTitular()) {
                throw new IllegalArgumentException("El jugador suplente ya es titular");
            }
        }
        if (!Objects.equals(equipoDeJugadorEnPartido(partido, jugadorEntrante).getId(), equipo.getId())) {
            throw new IllegalArgumentException("El suplente debe pertenecer a tu equipo");
        }

        SolicitudSustitucionPartido nueva = new SolicitudSustitucionPartido();
        nueva.setPartido(partido);
        nueva.setEquipo(equipo);
        nueva.setSolicitante(usuario);
        nueva.setTitular(titular);
        nueva.setSuplente(suplente);
        nueva.setJugadorEntrante(jugadorEntrante);
        nueva.setNumeroUniforme(solicitud.numeroUniforme());
        nueva.setMinuto(solicitud.minuto());
        nueva.setEstado(EstadoSolicitudSustitucion.PENDIENTE);
        nueva.setCreadaEn(LocalDateTime.now());
        nueva = solicitudSustitucionDao.save(nueva);
        notificarSolicitudSustitucion(nueva);
        return dtoSolicitudSustitucion(nueva);
    }

    /** Angular API: GET /partido/{partidoId}/solicitudes-sustitucion. */
    @Transactional(readOnly = true)
    public List<DtoSolicitudSustitucion> listarSolicitudesSustitucion(long partidoId, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        List<SolicitudSustitucionPartido> solicitudes;
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO) {
            validarGestor(partido, usuario);
            Equipo equipo = equipoDelGestor(partido, usuario);
            solicitudes = solicitudSustitucionDao.findByPartidoAndEquipoOrderByCreadaEnDesc(partido, equipo);
        } else {
            validarGestorOrganizador(partido, usuario);
            solicitudes = solicitudSustitucionDao.findByPartidoOrderByCreadaEnDesc(partido);
        }
        return solicitudes.stream()
                .map(this::dtoSolicitudSustitucion)
                .toList();
    }

    /** Angular API: PUT /partido/solicitudes-sustitucion/{solicitudId}/aprobar. */
    @Transactional
    public DtoSolicitudSustitucion aprobarSolicitudSustitucion(long solicitudId, long usuarioId) {
        SolicitudSustitucionPartido solicitud = solicitudSustitucionDao.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de sustitución no existe"));
        partidoDao.findByIdForUpdate(solicitud.getPartido().getId())
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        solicitud = solicitudSustitucionDao.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de sustitución no existe"));
        Partido partido = solicitud.getPartido();
        validarGestorOrganizador(partido, usuario(usuarioId));
        if (solicitud.getEstado() != EstadoSolicitudSustitucion.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya fue resuelta");
        }
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Solo se puede aprobar una sustitución durante un partido en proceso");
        }
        int minutoCambio = solicitud.getMinuto() == null ? 0 : solicitud.getMinuto();

        if (solicitud.getSuplente() != null) {
            sustituir(partido.getId(), solicitud.getTitular().getId(), solicitud.getSuplente().getId(),
                solicitud.getNumeroUniforme(), minutoCambio, usuarioId);
        } else {
            sustituirJugadorInscrito(partido.getId(), solicitud.getTitular().getId(),
                    solicitud.getJugadorEntrante().getId(), solicitud.getNumeroUniforme(), minutoCambio, usuarioId);
        }
        solicitud.setEstado(EstadoSolicitudSustitucion.APROBADA);
        registrarDecisionSolicitud(solicitud, usuarioId);
        solicitudSustitucionDao.save(solicitud);
        notificarDecisionSolicitud(solicitud);
        return dtoSolicitudSustitucion(solicitud);
    }

    /** Angular API: PUT /partido/solicitudes-sustitucion/{solicitudId}/rechazar. */
    @Transactional
    public DtoSolicitudSustitucion rechazarSolicitudSustitucion(long solicitudId, long usuarioId) {
        SolicitudSustitucionPartido solicitud = solicitudSustitucionDao.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de sustitución no existe"));
        partidoDao.findByIdForUpdate(solicitud.getPartido().getId())
                .orElseThrow(() -> new IllegalArgumentException("El partido no existe"));
        solicitud = solicitudSustitucionDao.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de sustitución no existe"));
        validarGestorOrganizador(solicitud.getPartido(), usuario(usuarioId));
        if (solicitud.getEstado() != EstadoSolicitudSustitucion.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya fue resuelta");
        }
        solicitud.setEstado(EstadoSolicitudSustitucion.RECHAZADA);
        registrarDecisionSolicitud(solicitud, usuarioId);
        solicitudSustitucionDao.save(solicitud);
        notificarDecisionSolicitud(solicitud);
        return dtoSolicitudSustitucion(solicitud);
    }

    private void registrarDecisionSolicitud(SolicitudSustitucionPartido solicitud, long usuarioId) {
        solicitud.setResueltaPor(usuario(usuarioId));
        solicitud.setResueltaEn(LocalDateTime.now());
    }

    private DtoSolicitudSustitucion dtoSolicitudSustitucion(SolicitudSustitucionPartido solicitud) {
        return new DtoSolicitudSustitucion(
                solicitud.getId(),
                solicitud.getPartido().getId(),
                solicitud.getEquipo().getId(),
                solicitud.getEquipo().getNombre(),
                solicitud.getSolicitante().getId(),
                solicitud.getTitular().getId(),
                solicitud.getSuplente() == null ? null : solicitud.getSuplente().getId(),
                solicitud.getSuplente() == null ? solicitud.getJugadorEntrante().getId() : null,
                solicitud.getNumeroUniforme(),
                solicitud.getMinuto() == null ? 0 : solicitud.getMinuto(),
                solicitud.getEstado(),
                solicitud.getCreadaEn(),
                solicitud.getResueltaPor() == null ? null : solicitud.getResueltaPor().getId(),
                solicitud.getResueltaEn());
    }

    private void notificarSolicitudSustitucion(SolicitudSustitucionPartido solicitud) {
        Set<Long> destinatarios = new java.util.HashSet<>();
        Partido partido = solicitud.getPartido();
        if (partido.getTorneo().getEncargadoTorneo() != null) {
            destinatarios.add(partido.getTorneo().getEncargadoTorneo().getId());
        }
        ayudanteTorneoDao.findByTorneo(partido.getTorneo()).stream()
                .map(ayudante -> ayudante.getUsuario().getId())
                .forEach(destinatarios::add);
        for (Long usuarioId : destinatarios) {
            Usuario destinatario = usuarioDao.findById(usuarioId).orElse(null);
            if (destinatario == null) continue;
            NotificacionUsuario notificacion = nuevaNotificacion(
                    destinatario,
                    "Solicitud de sustitución",
                    solicitud.getEquipo().getNombre() + " solicitó sustituir a "
                            + solicitud.getTitular().getJugador().getNombre() + " durante el partido.",
                    partido,
                    "SOLICITUD_SUSTITUCION",
                    solicitud.getId());
            notificacionDao.save(notificacion);
        }
    }

    private void notificarDecisionSolicitud(SolicitudSustitucionPartido solicitud) {
        String decision = solicitud.getEstado() == EstadoSolicitudSustitucion.APROBADA
                ? "aprobada" : "rechazada";
        notificacionDao.save(nuevaNotificacion(
                solicitud.getSolicitante(),
                "Solicitud de sustitución " + decision,
                "Tu solicitud para sustituir a " + solicitud.getTitular().getJugador().getNombre()
                        + " fue " + decision + ".",
                solicitud.getPartido(),
                "SOLICITUD_SUSTITUCION",
                solicitud.getId()));
    }

    private NotificacionUsuario nuevaNotificacion(
            Usuario destinatario, String titulo, String mensaje, Partido partido,
            String referenciaTipo, Long referenciaId) {
        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setUsuario(destinatario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setRuta("/auth/partidos/partido/" + partido.getId());
        notificacion.setTipo("ACCION");
        notificacion.setReferenciaTipo(referenciaTipo);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacion.setLeida(false);
        return notificacion;
    }

    private void validarElegibilidadParaSustitucion(Partido partido, Equipo equipo, Jugador jugador) {
        sancionService.validarElegibilidad(partido, jugador);
        if (partido.getTorneo().getValorInscripcion() <= 0) return;
        long pagado = pagoInscripcionDao.findByEquipoAndTorneoOrderByFechaPagoDesc(equipo, partido.getTorneo())
                .stream()
                .filter(pago -> pago.getEstado() == com.example.torneos.enums.EstadoPago.PARCIAL
                        || pago.getEstado() == com.example.torneos.enums.EstadoPago.PAGADO)
                .mapToLong(com.example.torneos.entities.PagoInscripcion::getMonto)
                .sum();
        if (partido.getTorneo().getValorInscripcion() > pagado) {
            throw new IllegalArgumentException("El equipo tiene un saldo pendiente de inscripción y no puede realizar sustituciones");
        }
    }

    private void validarNumeroUniforme(
            Partido partido, Equipo equipo, ConvocatoriaPartido convocatoria, int numeroUniforme) {
        if (numeroUniforme < 0 || numeroUniforme > 99) {
            throw new IllegalArgumentException("El número de uniforme debe estar entre 0 y 99");
        }
        Long convocatoriaId = convocatoria.getId();
        boolean numeroEnUso = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(item -> !Objects.equals(item.getId(), convocatoriaId))
                .anyMatch(item -> Objects.equals(
                        item.getNumeroUniforme() == null && item.getJugador() != null
                                ? item.getJugador().getNumeroCamiseta()
                                : item.getNumeroUniforme(),
                        numeroUniforme));
        if (numeroEnUso) {
            throw new IllegalArgumentException("El número de uniforme ya está asignado a otro jugador de este equipo en el partido");
        }
    }

    private void sustituirConvocados(Partido partido, ConvocatoriaPartido sale, ConvocatoriaPartido entra, int minuto) {
        if (sale.getPartido().getId() != partido.getId() || entra.getPartido().getId() != partido.getId()) {
            throw new IllegalArgumentException("Los jugadores no pertenecen a este partido");
        }
        if (!sale.isTitular() || entra.isTitular()) throw new IllegalArgumentException("Selecciona un titular que sale y un suplente que entra");
        Equipo equipoSale = equipoDeJugadorEnPartido(partido, sale.getJugador());
        Equipo equipoEntra = equipoDeJugadorEnPartido(partido, entra.getJugador());
        if (!Objects.equals(equipoSale.getId(), equipoEntra.getId())) throw new IllegalArgumentException("Los jugadores deben pertenecer al mismo equipo");
        if (eventoDao.existsByPartidoAndJugadorAndTipo(partido, sale.getJugador(), TipoEventoPartido.TARJETA_ROJA) || sale.isExpulsado()) throw new IllegalArgumentException("Un jugador expulsado no puede salir por cambio");
        if (eventoDao.existsByPartidoAndJugadorAndTipo(partido, entra.getJugador(), TipoEventoPartido.TARJETA_ROJA) || entra.isExpulsado()) throw new IllegalArgumentException("Un suplente expulsado no puede entrar");
        ModoCambioJugador modo = partido.getTorneo().getModoCambioJugador() == null ? ModoCambioJugador.LIMITADOS : partido.getTorneo().getModoCambioJugador();
        if (modo != ModoCambioJugador.SALIR_ENTRAR && entra.isFueTitular()) throw new IllegalArgumentException("Este suplente ya fue titular y no puede volver a entrar");
        if (modo == ModoCambioJugador.LIMITADOS && partido.getTorneo().getMaximoCambios() > 0 && cambiosDelEquipo(partido, equipoSale) >= partido.getTorneo().getMaximoCambios()) throw new IllegalArgumentException("Se alcanzó el límite de cambios del equipo");
        sale.setTitular(false);
        sale.setCambiosRealizados(sale.getCambiosRealizados() + 1);
        entra.setTitular(true);
        entra.setFueTitular(true);
        entra.setCambiosRealizados(entra.getCambiosRealizados() + 1);
        convocatoriaDao.save(sale);
        convocatoriaDao.save(entra);
        guardarEvento(partido, sale.getJugador(), TipoEventoPartido.SALE_TITULAR, minuto);
        guardarEvento(partido, entra.getJugador(), TipoEventoPartido.ENTRA_TITULAR, minuto);
    }

    private long cambiosDelEquipo(Partido partido, Equipo equipo) {
        return convocatoriasDelEquipo(partido, equipo).stream()
                .mapToLong(ConvocatoriaPartido::getCambiosRealizados).sum() / 2;
    }

    @Transactional
    public ConvocatoriaPartido cambiarNumeroUniforme(long convocatoriaId, int numero, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        validarGestor(convocatoria.getPartido(), usuario(usuarioId));
        validarEstadoEditable(convocatoria.getPartido());
        if (numero < 0 || numero > 99) {
            throw new IllegalArgumentException("El número de uniforme debe estar entre 0 y 99");
        }
        Equipo equipo = equipoDeJugadorEnPartido(convocatoria.getPartido(), convocatoria.getJugador());
        boolean numeroEnUso = convocatoriasDelEquipo(convocatoria.getPartido(), equipo).stream()
                .filter(item -> !Objects.equals(item.getId(), convocatoriaId))
                .anyMatch(item -> Objects.equals(
                        item.getNumeroUniforme() == null && item.getJugador() != null
                                ? item.getJugador().getNumeroCamiseta()
                                : item.getNumeroUniforme(),
                        numero));
        if (numeroEnUso) {
            throw new IllegalArgumentException("El número de uniforme ya está asignado a otro jugador de este equipo en el partido");
        }
        convocatoria.setNumeroUniforme(numero);
        return convocatoriaDao.save(convocatoria);
    }

    @Transactional(readOnly = true)
    public List<DtoEventoPartido> eventos(long partidoId) {
        return eventoDao.findByPartidoOrderByMinutoAscIdAsc(partido(partidoId)).stream()
                .map(evento -> {
                    Jugador jugador = evento.getJugador();
                    Equipo equipo = evento.getEquipoAnotador();
                    if (equipo == null && jugador != null) {
                        equipo = jugador.getEquipo();
                    }
                    return new DtoEventoPartido(
                            evento.getId(),
                            evento.getTipo(),
                            evento.getMinuto(),
                            jugador == null ? null : jugador.getId(),
                            evento.getAsistidor() == null ? null : evento.getAsistidor().getId(),
                            evento.isPenalti(),
                            evento.isDobleAmarilla(),
                            equipo == null ? null : equipo.getId());
                })
                .toList();
    }

    @Transactional
    public com.example.torneos.entities.EventoPartido registrarEvento(long partidoId, long jugadorId, TipoEventoPartido tipo, int minuto, long usuarioId) {
        return registrarEvento(partidoId, jugadorId, tipo, minuto, false, usuarioId);
    }

    @Transactional
    public com.example.torneos.entities.EventoPartido registrarEvento(
            long partidoId, long jugadorId, TipoEventoPartido tipo, int minuto,
            boolean dobleAmarilla, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestorOrganizador(partido, usuario(usuarioId));
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Los goles y tarjetas solo se pueden registrar durante el partido");
        }
        Jugador jugador = jugadorDao.findById(jugadorId).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isEmpty()) throw new IllegalArgumentException("El jugador no está convocado en este partido");
        if (tipo != TipoEventoPartido.TARJETA_AMARILLA && tipo != TipoEventoPartido.TARJETA_ROJA) {
            throw new IllegalArgumentException("Los goles y autogoles deben registrarse con los datos del equipo y del jugador");
        }
        if (dobleAmarilla && tipo != TipoEventoPartido.TARJETA_ROJA) {
            throw new IllegalArgumentException("La doble amarilla solo puede registrarse como tarjeta roja");
        }
        EventoPartido evento = guardarEvento(partido, jugador, tipo, minuto);
        evento.setDobleAmarilla(dobleAmarilla);
        evento = eventoDao.save(evento);
        sancionService.registrarTarjeta(evento);
        if (tipo == TipoEventoPartido.TARJETA_ROJA) {
            convocatoriaDao.findByPartidoAndJugador(partido, jugador).ifPresent(convocatoria -> { convocatoria.setExpulsado(true); convocatoriaDao.save(convocatoria); });
        }
        return evento;
    }

    @Transactional
    public Partido registrarGol(long partidoId, DtoRegistrarGol solicitud, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestorOrganizador(partido, usuario(usuarioId));
        if (partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("Los goles solo se pueden registrar durante el partido");
        }
        if (solicitud.getMinuto() < 0) {
            throw new IllegalArgumentException("El minuto del gol no puede ser negativo");
        }

        Equipo equipoAnotador = equipoParticipante(partido, solicitud.getEquipoAnotadorId());
        Jugador jugador = jugadorConvocado(partido, solicitud.getJugadorId());
        Equipo equipoJugador = equipoDeJugadorEnPartido(partido, jugador);
        if (equipoJugador == null) {
            throw new IllegalArgumentException("El jugador no pertenece a un equipo");
        }

        if (solicitud.isAutogol()) {
            if (solicitud.isPenalti()) {
                throw new IllegalArgumentException("Un autogol no puede registrarse como penalti");
            }
            if (solicitud.getAsistenciaId() != null) {
                throw new IllegalArgumentException("Un autogol no puede tener asistencia");
            }
            if (equipoJugador.getId() == equipoAnotador.getId()) {
                throw new IllegalArgumentException("En un autogol, el jugador debe pertenecer al equipo contrario");
            }
            EventoPartido gol = guardarEvento(partido, jugador, TipoEventoPartido.AUTOGOL, solicitud.getMinuto());
            gol.setEquipoAnotador(equipoAnotador);
        } else {
            if (equipoJugador.getId() != equipoAnotador.getId()) {
                throw new IllegalArgumentException("El autor del gol debe pertenecer al equipo que anota");
            }
            EventoPartido gol = guardarEvento(partido, jugador, TipoEventoPartido.GOL, solicitud.getMinuto());
            gol.setEquipoAnotador(equipoAnotador);
            gol.setPenalti(solicitud.isPenalti());
            if (solicitud.getAsistenciaId() != null) {
                Jugador asistente = jugadorConvocado(partido, solicitud.getAsistenciaId());
                if (!Objects.equals(equipoDeJugadorEnPartido(partido, asistente).getId(), equipoAnotador.getId())) {
                    throw new IllegalArgumentException("La asistencia debe pertenecer al equipo que anota");
                }
                if (asistente.getId() == jugador.getId()) {
                    throw new IllegalArgumentException("El autor del gol no puede asistirse a sí mismo");
                }
                gol.setAsistidor(asistente);
                guardarEvento(partido, asistente, TipoEventoPartido.ASISTENCIA, solicitud.getMinuto());
            }
        }

        if (partido.getEquipoLocal() != null && equipoAnotador.getId() == partido.getEquipoLocal().getId()) {
            partido.setAnotacionesEquipoLocal(partido.getAnotacionesEquipoLocal() + 1);
        } else {
            partido.setAnotacionesEquipoVisitante(partido.getAnotacionesEquipoVisitante() + 1);
        }
        return partidoDao.save(partido);
    }

    private Equipo equipoParticipante(Partido partido, Long equipoId) {
        if (equipoId == null) {
            throw new IllegalArgumentException("Selecciona el equipo al que se le suma el gol");
        }
        if (partido.getEquipoLocal() != null && partido.getEquipoLocal().getId() == equipoId) {
            return partido.getEquipoLocal();
        }
        if (partido.getEquipoVisitante() != null && partido.getEquipoVisitante().getId() == equipoId) {
            return partido.getEquipoVisitante();
        }
        throw new IllegalArgumentException("El equipo seleccionado no participa en este partido");
    }

    private Jugador jugadorConvocado(Partido partido, Long jugadorId) {
        if (jugadorId == null) {
            throw new IllegalArgumentException("Selecciona el jugador que realizó el gol");
        }
        Jugador jugador = jugadorDao.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isEmpty()) {
            throw new IllegalArgumentException("El jugador no está convocado en este partido");
        }
        return jugador;
    }

    private void registrarEvento(Partido partido, Jugador jugador, TipoEventoPartido tipo) {
        if (partido.getEstadoPartido() == EstadoPartido.EN_PROCESO) guardarEvento(partido, jugador, tipo, 0);
    }

    private com.example.torneos.entities.EventoPartido guardarEvento(Partido partido, Jugador jugador, TipoEventoPartido tipo, int minuto) {
        com.example.torneos.entities.EventoPartido evento = new com.example.torneos.entities.EventoPartido();
        evento.setPartido(partido);
        evento.setJugador(jugador);
        evento.setTipo(tipo);
        evento.setMinuto(Math.max(minuto, 0));
        evento.setRegistradoEn(LocalDateTime.now());
        evento.setEquipoAnotador(equipoDeJugadorEnPartido(partido, jugador));
        return eventoDao.save(evento);
    }

    @Transactional
    public void eliminar(long convocatoriaId, long usuarioId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        validarGestor(convocatoria.getPartido(), usuario(usuarioId));
        validarEstadoEditable(convocatoria.getPartido());
        convocatoriaDao.delete(convocatoria);
    }

    public void validarTitulares(long partidoId, long usuarioId) {
        Partido partido = partido(partidoId);
        validarGestor(partido, usuario(usuarioId));
        for (Equipo equipo : equiposDelPartido(partido)) {
            long titulares = convocatoriasDelEquipo(partido, equipo).stream()
                    .filter(ConvocatoriaPartido::isTitular)
                    .count();
            int minimo = minimoTitulares(partido);
            int maximo = maximoTitulares(partido);
            if (titulares < minimo || titulares > maximo) {
                throw new IllegalArgumentException("La lista de titulares de " + equipo.getNombre() + " debe tener entre " + minimo + " y " + maximo + " jugadores");
            }
        }
    }

    @Transactional
    public InvitacionPartido crearInvitacion(long partidoId, long equipoId, long usuarioId) {
        Partido partido = partido(partidoId);
        Usuario usuario = usuario(usuarioId);
        validarGestor(partido, usuario);
        validarEstadoEditable(partido);
        Equipo equipo = equipo(equipoId);
        if (equipo.getId() != partido.getEquipoLocal().getId() && equipo.getId() != partido.getEquipoVisitante().getId()) {
            throw new IllegalArgumentException("El equipo no participa en este partido");
        }
        validarEquipoDelDelegado(partido, equipo, usuario);
        InvitacionPartido invitacion = new InvitacionPartido();
        invitacion.setPartido(partido);
        invitacion.setEquipo(equipo);
        invitacion.setToken(UUID.randomUUID().toString());
        invitacion.setFechaExpiracion(LocalDateTime.now().plusDays(2));
        return invitacionDao.save(invitacion);
    }

    @Transactional
    public ConvocatoriaPartido aceptarInvitacion(String token, long usuarioId) {
        InvitacionPartido invitacion = invitacionDao.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de invitación no es válido"));
        if (invitacion.isUsada() || invitacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de invitación expiró o ya fue utilizado");
        }
        Usuario usuario = usuario(usuarioId);
        if (usuario.getTipoUsuario() != TipoUsuario.JUGADOR) {
            throw new IllegalArgumentException("Solo un usuario de tipo jugador puede aceptar esta invitación");
        }
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) {
            throw new IllegalArgumentException("El usuario jugador debe tener una identificación registrada");
        }
        Jugador jugador = jugadorDao.findByIdentificacion(usuario.getIdentificacion()).orElseGet(() -> {
            Jugador nuevo = new Jugador();
            nuevo.setIdentificacion(usuario.getIdentificacion());
            nuevo.setNombre(usuario.getNombre());
            nuevo.setNumeroCelular(usuario.getNumeroCelular());
            nuevo.setCorreoElectronico(usuario.getCorreoElectronico());
            nuevo.setEquipo(invitacion.getEquipo());
            nuevo.getEquipos().add(invitacion.getEquipo());
            return jugadorDao.save(nuevo);
        });
        if (jugador.getEquipo() == null || jugador.getEquipo().getId() != invitacion.getEquipo().getId()) {
            if (jugador.getEquipos() == null || jugador.getEquipos().stream().noneMatch(item -> item.getId() == invitacion.getEquipo().getId())) {
                throw new IllegalArgumentException("El jugador no pertenece al equipo invitado");
            }
        }
        Partido partido = invitacion.getPartido();
        validarEstadoEditable(partido);
        sancionService.validarElegibilidad(partido, jugador);
        if (convocatoriaDao.countByPartidoAndJugadorEquipo(partido, invitacion.getEquipo()) >= maximoConvocados(partido)) {
            throw new IllegalArgumentException("Se alcanzó el máximo de jugadores convocados para " + invitacion.getEquipo().getNombre());
        }
        invitacion.setUsada(true);
        invitacionDao.save(invitacion);
        return agregarInterno(partido, jugador, false);
    }

    private ConvocatoriaPartido agregarInterno(Partido partido, Jugador jugador, boolean titular) {
        if (convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent()) {
            throw new IllegalArgumentException("El jugador ya está agregado a este partido");
        }
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        convocatoria.setTitular(titular);
        return convocatoriaDao.save(convocatoria);
    }

    private void validarMaxTitulares(Partido partido, Equipo equipo) {
        int maximo = maximoTitulares(partido);
        long titulares = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(ConvocatoriaPartido::isTitular)
                .count();
        if (titulares >= maximo) {
            throw new IllegalArgumentException("No se pueden registrar más de " + maximo + " titulares");
        }
    }

    private int maximoConvocados(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 22 : partido.getTorneo().getDeporte().getMaximoConvocados();
    }

    private int minimoTitulares(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 7 : partido.getTorneo().getDeporte().getMinimoTitulares();
    }

    private int maximoTitulares(Partido partido) {
        return partido.getTorneo().getDeporte() == null ? 11 : partido.getTorneo().getDeporte().getMaximoTitulares();
    }

    private List<Equipo> equiposDelPartido(Partido partido) {
        return java.util.stream.Stream.of(partido.getEquipoLocal(), partido.getEquipoVisitante())
                .filter(java.util.Objects::nonNull).toList();
    }

    private void validarEstadoEditable(Partido partido) {
        if (partido.getEstadoPartido() != EstadoPartido.PROGRAMADO && partido.getEstadoPartido() != EstadoPartido.EN_PROCESO) {
            throw new IllegalArgumentException("La lista solo se puede modificar cuando el partido está programado o en proceso");
        }
    }

    private void validarPermisoAlineacionDurantePartido(Partido partido, Usuario usuario) {
        if (partido.getEstadoPartido() == EstadoPartido.EN_PROCESO
                && usuario.getTipoUsuario() != TipoUsuario.DELEGADO) {
            throw new IllegalArgumentException("Durante el partido solo el delegado puede modificar la formación o las posiciones de su equipo");
        }
    }

    private void validarGestor(Partido partido, Usuario usuario) {
        boolean propietario = partido.getTorneo().getEncargadoTorneo() != null &&
                partido.getTorneo().getEncargadoTorneo().getId() == usuario.getId();
        boolean ayudante = ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), usuario).isPresent();
        if (propietario || ayudante) {
            validarNoJugador(partido, usuario);
            return;
        }
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoLocal(), usuario) || usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoVisitante(), usuario)) {
            return;
        }
        throw new IllegalArgumentException("Solo el organizador o el delegado del equipo puede modificar la lista");
    }

    private Equipo equipoDelPartido(Partido partido, long equipoId) {
        return equiposDelPartido(partido).stream()
                .filter(equipo -> equipo.getId() == equipoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El equipo no participa en este partido"));
    }

    private List<ConvocatoriaPartido> convocatoriasDelEquipo(Partido partido, Equipo equipo) {
        return convocatoriaDao.findByPartido(partido).stream()
                .filter(convocatoria -> convocatoria.getJugador() != null
                        && Objects.equals(equipoDeJugadorEnPartido(partido, convocatoria.getJugador()).getId(), equipo.getId()))
                .toList();
    }

    private int siguientePosicionAlineacion(Partido partido, Equipo equipo) {
        Set<Integer> posicionesOcupadas = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(ConvocatoriaPartido::isTitular)
                .map(ConvocatoriaPartido::getPosicionAlineacion)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        int totalPosiciones = FORMACIONES.get(FORMACION_INICIAL).stream()
                .mapToInt(Integer::intValue)
                .sum();
        for (int posicion = 0; posicion < totalPosiciones; posicion++) {
            if (!posicionesOcupadas.contains(posicion)) {
                return posicion;
            }
        }
        throw new IllegalArgumentException("No hay posiciones disponibles en la formación seleccionada");
    }

    private ConvocatoriaPartido convocatoria(long partidoId, long convocatoriaId) {
        ConvocatoriaPartido convocatoria = convocatoriaDao.findById(convocatoriaId)
                .orElseThrow(() -> new IllegalArgumentException("La convocatoria no existe"));
        if (convocatoria.getPartido() == null || convocatoria.getPartido().getId() != partidoId) {
            throw new IllegalArgumentException("La convocatoria no pertenece a este partido");
        }
        return convocatoria;
    }

    private Map<Long, Integer> posicionesEfectivas(Partido partido, Equipo equipo, int totalPosiciones) {
        List<ConvocatoriaPartido> titulares = convocatoriasDelEquipo(partido, equipo).stream()
                .filter(ConvocatoriaPartido::isTitular)
                .sorted(Comparator.comparing(ConvocatoriaPartido::getPosicionAlineacion,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ConvocatoriaPartido::getId))
                .toList();
        Map<Long, Integer> posiciones = new HashMap<>();
        Set<Integer> ocupadas = new java.util.HashSet<>();
        for (ConvocatoriaPartido titular : titulares) {
            Integer posicion = titular.getPosicionAlineacion();
            if (posicion == null || posicion < 0 || posicion >= totalPosiciones || ocupadas.contains(posicion)) {
                posicion = 0;
                while (posicion < totalPosiciones && ocupadas.contains(posicion)) posicion++;
            }
            if (posicion < totalPosiciones) {
                posiciones.put(titular.getId(), posicion);
                ocupadas.add(posicion);
            }
        }
        return posiciones;
    }

    private void validarGestorOrganizador(Partido partido, Usuario usuario) {
        boolean propietario = partido.getTorneo().getEncargadoTorneo() != null &&
                partido.getTorneo().getEncargadoTorneo().getId() == usuario.getId();
        boolean ayudante = ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), usuario).isPresent();
        if (!propietario && !ayudante) throw new IllegalArgumentException("Solo el organizador o un ayudante puede gestionar el partido");
        validarNoJugador(partido, usuario);
    }

    private void validarNoJugador(Partido partido, Usuario usuario) {
        if (usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank()) return;
        Jugador jugador = jugadorDao.findByIdentificacion(usuario.getIdentificacion()).orElse(null);
        if (jugador == null) return;
        boolean convocado = convocatoriaDao.findByPartidoAndJugador(partido, jugador).isPresent();
        boolean inscritoEnEquipos = jugador.getEquipos() != null && jugador.getEquipos().stream()
                .anyMatch(equipo -> partido.getEquipoLocal() != null && equipo.getId() == partido.getEquipoLocal().getId()
                        || partido.getEquipoVisitante() != null && equipo.getId() == partido.getEquipoVisitante().getId());
        boolean participacionTorneo = participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugador)
                .map(item -> partido.getEquipoLocal() != null && item.getEquipo().getId() == partido.getEquipoLocal().getId()
                        || partido.getEquipoVisitante() != null && item.getEquipo().getId() == partido.getEquipoVisitante().getId())
                .orElse(false);
        if (convocado || inscritoEnEquipos || participacionTorneo) {
            throw new IllegalArgumentException("Un jugador de este partido no puede gestionarlo; debe hacerlo otro ayudante");
        }
    }

    private void validarEquipoDelDelegado(Partido partido, Equipo equipo, Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && (equipo == null || equipo.getId() != equipoDelGestor(partido, usuario).getId())) {
            throw new IllegalArgumentException("El delegado solo puede gestionar los jugadores de su propio equipo");
        }
    }

    private Equipo equipoDelGestor(Partido partido, Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoLocal(), usuario)) return partido.getEquipoLocal();
        if (usuario.getTipoUsuario() == TipoUsuario.DELEGADO && esDelegadoDeEquipo(partido.getEquipoVisitante(), usuario)) return partido.getEquipoVisitante();
        return partido.getEquipoLocal();
    }

    private boolean esDelegadoDeEquipo(Equipo equipo, Usuario usuario) {
        return equipo != null && equipo.getDelegado() != null && equipo.getDelegado().getNumeroCelular() != null && equipo.getDelegado().getNumeroCelular().equals(usuario.getNumeroCelular());
    }

    private Equipo equipoDeJugadorEnPartido(Partido partido, Jugador jugador) {
        Equipo equipoInscrito = participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugador)
                .map(com.example.torneos.entities.ParticipacionJugadorTorneo::getEquipo)
                .orElse(null);
        if (equipoInscrito != null && equiposDelPartido(partido).stream()
                .anyMatch(equipo -> equipo.getId() == equipoInscrito.getId())) {
            return equipoInscrito;
        }
        for (Equipo equipo : equiposDelPartido(partido)) {
            boolean equipoPrincipal = jugador.getEquipo() != null && jugador.getEquipo().getId() == equipo.getId();
            boolean equipoAdicional = jugador.getEquipos() != null &&
                    jugador.getEquipos().stream().anyMatch(inscrito -> inscrito.getId() == equipo.getId());
            if (equipoPrincipal || equipoAdicional) return equipo;
        }
        throw new IllegalArgumentException("El jugador no pertenece a uno de los equipos del partido");
    }

    private Partido partido(long id) { return partidoDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El partido no existe")); }
    private Equipo equipo(long id) { return equipoDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El equipo no existe")); }
    private Usuario usuario(long id) { return usuarioDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El usuario no existe")); }
}
