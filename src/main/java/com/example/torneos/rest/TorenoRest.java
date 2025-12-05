package com.example.torneos.rest;

import com.example.torneos.DTO.DtoGrupoEquipo;
import com.example.torneos.DTO.DtoOptionTorneo;
import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Reglamento;
import com.example.torneos.entities.Torneo;
import com.example.torneos.services.TorenoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/torneo")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class TorenoRest {
    @Autowired
    private TorenoService torenoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Torneo save(@RequestBody @RequestParam("objeto") String torneoS) throws JsonMappingException, JsonProcessingException, IOException {
        //@RequestBody @RequestParam("archivo") MultipartFile file
        ObjectMapper mapper = new ObjectMapper();
        Torneo torneo = mapper.readValue(torneoS, Torneo.class);
        //Reglamento reglamento = new Reglamento();
        //reglamento.setReglamento(file.getBytes());
        return torenoService.save(torneo, null);
    }

    @GetMapping
    public List<Torneo> getAll(){
        return torenoService.getAll();
    }

    @GetMapping("/option")
    public List<DtoOptionTorneo> getOptionAll() {
        return torenoService.getOptionAll();
    }

    @GetMapping("/{id}")
    public Torneo getById(@PathVariable long id){
        return torenoService.getById(id);
    }

    @PutMapping("/cambiar_fase_torneo/{idTorneo}")
    public void cabiarFaseTorneo(@RequestBody List<DtoGrupoEquipo> listGrupoEquipo, @PathVariable long idTorneo) {
        torenoService.cabiarFaseTorneo(listGrupoEquipo, idTorneo);
    }

    @GetMapping("/usuario/{id}")
    public List<Torneo>  getByUsuarioId(@PathVariable long id){
        return torenoService.getByUsuarioId(id);
    }
    @GetMapping("/reglamento/{id}")
    public Reglamento getReglamento(@PathVariable long id){
        return torenoService.getReglamento(id);
    }

    @PutMapping("/cambiar_fase/{idTorneo}")
    public void camabiarFase(@PathVariable long idTorneo){
        torenoService.cambiarFaseTorneo(idTorneo);
    }
    @PutMapping()
    public Torneo update(@RequestBody Torneo torneo){
        return torenoService.update(torneo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        torenoService.delete(id);
    }
}
