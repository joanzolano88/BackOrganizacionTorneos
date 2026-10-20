package com.example.torneos.services;

import com.example.torneos.DTO.DtoRegistrarGol;
import com.example.torneos.DTO.DtoJugadorDisponiblePartido;
import com.example.torneos.DTO.DtoCrearSolicitudSustitucion;
import com.example.torneos.dao.AlineacionEquipoPartidoDao;
import com.example.torneos.dao.AyudanteTorneoDao;
import com.example.torneos.dao.ConvocatoriaPartidoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.NotificacionUsuarioDao;
import com.example.torneos.dao.PagoInscripcionDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.PartidoDao;
import com.example.torneos.dao.SolicitudSustitucionPartidoDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.AlineacionEquipoPartido;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.EventoPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.SolicitudSustitucionPartido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.EstadoJugador;
import com.example.torneos.enums.TipoUsuario;
import com.example.torneos.enums.EstadoSolicitudSustitucion;
import com.example.torneos.enums.TipoEventoPartido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.anyLong;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaPartidoServiceTest {
    @Mock private PartidoDao partidoDao;
    @Mock private JugadorDao jugadorDao;
    @Mock private ConvocatoriaPartidoDao convocatoriaDao;
    @Mock private UsuarioDao usuarioDao;
    @Mock private AyudanteTorneoDao ayudanteTorneoDao;
    @Mock private EventoPartidoDao eventoDao;
    @Mock private ParticipacionJugadorTorneoDao participacionDao;
    @Mock private SancionJugadorTorneoService sancionService;
    @Mock private AlineacionEquipoPartidoDao alineacionDao;
    @Mock private SolicitudSustitucionPartidoDao solicitudSustitucionDao;
    @Mock private NotificacionUsuarioDao notificacionDao;
    @Mock private PagoInscripcionDao pagoInscripcionDao;

    @InjectMocks
    private ConvocatoriaPartidoService service;

    private Partido partido;
    private Equipo local;
    private Equipo visitante;
    private Jugador jugadorLocal;
    private Jugador jugadorVisitante;
    private Jugador asistenteLocal;
    private Usuario organizador;

    @BeforeEach
    void setUp() {
        local = new Equipo();
        local.setId(10L);
        visitante = new Equipo();
        visitante.setId(20L);

        organizador = new Usuario();
        organizador.setId(99L);
        Torneo torneo = new Torneo();
        torneo.setEncargadoTorneo(organizador);

        partido = new Partido();
        partido.setId(5L);
        partido.setEstadoPartido(EstadoPartido.EN_PROCESO);
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partido.setTorneo(torneo);

        jugadorLocal = new Jugador();
        jugadorLocal.setId(100L);
        jugadorLocal.setEquipo(local);
        jugadorVisitante = new Jugador();
        jugadorVisitante.setId(200L);
        jugadorVisitante.setEquipo(visitante);
        asistenteLocal = new Jugador();
        asistenteLocal.setId(101L);
        asistenteLocal.setEquipo(local);

        lenient().when(partidoDao.findById(5L)).thenReturn(Optional.of(partido));
        lenient().when(participacionDao.findByTorneoAndJugador(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void solicitarSustitucion_guardaPendienteSinAplicarCambiosNiEventos() {
        Persona delegadoPersona = new Persona();
        delegadoPersona.setNumeroCelular("555");
        local.setDelegado(delegadoPersona);
        Usuario delegado = new Usuario();
        delegado.setId(50L);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO);
        delegado.setNumeroCelular("555");
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal, true);
        ConvocatoriaPartido suplente = convocatoria(2L, asistenteLocal, false);
        when(partidoDao.findByIdForUpdate(5L)).thenReturn(Optional.of(partido));
        when(usuarioDao.findById(50L)).thenReturn(Optional.of(delegado));
        when(ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), delegado)).thenReturn(Optional.empty());
        when(solicitudSustitucionDao.existsByPartido_IdAndEquipo_IdAndEstado(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(convocatoriaDao.findById(2L)).thenReturn(Optional.of(suplente));
        when(solicitudSustitucionDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.solicitarSustitucion(
                5L, new DtoCrearSolicitudSustitucion(1L, 2L, null, 12, 67), 50L);

        assertEquals(10L, saved.equipoId());
        assertEquals(1L, saved.titularId());
        assertEquals(2L, saved.suplenteId());
        assertEquals(12, saved.numeroUniforme());
        assertEquals(67, saved.minuto());
        assertEquals(com.example.torneos.enums.EstadoSolicitudSustitucion.PENDIENTE, saved.estado());
        assertTrue(titular.isTitular());
        assertTrue(!suplente.isTitular());
        verify(eventoDao, never()).save(any());
    }

    @Test
    void solicitarSustitucion_noPermiteDosPendientesDelMismoEquipo() {
        Persona delegadoPersona = new Persona();
        delegadoPersona.setNumeroCelular("555");
        local.setDelegado(delegadoPersona);
        Usuario delegado = new Usuario();
        delegado.setId(50L);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO);
        delegado.setNumeroCelular("555");
        when(partidoDao.findByIdForUpdate(5L)).thenReturn(Optional.of(partido));
        when(usuarioDao.findById(50L)).thenReturn(Optional.of(delegado));
        when(ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), delegado)).thenReturn(Optional.empty());
        when(solicitudSustitucionDao.existsByPartido_IdAndEquipo_IdAndEstado(anyLong(), anyLong(), any()))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.solicitarSustitucion(
                5L, new DtoCrearSolicitudSustitucion(1L, 2L, null, 12, 67), 50L));
        verify(solicitudSustitucionDao, never()).save(any());
    }

    @Test
    void listarSolicitudesSustitucion_delegadoSoloVeSolicitudesDeSuEquipo() {
        Persona delegadoPersona = new Persona();
        delegadoPersona.setNumeroCelular("555");
        local.setDelegado(delegadoPersona);
        Usuario delegado = new Usuario();
        delegado.setId(50L);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO);
        delegado.setNumeroCelular("555");

        SolicitudSustitucionPartido pendiente = solicitud(
                convocatoria(1L, jugadorLocal, true), convocatoria(2L, asistenteLocal, false));
        pendiente.setId(31L);
        pendiente.setSolicitante(delegado);
        pendiente.setEstado(EstadoSolicitudSustitucion.PENDIENTE);
        SolicitudSustitucionPartido resueltaDeMiEquipo = solicitud(
                convocatoria(3L, jugadorLocal, true), convocatoria(4L, asistenteLocal, false));
        resueltaDeMiEquipo.setId(32L);
        resueltaDeMiEquipo.setSolicitante(delegado);
        resueltaDeMiEquipo.setEstado(EstadoSolicitudSustitucion.RECHAZADA);
        when(usuarioDao.findById(50L)).thenReturn(Optional.of(delegado));
        when(ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), delegado)).thenReturn(Optional.empty());
        when(solicitudSustitucionDao.findByPartidoAndEquipoOrderByCreadaEnDesc(partido, local))
                .thenReturn(List.of(pendiente, resueltaDeMiEquipo));

        var results = service.listarSolicitudesSustitucion(5L, 50L);

        assertEquals(List.of(31L, 32L), results.stream().map(item -> item.id()).toList());
        assertTrue(results.stream().allMatch(item -> item.equipoId() == local.getId()));
        verify(solicitudSustitucionDao, never()).findByPartidoOrderByCreadaEnDesc(partido);
    }

    @Test
    void listarSolicitudesSustitucion_delegadoDeOtroEquipoEsRechazado() {
        Usuario delegado = new Usuario();
        delegado.setId(51L);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO);
        delegado.setNumeroCelular("sin-permiso");
        when(usuarioDao.findById(51L)).thenReturn(Optional.of(delegado));
        when(ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), delegado)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.listarSolicitudesSustitucion(5L, 51L));
        verify(solicitudSustitucionDao, never()).findByPartidoAndEquipoOrderByCreadaEnDesc(any(), any());
        verify(solicitudSustitucionDao, never()).findByPartidoOrderByCreadaEnDesc(any());
    }

    @Test
    void eventos_mapeaDatosDentroDeConsultaYSoportaJugadorAusente() {
        EventoPartido gol = new EventoPartido();
        gol.setId(40L);
        gol.setTipo(TipoEventoPartido.GOL);
        gol.setMinuto(12);
        gol.setJugador(jugadorLocal);
        gol.setEquipoAnotador(local);
        EventoPartido eventoSinJugador = new EventoPartido();
        eventoSinJugador.setId(41L);
        eventoSinJugador.setTipo(TipoEventoPartido.TARJETA_AMARILLA);
        eventoSinJugador.setMinuto(30);
        when(eventoDao.findByPartidoOrderByMinutoAscIdAsc(partido)).thenReturn(List.of(gol, eventoSinJugador));

        var eventos = service.eventos(5L);

        assertEquals(2, eventos.size());
        assertEquals(100L, eventos.get(0).jugadorId());
        assertEquals(10L, eventos.get(0).equipoId());
        assertEquals(12, eventos.get(0).minuto());
        assertNull(eventos.get(1).jugadorId());
        assertNull(eventos.get(1).equipoId());
    }

    @Test
    void aprobarSolicitudSustitucion_aplicaCambioYRegistraEventosSoloAlAprobar() {
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal, true);
        titular.setNumeroUniforme(10);
        ConvocatoriaPartido suplente = convocatoria(2L, asistenteLocal, false);
        suplente.setNumeroUniforme(12);
        SolicitudSustitucionPartido solicitud = solicitud(titular, suplente);
        when(solicitudSustitucionDao.findById(30L)).thenReturn(Optional.of(solicitud));
        when(solicitudSustitucionDao.findByIdForUpdate(30L)).thenReturn(Optional.of(solicitud));
        when(partidoDao.findByIdForUpdate(5L)).thenReturn(Optional.of(partido));
        usuarioOrganizador();
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(convocatoriaDao.findById(2L)).thenReturn(Optional.of(suplente));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular, suplente));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resolved = service.aprobarSolicitudSustitucion(30L, organizador.getId());

        assertEquals(EstadoSolicitudSustitucion.APROBADA, resolved.estado());
        assertTrue(!titular.isTitular());
        assertTrue(suplente.isTitular());
        ArgumentCaptor<EventoPartido> eventos = ArgumentCaptor.forClass(EventoPartido.class);
        verify(eventoDao, times(2)).save(eventos.capture());
        assertEquals(List.of(67, 67), eventos.getAllValues().stream().map(EventoPartido::getMinuto).toList());
    }

    @Test
    void rechazarSolicitudSustitucion_noCambiaLaAlineacionNiRegistraEventos() {
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal, true);
        ConvocatoriaPartido suplente = convocatoria(2L, asistenteLocal, false);
        SolicitudSustitucionPartido solicitud = solicitud(titular, suplente);
        when(solicitudSustitucionDao.findById(30L)).thenReturn(Optional.of(solicitud));
        when(solicitudSustitucionDao.findByIdForUpdate(30L)).thenReturn(Optional.of(solicitud));
        when(partidoDao.findByIdForUpdate(5L)).thenReturn(Optional.of(partido));
        usuarioOrganizador();

        var resolved = service.rechazarSolicitudSustitucion(30L, organizador.getId());

        assertEquals(EstadoSolicitudSustitucion.RECHAZADA, resolved.estado());
        assertTrue(titular.isTitular());
        assertTrue(!suplente.isTitular());
        verify(eventoDao, never()).save(any(EventoPartido.class));
    }

    private ConvocatoriaPartido convocatoria(long id, Jugador jugador, boolean titular) {
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setId(id);
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        convocatoria.setTitular(titular);
        return convocatoria;
    }

    private SolicitudSustitucionPartido solicitud(
            ConvocatoriaPartido titular, ConvocatoriaPartido suplente) {
        SolicitudSustitucionPartido solicitud = new SolicitudSustitucionPartido();
        solicitud.setId(30L);
        solicitud.setPartido(partido);
        solicitud.setEquipo(local);
        solicitud.setSolicitante(organizador);
        solicitud.setTitular(titular);
        solicitud.setSuplente(suplente);
        solicitud.setJugadorEntrante(suplente.getJugador());
        solicitud.setNumeroUniforme(12);
        solicitud.setMinuto(67);
        solicitud.setEstado(EstadoSolicitudSustitucion.PENDIENTE);
        return solicitud;
    }

    @Test
    void registrarGol_guardaAutorYAsistenciaYActualizaMarcador() {
        usuarioOrganizador();
        convocar(jugadorLocal);
        convocar(asistenteLocal);
        when(jugadorDao.findById(100L)).thenReturn(Optional.of(jugadorLocal));
        when(jugadorDao.findById(101L)).thenReturn(Optional.of(asistenteLocal));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoDao.save(partido)).thenReturn(partido);
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(100L);
        solicitud.setAsistenciaId(101L);
        solicitud.setPenalti(true);
        solicitud.setMinuto(24);

        service.registrarGol(5L, solicitud, 99L);

        assertEquals(1, partido.getAnotacionesEquipoLocal());
        assertEquals(0, partido.getAnotacionesEquipoVisitante());
        ArgumentCaptor<EventoPartido> eventos = ArgumentCaptor.forClass(EventoPartido.class);
        verify(eventoDao, times(2)).save(eventos.capture());
        assertEquals(List.of(TipoEventoPartido.GOL, TipoEventoPartido.ASISTENCIA),
                eventos.getAllValues().stream().map(EventoPartido::getTipo).toList());
        assertEquals(asistenteLocal, eventos.getAllValues().get(0).getAsistidor());
        assertTrue(eventos.getAllValues().get(0).isPenalti());
        assertEquals(local, eventos.getAllValues().get(0).getEquipoAnotador());
        verify(partidoDao).save(partido);
    }

    @Test
    void registrarGol_autogolSumaAlEquipoObjetivoYRegistraAlJugadorRival() {
        usuarioOrganizador();
        convocar(jugadorVisitante);
        when(jugadorDao.findById(200L)).thenReturn(Optional.of(jugadorVisitante));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoDao.save(partido)).thenReturn(partido);
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(200L);
        solicitud.setAutogol(true);
        solicitud.setMinuto(37);

        service.registrarGol(5L, solicitud, 99L);

        assertEquals(1, partido.getAnotacionesEquipoLocal());
        ArgumentCaptor<EventoPartido> evento = ArgumentCaptor.forClass(EventoPartido.class);
        verify(eventoDao).save(evento.capture());
        assertEquals(TipoEventoPartido.AUTOGOL, evento.getValue().getTipo());
        assertEquals(jugadorVisitante, evento.getValue().getJugador());
        assertEquals(local, evento.getValue().getEquipoAnotador());
    }

    @Test
    void registrarGol_rechazaAutorDeGolDelEquipoContrarioSinCambiarMarcador() {
        usuarioOrganizador();
        convocar(jugadorVisitante);
        when(jugadorDao.findById(200L)).thenReturn(Optional.of(jugadorVisitante));
        DtoRegistrarGol solicitud = new DtoRegistrarGol();
        solicitud.setEquipoAnotadorId(10L);
        solicitud.setJugadorId(200L);

        assertThrows(IllegalArgumentException.class, () -> service.registrarGol(5L, solicitud, 99L));

        assertEquals(0, partido.getAnotacionesEquipoLocal());
        verify(eventoDao, org.mockito.Mockito.never()).save(any(EventoPartido.class));
    }

    @Test
    void registrarEvento_guardaIndicadorDeRojaPorDobleAmarilla() {
        usuarioOrganizador();
        convocar(jugadorLocal);
        when(jugadorDao.findById(100L)).thenReturn(Optional.of(jugadorLocal));
        when(eventoDao.save(any(EventoPartido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoPartido evento = service.registrarEvento(
                5L, 100L, TipoEventoPartido.TARJETA_ROJA, 70, true, 99L);

        assertTrue(evento.isDobleAmarilla());
        assertEquals(TipoEventoPartido.TARJETA_ROJA, evento.getTipo());
    }

    @Test
    void agregarTitular_aceptaJugadorAsociadoAlEquipoPrincipal() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(jugadorDao.findById(100L)).thenReturn(Optional.of(jugadorLocal));
        when(convocatoriaDao.save(any(ConvocatoriaPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConvocatoriaPartido convocatoria = service.agregar(5L, 100L, true, 10, 99L);

        assertEquals(jugadorLocal, convocatoria.getJugador());
        assertEquals(local, convocatoria.getJugador().getEquipo());
        assertTrue(convocatoria.isTitular());
    }

    @Test
    void agregarTitular_requiereNumeroUniforme() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(jugadorDao.findById(100L)).thenReturn(Optional.of(jugadorLocal));

        assertThrows(IllegalArgumentException.class,
                () -> service.agregar(5L, 100L, true, null, 99L));

        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void agregar_rechazaAltasGenericasCuandoElPartidoYaEstaEnProceso() {
        usuarioOrganizador();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregar(5L, jugadorLocal.getId(), false, null, 99L));

        verify(jugadorDao, never()).findById(jugadorLocal.getId());
        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void sustituirJugadorInscrito_agregaAlPartidoJugadorInscritoQueAunNoTeniaConvocatoria() {
        usuarioOrganizador();
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        Jugador suplenteInscrito = new Jugador();
        suplenteInscrito.setId(102L);
        suplenteInscrito.setEquipo(local);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(jugadorDao.findById(102L)).thenReturn(Optional.of(suplenteInscrito));
        when(convocatoriaDao.findByPartidoAndJugador(partido, suplenteInscrito)).thenReturn(Optional.empty());
        when(convocatoriaDao.countByPartidoAndJugadorEquipo(partido, local)).thenReturn(5L);
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), suplenteInscrito))
                .thenReturn(Optional.of(participacion(suplenteInscrito, local)));
        when(convocatoriaDao.save(any(ConvocatoriaPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoDao.save(any(EventoPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sustituirJugadorInscrito(5L, 1L, 102L, 10, 99L);

        ArgumentCaptor<ConvocatoriaPartido> convocatorias = ArgumentCaptor.forClass(ConvocatoriaPartido.class);
        verify(convocatoriaDao, times(3)).save(convocatorias.capture());
        ConvocatoriaPartido suplenteAgregado = convocatorias.getAllValues().get(0);
        assertEquals(suplenteInscrito, suplenteAgregado.getJugador());
        assertEquals(10, suplenteAgregado.getNumeroUniforme());
        assertTrue(suplenteAgregado.isTitular());
        assertTrue(suplenteAgregado.isFueTitular());
        assertTrue(!titular.isTitular());
    }

    @Test
    void sustituirJugadorInscrito_rechazaNumeroUniformeYaUsadoPorElEquipo() {
        usuarioOrganizador();
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        Jugador suplenteInscrito = new Jugador();
        suplenteInscrito.setId(102L);
        suplenteInscrito.setEquipo(local);
        ConvocatoriaPartido otroJugadorConNumero = convocatoria(2L, jugadorLocal);
        otroJugadorConNumero.setNumeroUniforme(10);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(jugadorDao.findById(102L)).thenReturn(Optional.of(suplenteInscrito));
        when(convocatoriaDao.findByPartidoAndJugador(partido, suplenteInscrito)).thenReturn(Optional.empty());
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), suplenteInscrito))
                .thenReturn(Optional.of(participacion(suplenteInscrito, local)));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(otroJugadorConNumero));

        assertThrows(IllegalArgumentException.class,
                () -> service.sustituirJugadorInscrito(5L, 1L, 102L, 10, 99L));

        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void agregarEnPosicionVacia_rechazaAgregarTitularDuranteElPartido() {
        usuarioOrganizador();
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarEnPosicionVacia(5L, 102L, 3, 12, 99L));
        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void agregarEnPosicionVacia_rechazaUnaPosicionOcupada() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        titular.setPosicionAlineacion(2);
        Jugador jugadorTardio = new Jugador();
        jugadorTardio.setId(102L);
        jugadorTardio.setEquipo(local);
        when(jugadorDao.findById(102L)).thenReturn(Optional.of(jugadorTardio));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorTardio))
                .thenReturn(Optional.of(participacion(jugadorTardio, local)));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular));
        when(alineacionDao.findByPartidoAndEquipo(partido, local)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarEnPosicionVacia(5L, 102L, 2, 12, 99L));

        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void agregarEnPosicionVacia_rechazaNumeroDeUniformeRepetidoEnElEquipo() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        titular.setPosicionAlineacion(0);
        titular.setNumeroUniforme(12);
        Jugador jugadorTardio = new Jugador();
        jugadorTardio.setId(102L);
        jugadorTardio.setEquipo(local);
        when(jugadorDao.findById(102L)).thenReturn(Optional.of(jugadorTardio));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorTardio))
                .thenReturn(Optional.of(participacion(jugadorTardio, local)));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular));
        when(alineacionDao.findByPartidoAndEquipo(partido, local)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarEnPosicionVacia(5L, 102L, 3, 12, 99L));

        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void listar_usaElEquipoDeLaInscripcionDelTorneoParaClasificarLaConvocatoria() {
        jugadorLocal.setEquipo(visitante);
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugadorLocal);
        convocatoria.setTitular(true);
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(convocatoria));
        when(participacionDao.findByTorneo(partido.getTorneo()))
                .thenReturn(List.of(participacion(jugadorLocal, local)));

        List<com.example.torneos.DTO.DtoConvocatoriaPartido> convocatorias = service.listar(5L);

        assertEquals(local.getId(), convocatorias.get(0).equipo().id());
        assertEquals(jugadorLocal.getId(), convocatorias.get(0).jugador().id());
        assertEquals(jugadorLocal.getNombre(), convocatorias.get(0).jugador().nombre());
        assertEquals(local.getId(), convocatorias.get(0).equipo().id());
    }

    @Test
    void jugadoresDelEquipo_soloDevuelveJugadoresActivosInscritosEnElEquipoYTorneo() {
        Jugador inactivo = new Jugador();
        inactivo.setId(102L);
        inactivo.setEstadoJugador(EstadoJugador.INACTIVO);
        Jugador delEquipoRival = new Jugador();
        delEquipoRival.setId(201L);
        delEquipoRival.setEstadoJugador(EstadoJugador.ACTIVO);
        jugadorLocal.setEstadoJugador(EstadoJugador.ACTIVO);
        when(participacionDao.findByTorneo(partido.getTorneo())).thenReturn(List.of(
                participacion(jugadorLocal, local),
                participacion(inactivo, local),
                participacion(delEquipoRival, visitante)));

        List<DtoJugadorDisponiblePartido> disponibles = service.jugadoresDelEquipo(5L, 10L);

        assertEquals(1, disponibles.size());
        assertEquals(jugadorLocal.getId(), disponibles.get(0).id());
        assertEquals(jugadorLocal.getNombre(), disponibles.get(0).nombre());
    }

    @Test
    void cambiarFormacion_persisteFormacionYAsignaPosicionesEnOrden() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        ConvocatoriaPartido primera = convocatoria(1L, jugadorLocal);
        ConvocatoriaPartido segunda = convocatoria(2L, asistenteLocal);
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), asistenteLocal))
                .thenReturn(Optional.of(participacion(asistenteLocal, local)));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(primera, segunda));
        when(convocatoriaDao.save(any(ConvocatoriaPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(alineacionDao.findByPartidoAndEquipo(partido, local)).thenReturn(Optional.empty());
        when(alineacionDao.save(any(AlineacionEquipoPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var alineacion = service.cambiarFormacion(5L, 10L, "4-3-3", 99L);

        assertEquals("4-3-3", alineacion.formacion());
        assertEquals(0, primera.getPosicionAlineacion());
        assertEquals(1, segunda.getPosicionAlineacion());
        verify(alineacionDao).save(any(AlineacionEquipoPartido.class));
    }

    @Test
    void cambiarFormacion_permiteActualizarlaDuranteElPartido() {
        usuarioDelegadoLocal();
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        titular.setPosicionAlineacion(0);
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular));
        when(convocatoriaDao.save(any(ConvocatoriaPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(alineacionDao.findByPartidoAndEquipo(partido, local)).thenReturn(Optional.empty());
        when(alineacionDao.save(any(AlineacionEquipoPartido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var alineacion = service.cambiarFormacion(5L, 10L, "4-3-3", 50L);

        assertEquals("4-3-3", alineacion.formacion());
        verify(alineacionDao).save(any(AlineacionEquipoPartido.class));
    }

    @Test
    void cambiarFormacion_rechazaCambiosDespuesDeTerminarElPartido() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.TERMINADO);

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarFormacion(5L, 10L, "4-3-3", 99L));

        verify(alineacionDao, never()).save(any(AlineacionEquipoPartido.class));
    }

    @Test
    void cambiarNumeroUniforme_rechazaNumeroYaUsadoPorOtroJugadorDelMismoEquipo() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        ConvocatoriaPartido convocatoria = convocatoria(1L, jugadorLocal);
        ConvocatoriaPartido existente = convocatoria(2L, asistenteLocal);
        existente.setNumeroUniforme(7);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(convocatoria));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(convocatoria, existente));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), asistenteLocal))
                .thenReturn(Optional.of(participacion(asistenteLocal, local)));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarNumeroUniforme(1L, 7, 99L));

        verify(convocatoriaDao, never()).save(any(ConvocatoriaPartido.class));
    }

    @Test
    void moverPosicionAlineacion_mueveTitularAUnaPosicionVacia() {
        usuarioOrganizador();
        partido.setEstadoPartido(EstadoPartido.PROGRAMADO);
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        titular.setPosicionAlineacion(0);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(convocatoriaDao.save(titular)).thenReturn(titular);

        service.moverPosicionAlineacion(5L, 1L, 2, 99L);

        assertEquals(2, titular.getPosicionAlineacion());
        verify(convocatoriaDao).save(titular);
    }

    @Test
    void moverPosicionAlineacion_permiteMoverTitularDuranteElPartido() {
        usuarioDelegadoLocal();
        ConvocatoriaPartido titular = convocatoria(1L, jugadorLocal);
        titular.setPosicionAlineacion(0);
        when(convocatoriaDao.findById(1L)).thenReturn(Optional.of(titular));
        when(convocatoriaDao.findByPartido(partido)).thenReturn(List.of(titular));
        when(participacionDao.findByTorneoAndJugador(partido.getTorneo(), jugadorLocal))
                .thenReturn(Optional.of(participacion(jugadorLocal, local)));
        when(alineacionDao.findByPartidoAndEquipo(partido, local)).thenReturn(Optional.empty());
        when(convocatoriaDao.save(titular)).thenReturn(titular);

        service.moverPosicionAlineacion(5L, 1L, 2, 50L);

        assertEquals(2, titular.getPosicionAlineacion());
        verify(convocatoriaDao).save(titular);
    }

    private ParticipacionJugadorTorneo participacion(Jugador jugador, Equipo equipo) {
        ParticipacionJugadorTorneo participacion = new ParticipacionJugadorTorneo();
        participacion.setTorneo(partido.getTorneo());
        participacion.setJugador(jugador);
        participacion.setEquipo(equipo);
        participacion.setParticipando(true);
        return participacion;
    }

    private ConvocatoriaPartido convocatoria(long id, Jugador jugador) {
        ConvocatoriaPartido convocatoria = new ConvocatoriaPartido();
        convocatoria.setId(id);
        convocatoria.setPartido(partido);
        convocatoria.setJugador(jugador);
        convocatoria.setTitular(true);
        return convocatoria;
    }

    private void usuarioOrganizador() {
        when(usuarioDao.findById(99L)).thenReturn(Optional.of(organizador));
    }

    private void usuarioDelegadoLocal() {
        Persona persona = new Persona();
        persona.setNumeroCelular("555");
        local.setDelegado(persona);
        Usuario delegado = new Usuario();
        delegado.setId(50L);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO);
        delegado.setNumeroCelular("555");
        when(usuarioDao.findById(50L)).thenReturn(Optional.of(delegado));
        when(ayudanteTorneoDao.findByTorneoAndUsuario(partido.getTorneo(), delegado)).thenReturn(Optional.empty());
    }

    private void convocar(Jugador jugador) {
        when(convocatoriaDao.findByPartidoAndJugador(partido, jugador))
                .thenReturn(Optional.of(new ConvocatoriaPartido()));
    }
}
