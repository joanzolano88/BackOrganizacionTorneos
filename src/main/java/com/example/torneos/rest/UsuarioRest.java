package com.example.torneos.rest;

import com.example.torneos.DTO.DtoLoginInfo;
import com.example.torneos.DTO.DtoUsuarioInfo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.services.UsuarioService;
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
@RequestMapping("/usuario")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class UsuarioRest {
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario save(@RequestBody @RequestParam("objeto") String usuarioS,
                        @Nullable @RequestBody @RequestParam("foto") MultipartFile fileF,
                        @Nullable @RequestBody @RequestParam("identificacion") MultipartFile fileI) throws JsonMappingException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Usuario usuario = mapper.readValue(usuarioS, Usuario.class);
        if (fileF != null) {
            usuario.setFoto(fileF.getBytes());
        }
        if (fileI != null){
            usuario.setIdentificacion(fileI.getBytes());
        }
        return usuarioService.save(usuario);
    }

    @GetMapping
    public List<Usuario> getAll(){
        return usuarioService.getAll();
    }

    @PostMapping("/login")
    public DtoUsuarioInfo iniciarSesion(@RequestBody DtoLoginInfo dtoLoginInfo){
        return usuarioService.iniciarSesion(dtoLoginInfo);
    }

    @GetMapping("/{id}")
    public Usuario getById(@PathVariable long id){
        return usuarioService.getById(id);
    }

    @PutMapping()
    public Usuario update(@RequestBody Usuario usuario){
        return usuarioService.update(usuario);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id){
        usuarioService.delete(id);
    }
}
