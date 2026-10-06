package com.example.torneos.rest;

import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.InvitacionPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.services.ConvocatoriaPartidoService;
import com.example.torneos.enums.TipoEventoPartido;
import com.example.torneos.entities.EventoPartido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partido")
@CrossOrigin(origins = "*")
public class ConvocatoriaPartidoRest {
    @Autowired private ConvocatoriaPartidoService service;
    @Autowired private com.example.torneos.services.SancionJugadorTorneoService sancionService;

    @GetMapping("/{partidoId}/convocados")
    public List<ConvocatoriaPartido> listar(@PathVariable long partidoId) { return service.listar(partidoId); }

    @GetMapping("/{partidoId}/jugadores-disponibles/{equipoId}")
    public List<Jugador> disponibles(@PathVariable long equipoId) { return service.jugadoresDelEquipo(equipoId); }

    @GetMapping("/{partidoId}/jugador/identificacion/{identificacion}")
    public Jugador buscar(@PathVariable long partidoId, @PathVariable String identificacion, @AuthenticationPrincipal Long usuarioId) { return service.buscarPorIdentificacion(partidoId, identificacion, usuarioId); }

    @GetMapping("/{partidoId}/eventos")
    public List<EventoPartido> eventos(@PathVariable long partidoId) { return service.eventos(partidoId); }

    @PostMapping("/{partidoId}/eventos")
    public EventoPartido registrarEvento(@PathVariable long partidoId, @RequestParam long jugadorId, @RequestParam TipoEventoPartido tipo, @RequestParam(defaultValue = "0") int minuto, @AuthenticationPrincipal Long usuarioId) {
        return service.registrarEvento(partidoId, jugadorId, tipo, minuto, usuarioId);
    }

    @PostMapping("/{partidoId}/convocados")
    public ConvocatoriaPartido agregar(@PathVariable long partidoId, @RequestParam long jugadorId, @RequestParam(defaultValue = "false") boolean titular, @AuthenticationPrincipal Long usuarioId) {
        return service.agregar(partidoId, jugadorId, titular, usuarioId);
    }

    @PostMapping("/{partidoId}/convocados/identificacion")
    public ConvocatoriaPartido agregarPorIdentificacion(@PathVariable long partidoId, @RequestParam String identificacion, @RequestParam(defaultValue = "false") boolean titular, @AuthenticationPrincipal Long usuarioId) {
        return service.agregarPorIdentificacion(partidoId, identificacion, titular, usuarioId);
    }

    @PutMapping("/convocados/{convocatoriaId}/titular")
    public ConvocatoriaPartido titular(@PathVariable long convocatoriaId, @RequestParam boolean titular, @AuthenticationPrincipal Long usuarioId) {
        return service.cambiarTitular(convocatoriaId, titular, usuarioId);
    }

    @PostMapping("/{partidoId}/sustituciones")
    public void sustituir(@PathVariable long partidoId, @RequestParam long titularId, @RequestParam long suplenteId, @AuthenticationPrincipal Long usuarioId) {
        service.sustituir(partidoId, titularId, suplenteId, usuarioId);
    }

    @PutMapping("/convocados/{convocatoriaId}/numero")
    public ConvocatoriaPartido numero(@PathVariable long convocatoriaId, @RequestParam int numero, @AuthenticationPrincipal Long usuarioId) {
        return service.cambiarNumeroUniforme(convocatoriaId, numero, usuarioId);
    }

    @DeleteMapping("/convocados/{convocatoriaId}")
    public void eliminar(@PathVariable long convocatoriaId, @AuthenticationPrincipal Long usuarioId) { service.eliminar(convocatoriaId, usuarioId); }

    @PostMapping("/{partidoId}/convocados/validar")
    public void validar(@PathVariable long partidoId, @AuthenticationPrincipal Long usuarioId) { service.validarTitulares(partidoId, usuarioId); }

    @PostMapping("/{partidoId}/invitacion")
    public InvitacionPartido invitacion(@PathVariable long partidoId, @RequestParam long equipoId, @AuthenticationPrincipal Long usuarioId) {
        return service.crearInvitacion(partidoId, equipoId, usuarioId);
    }

    @PostMapping("/invitacion/{token}/aceptar")
    public ConvocatoriaPartido aceptar(@PathVariable String token, @AuthenticationPrincipal Long usuarioId) { return service.aceptarInvitacion(token, usuarioId); }

    @GetMapping("/torneo/{torneoId}/sanciones")
    public List<com.example.torneos.entities.SancionJugadorTorneo> sanciones(@PathVariable long torneoId) {
        return sancionService.listarTorneo(torneoId);
    }

    @PutMapping("/{partidoId}/sanciones/{jugadorId}/levantar")
    public com.example.torneos.entities.SancionJugadorTorneo levantarSancion(@PathVariable long partidoId, @PathVariable long jugadorId, @AuthenticationPrincipal Long usuarioId) {
        return sancionService.modificarEnPartido(partidoId, jugadorId, usuarioId, true);
    }

    @PutMapping("/{partidoId}/sanciones/{jugadorId}/restaurar")
    public com.example.torneos.entities.SancionJugadorTorneo restaurarSancion(@PathVariable long partidoId, @PathVariable long jugadorId, @AuthenticationPrincipal Long usuarioId) {
        return sancionService.modificarEnPartido(partidoId, jugadorId, usuarioId, false);
    }
}
