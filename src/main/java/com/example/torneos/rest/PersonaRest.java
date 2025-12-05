package com.example.torneos.rest;

import com.example.torneos.entities.Persona;
import com.example.torneos.services.PersonaService;
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
@RequestMapping("/persona")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class PersonaRest {
    @Autowired
    private PersonaService personaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Persona save(@RequestBody @RequestParam("objeto") String pesonaS,
                        @Nullable @RequestBody @RequestParam("archivo1") MultipartFile fileF,
                        @Nullable @RequestBody @RequestParam("archivo2") MultipartFile fileI) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Persona persona = mapper.readValue(pesonaS, Persona.class);
        if (fileF != null) {
            persona.setFoto(fileF.getBytes());
        }
        if (fileI != null){
            persona.setIdentificacion(fileI.getBytes());
        }
        return personaService.save(persona);
    }

    @GetMapping
    public List<Persona> getAll(){
        return personaService.getAll();
    }

    @GetMapping("/{id}")
    public Persona getById(@PathVariable long id){
        return personaService.getById(id);
    }

    @PutMapping()
    public Persona update(@RequestBody Persona persona){
        return personaService.update(persona);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        personaService.delete(id);
    }
}
