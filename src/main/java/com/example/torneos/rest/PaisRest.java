package com.example.torneos.rest;

import com.example.torneos.entities.Pais;
import com.example.torneos.services.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pais")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class PaisRest {
    @Autowired
    private UbicacionService ubicacionService;

    @GetMapping
    public List<Pais> getAll() {
        return ubicacionService.listarPaises();
    }
}
