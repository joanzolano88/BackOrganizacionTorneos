package com.example.torneos.rest;

import com.example.torneos.entities.ConvocatoriaPartido;
import com.example.torneos.entities.InvitacionPartido;
import com.example.torneos.entities.Jugador;
import com.example.torneos.services.ConvocatoriaPartidoService;
import com.example.torneos.enums.TipoEventoPartido;
import com.example.torneos.entities.EventoPartido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partido")
@CrossOrigin(origins = "*")
public class ConvocatoriaPartidoRest {
    @Autowired private ConvocatoriaPartidoService service;

    @GetMapping("/{partidoId}/convocados")
    public List<ConvocatoriaPartido> listar(@PathVariable long partidoId) { return service.listar(partidoId); }

    @GetMapping("/{partidoId}/jugadores-disponibles/{equipoId}")
    public List<Jugador> disponibles(@PathVariable long equipoId) { return service.jugadoresDelEquipo(equipoId); }

    @GetMapping("/{partidoId}/jugador/cedula/{cedula}")
    public Jugador buscar(@PathVariable long partidoId, @PathVariable String cedula, @RequestParam long usuarioId) { return service.buscarPorCedula(partidoId, cedula, usuarioId); }

    @GetMapping("/{partidoId}/eventos")
    public List<EventoPartido> eventos(@PathVariable long partidoId) { return service.eventos(partidoId); }

    @PostMapping("/{partidoId}/eventos")
    public EventoPartido registrarEvento(@PathVariable long partidoId, @RequestParam long jugadorId, @RequestParam TipoEventoPartido tipo, @RequestParam(defaultValue = "0") int minuto, @RequestParam long usuarioId) {
        return service.registrarEvento(partidoId, jugadorId, tipo, minuto, usuarioId);
    }

    @PostMapping("/{partidoId}/convocados")
    public ConvocatoriaPartido agregar(@PathVariable long partidoId, @RequestParam long jugadorId, @RequestParam(defaultValue = "false") boolean titular, @RequestParam long usuarioId) {
        return service.agregar(partidoId, jugadorId, titular, usuarioId);
    }

    @PostMapping("/{partidoId}/convocados/cedula")
    public ConvocatoriaPartido agregarPorCedula(@PathVariable long partidoId, @RequestParam String cedula, @RequestParam(defaultValue = "false") boolean titular, @RequestParam long usuarioId) {
        return service.agregarPorCedula(partidoId, cedula, titular, usuarioId);
    }

    @PutMapping("/convocados/{convocatoriaId}/titular")
    public ConvocatoriaPartido titular(@PathVariable long convocatoriaId, @RequestParam boolean titular, @RequestParam long usuarioId) {
        return service.cambiarTitular(convocatoriaId, titular, usuarioId);
    }

    @PutMapping("/convocados/{convocatoriaId}/numero")
    public ConvocatoriaPartido numero(@PathVariable long convocatoriaId, @RequestParam int numero, @RequestParam long usuarioId) {
        return service.cambiarNumeroUniforme(convocatoriaId, numero, usuarioId);
    }

    @DeleteMapping("/convocados/{convocatoriaId}")
    public void eliminar(@PathVariable long convocatoriaId, @RequestParam long usuarioId) { service.eliminar(convocatoriaId, usuarioId); }

    @PostMapping("/{partidoId}/convocados/validar")
    public void validar(@PathVariable long partidoId, @RequestParam long usuarioId) { service.validarTitulares(partidoId, usuarioId); }

    @PostMapping("/{partidoId}/invitacion")
    public InvitacionPartido invitacion(@PathVariable long partidoId, @RequestParam long equipoId, @RequestParam long usuarioId) {
        return service.crearInvitacion(partidoId, equipoId, usuarioId);
    }

    @PostMapping("/invitacion/{token}/aceptar")
    public ConvocatoriaPartido aceptar(@PathVariable String token, @RequestParam long usuarioId) { return service.aceptarInvitacion(token, usuarioId); }
}
