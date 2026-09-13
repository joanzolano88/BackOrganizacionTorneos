package com.example.torneos.rest;

import com.example.torneos.entities.Ciudad;
import com.example.torneos.services.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ciudad")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class CiudadRest {
    @Autowired
    private UbicacionService ubicacionService;

    @GetMapping("/departamento/{departamentoId}")
    public List<Ciudad> getByDepartamento(@PathVariable Long departamentoId) {
        return ubicacionService.listarCiudadesPorDepartamento(departamentoId);
    }
}
