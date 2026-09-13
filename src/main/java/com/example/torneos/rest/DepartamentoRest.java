package com.example.torneos.rest;

import com.example.torneos.entities.Departamento;
import com.example.torneos.services.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departamento")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class DepartamentoRest {
    @Autowired
    private UbicacionService ubicacionService;

    @GetMapping("/pais/{paisId}")
    public List<Departamento> getByPais(@PathVariable Long paisId) {
        return ubicacionService.listarDepartamentosPorPais(paisId);
    }
}
