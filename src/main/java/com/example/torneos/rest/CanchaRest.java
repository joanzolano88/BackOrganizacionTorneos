package com.example.torneos.rest;

import com.example.torneos.entities.Cancha;
import com.example.torneos.services.CanchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cancha")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class CanchaRest {
    @Autowired
    private CanchaService canchaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cancha save(@RequestBody Cancha cancha) {
        return canchaService.save(cancha);
    }
    @GetMapping
    public List<Cancha> getAll() {
        return canchaService.getAll();
    }
    @GetMapping("/{id}")
    public Cancha getById(@PathVariable long id) {
        return canchaService.getById(id);
    }
    @GetMapping("/torneo/{id}")
    public List<Cancha> getByTorneo(@PathVariable long id) {
        return canchaService.getByTorneo(id);
    }
    @PutMapping
    public Cancha update(@RequestBody Cancha cancha) {
        return canchaService.update(cancha);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        canchaService.delete(id);
    }
}
