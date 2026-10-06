package com.example.torneos.rest;

import com.example.torneos.DTO.DtoResulatoLlave;
import com.example.torneos.DTO.DtoPartidoResumen;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Partido;
import com.example.torneos.enums.EstadoPartido;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.services.PartidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partido")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class PartidoRest {
    @Autowired
    private PartidoService partidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Partido save(@RequestBody Partido partido, @AuthenticationPrincipal Long usuarioId){
        return partidoService.save(partido, usuarioId);
    }
    @GetMapping("torneo/{idTorneo}")
    public List<List<Partido>> getTorneo(@PathVariable long idTorneo){
        return partidoService.getTorneo(idTorneo);
    }
    @GetMapping("torneo/fase-actual/{idTorneo}/{faseActual}")
    public List<List<Partido>> getTorneoFaseActual(@PathVariable long idTorneo, @PathVariable FaseActual faseActual){
        return partidoService.getTorneoFaseActual(idTorneo, faseActual);
    }
    @GetMapping("torneo/fase/{idTorneo}/{faseActual}")
    public List<List<DtoPartidoResumen>> getTorneoFaseFiltrada(
            @PathVariable long idTorneo,
            @PathVariable FaseActual faseActual,
            @RequestParam(defaultValue = "PENDIENTE") String estado,
            @RequestParam(defaultValue = "false") boolean sinProgramar,
            @RequestParam(defaultValue = "") String equipo) {
        return partidoService.getTorneoFaseFiltrada(idTorneo, faseActual, estado, sinProgramar, equipo);
    }
    @GetMapping("estado/{estado}")
    public List<Partido> getPartidosEstado(@PathVariable EstadoPartido estado){
        return partidoService.getPartidosEstado(estado);
    }
    @GetMapping("eliminatorias/{idTorneo}")
    public List<Partido> getPartidosEliminatorias(@PathVariable long idTorneo){
        return partidoService.getPartidosEliminatorias(idTorneo);
    }
    @GetMapping("/torneo/modalidad/{id}/{modalidadTorneo}")
    public List<Partido> getByTorneoModalidad(@PathVariable long id, @PathVariable ModalidadTorneo modalidadTorneo){
        return partidoService.getByTorneoModalidad(id, modalidadTorneo);
    }
    @GetMapping("programado_proceso/{fecha}/{idTorneo}")
    public List<DtoPartidoResumen> getPartidosFechaTorneo(@PathVariable long fecha, @PathVariable long idTorneo){
        return partidoService.getPartidosFechaTorneo(fecha, idTorneo);
    }
    @GetMapping("generar_partidos/{idTorneo}")
    public List<Partido> generarPartidos(@PathVariable long idTorneo, @AuthenticationPrincipal Long usuarioId){
        return partidoService.generarPartidos(idTorneo, usuarioId);
    }
    @PutMapping("asignar_fecha")
    public Partido asignarFecha(@RequestBody Partido partido, @AuthenticationPrincipal Long usuarioId) {
        return partidoService.asignarFecha(partido, usuarioId);
    }
    @GetMapping("/{id}")
    public Partido getById(@PathVariable long id){
        return partidoService.getById(id);
    }
    @GetMapping("/{id}/puede-gestionar")
    public boolean puedeGestionar(@PathVariable long id, @AuthenticationPrincipal Long usuarioId) {
        return partidoService.puedeGestionarPartido(id, usuarioId);
    }
    @GetMapping("/eliminatorias_torneo/{idTorneo}")
    public List<DtoResulatoLlave> getEliminatoriasTorneo(@PathVariable long idTorneo){
        return partidoService.getEliminatoriasTorneo(idTorneo);
    }
    @PutMapping()
    public Partido update(@RequestBody Partido partido, @AuthenticationPrincipal Long usuarioId){
        return partidoService.update(partido, usuarioId);
    }
    @PutMapping("/sumar_gol")
    public Partido sumarGol(@RequestBody Partido partido, @AuthenticationPrincipal Long usuarioId){
        return partidoService.sumarGol(partido, usuarioId);
    }
    @PutMapping("iniciar/{id}")
    public Partido iniciarPartido(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return partidoService.iniciarPartido(id, usuarioId);
    }
    @PutMapping("terminar/{id}")
    public Partido terminarPartido(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return partidoService.terminarPartido(id, usuarioId);
    }
    @PutMapping("terminar-penaltis/{id}/{ganador}")
    public Partido terminarPartidoPenaltis(@PathVariable long id, @PathVariable String ganador, @AuthenticationPrincipal Long usuarioId){
        return partidoService.terminarPartidoPenaltis(id, ganador, usuarioId);
    }
    @PutMapping("cancelar/{id}")
    public Partido cancelarPartido(@PathVariable long id, @AuthenticationPrincipal Long usuarioId){
        return partidoService.cancelarPartido(id, usuarioId);
    }
    @PutMapping("modificar-marcador")
    public Partido cambiarMarcador(@RequestBody Partido partido, @AuthenticationPrincipal Long usuarioId){
        return partidoService.cambiarMarcador(partido, usuarioId);
    }
/*
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        partidoService.delete(id);
    }
*/
}
