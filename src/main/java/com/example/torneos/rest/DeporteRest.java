package com.example.torneos.rest;

import com.example.torneos.dao.DeporteDao;
import com.example.torneos.entities.Deporte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deporte")
@CrossOrigin(origins = "*")
public class DeporteRest {
    @Autowired private DeporteDao deporteDao;

    @GetMapping
    public List<Deporte> listar() { return deporteDao.findAll(); }

    @GetMapping("/{id}")
    public Deporte buscar(@PathVariable long id) { return deporteDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El deporte no existe")); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Deporte guardar(@RequestBody Deporte deporte) {
        if (deporte.getNombre() == null || deporte.getNombre().isBlank()) throw new IllegalArgumentException("El nombre del deporte es obligatorio");
        if (deporte.getMinimoTitulares() < 1 || deporte.getMaximoTitulares() < deporte.getMinimoTitulares()) throw new IllegalArgumentException("Los límites de titulares no son válidos");
        if (deporte.getMaximoConvocados() < deporte.getMaximoTitulares()) throw new IllegalArgumentException("Los convocados no pueden ser menos que los titulares");
        return deporteDao.save(deporte);
    }

    @PutMapping("/{id}")
    public Deporte actualizar(@PathVariable long id, @RequestBody Deporte datos) {
        Deporte deporte = buscar(id);
        deporte.setNombre(datos.getNombre());
        deporte.setReglas(datos.getReglas());
        deporte.setMinimoTitulares(datos.getMinimoTitulares());
        deporte.setMaximoTitulares(datos.getMaximoTitulares());
        deporte.setMaximoConvocados(datos.getMaximoConvocados());
        return guardar(deporte);
    }
}
