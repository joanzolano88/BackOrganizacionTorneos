package com.example.torneos.rest;

import com.example.torneos.entities.PagoInscripcion;
import com.example.torneos.services.PagoInscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/equipo/{idEquipo}")
    public List<PagoInscripcion> listar(@PathVariable long idEquipo) {
        return pagoService.listarPorEquipo(idEquipo);
    }

    @PostMapping("/equipo/{idEquipo}")
    public PagoInscripcion registrar(@PathVariable long idEquipo, @RequestBody PagoInscripcion pago) {
        return pagoService.registrar(idEquipo, pago);
    }
}