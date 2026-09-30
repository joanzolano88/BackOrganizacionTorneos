package com.example.torneos.rest;

import com.example.torneos.DTO.DtoPago;
import com.example.torneos.entities.Pago;
import com.example.torneos.services.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos")
public class PagoRest {
    @Autowired private PagoService pagoService;

    @GetMapping("/torneo/{idTorneo}")
    public List<DtoPago> listar(@PathVariable long idTorneo, @RequestParam long usuarioId) {
        return pagoService.listar(idTorneo, usuarioId);
    }

    @PostMapping("/torneo/{idTorneo}")
    public DtoPago registrar(@PathVariable long idTorneo, @RequestBody Pago pago) {
        return pagoService.registrar(idTorneo, pago);
    }
}