package com.example.torneos.rest;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.DTO.DtoRegistroPlanilla;
import com.example.torneos.DTO.DtoResultadoRegistroPlanilla;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.SolicitudJugadorEquipo;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.ParticipacionEquipoTorneo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.services.EquipoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/equipo")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class EquipoRest {
    @Autowired
    private EquipoService equipoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo save(@RequestPart("objeto") String equipoS,
                       @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                       @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB,
                       @AuthenticationPrincipal Long usuarioId) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.save(equipo, usuarioId);
    }

    @PostMapping(value = "/solicitud", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo saveSolicitud(@RequestPart("objeto") String equipoS,
                               @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                               @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB,
                               @AuthenticationPrincipal Long usuarioId) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null) {
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.saveSolicitud(equipo, usuarioId);
    }

    @PostMapping("/torneo/{idTorneo}/registro-planilla")
    @ResponseStatus(HttpStatus.CREATED)
    public DtoResultadoRegistroPlanilla registrarPlanilla(@PathVariable long idTorneo,
                                                          @RequestBody DtoRegistroPlanilla planilla,
                                                          @AuthenticationPrincipal Long usuarioId) {
        return equipoService.registrarPlanilla(idTorneo, planilla, usuarioId);
    }

    @GetMapping
    public List<Equipo> getAll(){
        return equipoService.getAll();
    }

    @GetMapping("/{id}")
    public Equipo getById(@PathVariable long id){
        return equipoService.getById(id);
    }
    @GetMapping("/{idEquipo}/torneo/{idTorneo}")
    public Equipo getByIdAndTorneo(@PathVariable long idEquipo, @PathVariable long idTorneo) {
        return equipoService.getByIdAndTorneo(idEquipo, idTorneo);
    }
    @GetMapping("/torneo/{id}")
    public List<Equipo> getByTorneo(@PathVariable long id){
        return equipoService.getByTorneo(id);
    }
    @PostMapping("/torneo/{idTorneo}/jugador/{idEquipo}")
    public Jugador registrarJugador(@PathVariable long idTorneo, @PathVariable long idEquipo, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.registrarJugadorEnEquipo(idTorneo, idEquipo, usuarioId);
    }
    @PostMapping("/{idEquipo}/torneo/{idTorneo}/jugadores/{idJugador}")
    public ParticipacionJugadorTorneo agregarJugadorATorneo(@PathVariable long idEquipo, @PathVariable long idTorneo,
                                                              @PathVariable long idJugador, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.agregarJugadorATorneo(idEquipo, idTorneo, idJugador, usuarioId);
    }
    @DeleteMapping("/{idEquipo}/torneo/{idTorneo}/jugadores/{idJugador}")
    public void eliminarJugadorDeTorneo(@PathVariable long idEquipo, @PathVariable long idTorneo,
                                       @PathVariable long idJugador, @AuthenticationPrincipal Long usuarioId) {
        equipoService.eliminarJugadorDeTorneo(idEquipo, idTorneo, idJugador, usuarioId);
    }
    @PostMapping("/{idEquipo}/solicitud-jugador")
    public SolicitudJugadorEquipo solicitarJugador(@PathVariable long idEquipo, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.solicitarJugador(idEquipo, usuarioId);
    }
    @GetMapping("/{idEquipo}/solicitudes-jugador")
    public List<SolicitudJugadorEquipo> solicitudesJugador(@PathVariable long idEquipo, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.solicitudesJugador(idEquipo, usuarioId);
    }
    @PutMapping("/solicitudes-jugador/{id}/aceptar")
    public Equipo aceptarSolicitudJugador(@PathVariable long id, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.aceptarSolicitudJugador(id, usuarioId);
    }
    @DeleteMapping("/solicitudes-jugador/{id}/rechazar")
    public void rechazarSolicitudJugador(@PathVariable long id, @AuthenticationPrincipal Long usuarioId) {
        equipoService.rechazarSolicitudJugador(id, usuarioId);
    }
    @PostMapping("/{idEquipo}/invitaciones-jugador")
    public com.example.torneos.DTO.DtoInvitacionEquipo invitarJugador(@PathVariable long idEquipo, @RequestParam String identificacion, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.invitarJugador(idEquipo, identificacion, usuarioId);
    }
    @GetMapping("/invitaciones-jugador")
    public List<com.example.torneos.DTO.DtoInvitacionEquipo> invitacionesJugador(@AuthenticationPrincipal Long usuarioId) {
        return equipoService.invitacionesJugador(usuarioId);
    }
    @PutMapping("/invitaciones-jugador/{id}/aceptar")
    public com.example.torneos.DTO.DtoInvitacionEquipo aceptarInvitacionJugador(@PathVariable long id, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.resolverInvitacionJugador(id, usuarioId, true);
    }
    @PutMapping("/invitaciones-jugador/{id}/rechazar")
    public com.example.torneos.DTO.DtoInvitacionEquipo rechazarInvitacionJugador(@PathVariable long id, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.resolverInvitacionJugador(id, usuarioId, false);
    }
    @GetMapping("/{idEquipo}/jugadores/buscar")
    public Jugador buscarJugador(@PathVariable long idEquipo, @RequestParam String identificacion, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.buscarJugadorPorIdentificacion(idEquipo, identificacion, usuarioId);
    }
    @DeleteMapping("/{idEquipo}/jugadores/{idJugador}")
    public void eliminarJugador(@PathVariable long idEquipo, @PathVariable long idJugador, @AuthenticationPrincipal Long usuarioId) {
        equipoService.eliminarJugador(idEquipo, idJugador, usuarioId);
    }
    @PutMapping("/torneo/{idTorneo}/jugador/{idJugador}/cambiar-equipo/{idEquipoNuevo}")
    public ParticipacionJugadorTorneo cambiarEquipo(@PathVariable long idTorneo, @PathVariable long idJugador, @PathVariable long idEquipoNuevo, @AuthenticationPrincipal Long usuarioId) {
        return equipoService.cambiarEquipoJugador(idTorneo, idJugador, idEquipoNuevo, usuarioId);
    }
    @GetMapping("/torneo/{idTorneo}/participaciones")
    public List<ParticipacionJugadorTorneo> participaciones(@PathVariable long idTorneo) {
        return equipoService.participacionesTorneo(idTorneo);
    }
    @GetMapping("/{idEquipo}/participaciones")
    public List<ParticipacionJugadorTorneo> participacionesEquipo(@PathVariable long idEquipo) {
        return equipoService.getParticipacionesEquipo(idEquipo);
    }
    @GetMapping("/{idEquipo}/torneos")
    public List<ParticipacionEquipoTorneo> torneosEquipo(@PathVariable long idEquipo) {
        return equipoService.participacionesTorneosEquipo(idEquipo);
    }
    @GetMapping("/solicitudes/torneo/{id}")
    public List<Equipo> getSolicitudesByTorneo(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return equipoService.getSolicitudesByTorneo(id, usuarioId);
    }
    @GetMapping("/solicitudes/torneo/{id}/todas")
    public List<Equipo> getSolicitudesYAceptadasByTorneo(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return equipoService.getSolicitudesYAceptadasByTorneo(id, usuarioId);
    }
    @GetMapping("/delegado/{idUsuario}")
    public List<Equipo> getByDelegadoUsuario(@PathVariable long idUsuario, @AuthenticationPrincipal Long usuarioId){
        if (usuarioId == null || usuarioId != idUsuario) throw new IllegalArgumentException("Solo puedes consultar tus propios equipos");
        return equipoService.getByDelegadoUsuario(usuarioId);
    }
    @GetMapping("/torneo/modalidad/{id}/{modalidadTorneo}")
    public List<Equipo> getByTorneoModalidad(@PathVariable long id, @PathVariable ModalidadTorneo modalidadTorneo){
        return equipoService.getByTorneoModalidad(id, modalidadTorneo);
    }
    @GetMapping("/torneo/fase/{id}/{faseTorneo}")
    public List<Equipo> getByTorneoFase(@PathVariable long id, @PathVariable FaseActual faseTorneo){
        return equipoService.getByTorneoFase(id, faseTorneo);
    }
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Equipo update(@RequestPart("objeto") String equipoS,
                         @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                         @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB,
                         @AuthenticationPrincipal Long usuarioId)  throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.update(equipo, usuarioId);
    }

    @PutMapping("/solicitud/{id}/aceptar")
    @ResponseStatus(HttpStatus.OK)
    public Equipo aceptarSolicitud(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return equipoService.aceptarSolicitud(id, usuarioId);
    }

    @DeleteMapping("/solicitud/{id}/rechazar")
    public void rechazarSolicitud(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        equipoService.rechazarSolicitud(id, usuarioId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        equipoService.delete(id, usuarioId);
    }
}
