package com.example.torneos.rest;

import com.example.torneos.entities.PagoInscripcion;
import com.example.torneos.services.PagoInscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pago-inscripcion")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class PagoInscripcionRest {
    @Autowired
    private PagoInscripcionService pagoService;

    @GetMapping("/equipo/{idEquipo}/torneo/{idTorneo}")
    public List<PagoInscripcion> listar(@PathVariable long idEquipo, @PathVariable long idTorneo,
                                        @AuthenticationPrincipal Long usuarioId) {
        return pagoService.listarPorEquipo(idEquipo, idTorneo, usuarioId);
    }

    @PostMapping("/equipo/{idEquipo}/torneo/{idTorneo}")
    public PagoInscripcion registrar(@PathVariable long idEquipo, @PathVariable long idTorneo,
                                     @RequestBody PagoInscripcion pago, @AuthenticationPrincipal Long usuarioId) {
        return pagoService.registrar(idEquipo, idTorneo, pago, usuarioId);
    }
}