package com.example.torneos.rest;

import com.example.torneos.DTO.DtoLoginInfo;
import com.example.torneos.DTO.DtoUsuarioInfo;
import com.example.torneos.DTO.DtoEquipoPerfilJugador;
import com.example.torneos.DTO.DtoTorneoPerfilJugador;
import com.example.torneos.entities.Usuario;
import com.example.torneos.services.UsuarioService;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.TorneoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.NotificacionUsuario;
import com.example.torneos.dao.NotificacionUsuarioDao;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/usuario")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class UsuarioRest {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private EquipoDao equipoDao;
    @Autowired private TorneoDao torneoDao;
    @Autowired private JugadorDao jugadorDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired private NotificacionUsuarioDao notificacionDao;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario save(@RequestBody @RequestParam("objeto") String usuarioS,
                        @Nullable @RequestBody @RequestParam("foto") MultipartFile fileF,
                        @Nullable @RequestBody @RequestParam("identificacion") MultipartFile fileI) throws JsonMappingException, JsonProcessingException, IOException {
        Usuario usuario = objectMapper.readValue(usuarioS, Usuario.class);
        if (fileF != null) {
            usuario.setFoto(fileF.getBytes());
        }
        /*if (fileI != null){
            usuario.setIdentificacion(fileI.getBytes());
        }*/
        return usuarioService.save(usuario);
    }

    @GetMapping
    public List<Usuario> getAll(){
        return usuarioService.getAll();
    }

    @PostMapping("/login")
    public DtoUsuarioInfo iniciarSesion(@RequestBody DtoLoginInfo dtoLoginInfo){
        return usuarioService.iniciarSesion(dtoLoginInfo);
    }

    @GetMapping("/{id}")
    public Usuario getById(@PathVariable long id){
        return usuarioService.getById(id);
    }

    @GetMapping("/{id}/perfil")
    @Transactional(readOnly = true)
    public Map<String, Object> perfil(@PathVariable long id) {
        Usuario usuario = usuarioService.getById(id);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("usuario", usuario);
        respuesta.put("torneosCreados", torneoDao.findByEncargadoTorneo(usuario));
        Persona delegado = usuario.getNumeroCelular() == null ? null : usuarioService.buscarPersonaPorCelular(usuario.getNumeroCelular());
        List<Equipo> equiposDelegado = delegado == null ? List.of() : equipoDao.findByDelegado(delegado);
        respuesta.put("equiposDelegado", equiposDelegado);
        if (usuario.getIdentificacion() != null) {
            jugadorDao.findByIdentificacion(usuario.getIdentificacion()).ifPresent(jugador -> {
                respuesta.put("jugador", jugador);
                List<com.example.torneos.entities.ParticipacionJugadorTorneo> participaciones = participacionDao.findByJugador(jugador);
                Map<Long, com.example.torneos.entities.Torneo> torneoPorEquipo = new HashMap<>();
                participaciones.forEach(item -> torneoPorEquipo.put(item.getEquipo().getId(), item.getTorneo()));
                Map<Long, Equipo> equiposPorId = new java.util.LinkedHashMap<>();
                jugador.getEquipos().forEach(equipo -> equiposPorId.put(equipo.getId(), equipo));
                if (jugador.getEquipo() != null) {
                    equiposPorId.putIfAbsent(jugador.getEquipo().getId(), jugador.getEquipo());
                }
                participaciones.forEach(item -> equiposPorId.putIfAbsent(item.getEquipo().getId(), item.getEquipo()));
                List<DtoEquipoPerfilJugador> equiposJugador = equiposPorId.values().stream().map(equipo -> {
                    com.example.torneos.entities.Torneo torneo = torneoPorEquipo.get(equipo.getId());
                    if (torneo == null) torneo = equipo.getTorneo();
                    DtoTorneoPerfilJugador torneoDto = torneo == null ? null :
                            new DtoTorneoPerfilJugador(torneo.getId(), torneo.getNombre(), torneo.getUbicacion());
                    return new DtoEquipoPerfilJugador(equipo.getId(), equipo.getNombre(), equipo.getEscudo(), torneoDto);
                }).toList();
                respuesta.put("equiposJugador", equiposJugador);
                respuesta.put("participaciones", participaciones);
            });
        }
        return respuesta;
    }

    @GetMapping("/{id}/notificaciones/pendientes")
    public List<NotificacionUsuario> notificacionesPendientes(@PathVariable long id) {
        Usuario usuario = usuarioService.getById(id);
        return notificacionDao.findByUsuarioAndLeidaFalseOrderByCreadaEnDesc(usuario);
    }

    @GetMapping("/{id}/notificaciones")
    public List<NotificacionUsuario> notificaciones(@PathVariable long id) {
        Usuario usuario = usuarioService.getById(id);
        return notificacionDao.findByUsuarioOrderByCreadaEnDesc(usuario);
    }

    @GetMapping("/{id}/notificaciones/no-leidas/count")
    public long contarNotificacionesPendientes(@PathVariable long id) {
        Usuario usuario = usuarioService.getById(id);
        return notificacionDao.countByUsuarioAndLeidaFalse(usuario);
    }

    @PutMapping("/{id}/notificaciones/{idNotificacion}/leida")
    public NotificacionUsuario marcarNotificacionLeida(@PathVariable long id, @PathVariable long idNotificacion) {
        Usuario usuario = usuarioService.getById(id);
        NotificacionUsuario notificacion = notificacionDao.findByIdAndUsuario(idNotificacion, usuario)
                .orElseThrow(() -> new IllegalArgumentException("La notificación no existe"));
        notificacion.setLeida(true);
        return notificacionDao.save(notificacion);
    }

    @DeleteMapping("/{id}/notificaciones/{idNotificacion}")
    public void eliminarNotificacion(@PathVariable long id, @PathVariable long idNotificacion) {
        Usuario usuario = usuarioService.getById(id);
        notificacionDao.deleteByIdAndUsuario(idNotificacion, usuario);
    }

    @PutMapping()
    public Usuario update(@RequestBody Usuario usuario, @AuthenticationPrincipal Long usuarioId){
        return usuarioService.update(usuario, usuarioId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        usuarioService.delete(id);
    }
}
