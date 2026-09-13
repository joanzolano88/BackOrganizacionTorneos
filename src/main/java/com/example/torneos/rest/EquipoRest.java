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
import org.springframework.http.MediaType;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo save(@RequestPart("objeto") String equipoS,
                       @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                       @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.save(equipo);
    }

    @PostMapping(value = "/solicitud", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo saveSolicitud(@RequestPart("objeto") String equipoS,
                               @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                               @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null) {
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.saveSolicitud(equipo);
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
    @GetMapping("/solicitudes/torneo/{id}")
    public List<Equipo> getSolicitudesByTorneo(@PathVariable long id){
        return equipoService.getSolicitudesByTorneo(id);
    }
    @GetMapping("/solicitudes/torneo/{id}/todas")
    public List<Equipo> getSolicitudesYAceptadasByTorneo(@PathVariable long id){
        return equipoService.getSolicitudesYAceptadasByTorneo(id);
    }
    @GetMapping("/delegado/{idUsuario}")
    public List<Equipo> getByDelegadoUsuario(@PathVariable long idUsuario){
        return equipoService.getByDelegadoUsuario(idUsuario);
    }
    @GetMapping("/torneo/modalidad/{id}/{modalidadTorneo}")
    public List<Equipo> getByTorneoModalidad(@PathVariable long id, @PathVariable ModalidadTorneo modalidadTorneo){
        return equipoService.getByTorneoModalidad(id, modalidadTorneo);
    }
    @GetMapping("/torneo/fase/{id}/{faseTorneo}")
    public List<Equipo> getByTorneoFase(@PathVariable long id, @PathVariable FaseActual faseTorneo){
        return equipoService.getByTorneoFase(id, faseTorneo);
    }
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Equipo update(@RequestPart("objeto") String equipoS,
                         @Nullable @RequestPart(value = "archivo1", required = false) MultipartFile fileE,
                         @Nullable @RequestPart(value = "archivo2", required = false) MultipartFile fileB,
                         @Nullable @RequestPart(value = "usuarioId", required = false) Long usuarioId)  throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Equipo equipo = mapper.readValue(equipoS, Equipo.class);
        if (fileE != null) {
            equipo.setEscudo(fileE.getBytes());
        }
        if (fileB != null){
            equipo.setBandera(fileB.getBytes());
        }
        return equipoService.update(equipo, usuarioId);
    }

    @PutMapping("/solicitud/{id}/aceptar")
    @ResponseStatus(HttpStatus.OK)
    public Equipo aceptarSolicitud(@PathVariable long id){
        return equipoService.aceptarSolicitud(id);
    }

    @DeleteMapping("/solicitud/{id}/rechazar")
    public void rechazarSolicitud(@PathVariable long id){
        equipoService.rechazarSolicitud(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        equipoService.delete(id);
    }
}
