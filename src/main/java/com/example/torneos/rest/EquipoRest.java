package com.example.torneos.rest;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.entities.Equipo;
import com.example.torneos.enums.FaseActual;
import com.example.torneos.enums.ModalidadTorneo;
import com.example.torneos.services.EquipoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/equipo")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class EquipoRest {
    @Autowired
    private EquipoService equipoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo save(@RequestBody @RequestParam("objeto") String equipoS,
                       @Nullable @RequestBody @RequestParam("archivo1") MultipartFile fileE,
                       @Nullable @RequestBody @RequestParam("archivo2") MultipartFile fileB) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileE.getBytes());
        }
        return equipoService.save(equipo);
    }

    @GetMapping
    public List<Equipo> getAll(){
        return equipoService.getAll();
    }

    @GetMapping("/{id}")
    public Equipo getById(@PathVariable long id){
        return equipoService.getById(id);
    }
    @GetMapping("/torneo/{id}")
    public List<Equipo> getByTorneo(@PathVariable long id){
        return equipoService.getByTorneo(id);
    }
    @GetMapping("/torneo/modalidad/{id}/{modalidadTorneo}")
    public List<Equipo> getByTorneoModalidad(@PathVariable long id, @PathVariable ModalidadTorneo modalidadTorneo){
        return equipoService.getByTorneoModalidad(id, modalidadTorneo);
    }
    @GetMapping("/torneo/fase/{id}/{faseTorneo}")
    public List<Equipo> getByTorneoFase(@PathVariable long id, @PathVariable FaseActual faseTorneo){
        return equipoService.getByTorneoFase(id, faseTorneo);
    }
    @PutMapping()
    public Equipo update(@RequestBody @RequestParam("objeto") String equipoS,
                         @Nullable @RequestBody @RequestParam("archivo1") MultipartFile fileE,
                         @Nullable @RequestBody @RequestParam("archivo2") MultipartFile fileB)  throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileE.getBytes());
        }
        return equipoService.update(equipo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        equipoService.delete(id);
    }
}
