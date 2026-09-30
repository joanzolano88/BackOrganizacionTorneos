package com.example.torneos.services;

import com.example.torneos.DTO.DtoUsuarioInfo;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.Persona;
import com.example.torneos.DTO.DtoLoginInfo;
import com.example.torneos.entities.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private PersonaDao personaDao;
    public Usuario save(Usuario usuario) {
        return usuarioDao.save(usuario);
    }
    public List<Usuario> getAll() {
        return usuarioDao.findAll();
    }
    public DtoUsuarioInfo iniciarSesion(DtoLoginInfo dtoLoginInfo) {
        String identificacion = dtoLoginInfo.getIdentificacion() == null ? "" : dtoLoginInfo.getIdentificacion().trim();
        Usuario usuario = usuarioDao.findByCedula(identificacion);
        if (usuario != null && usuario.getContrasena() != null && usuario.getContrasena().equals(dtoLoginInfo.getContrasena()) &&
            usuario.getCedula() != null && usuario.getCedula().equals(identificacion)){
            DtoUsuarioInfo usuarioInfo = new DtoUsuarioInfo();
            usuarioInfo.setId(usuario.getId());
            usuarioInfo.setTipoUsuario(usuario.getTipoUsuario());
            usuarioInfo.setNombre(usuario.getNombre());
            usuarioInfo.setNumeroCelular(usuario.getNumeroCelular());
            usuarioInfo.setUbicacion(usuario.getUbicacion());
            return usuarioInfo;
        }
        throw new IllegalArgumentException("Identificación o contraseña inválida");
    }
    public Usuario getById(long id) {
        Usuario usuario = usuarioDao.findById(id).orElse(null);
        if (usuario == null) {
            throw  new IllegalArgumentException("No existen Usuario con el id:" + id);
        }
        return usuario;
    }

    public Persona buscarPersonaPorCelular(String celular) {
        return personaDao.findAll().stream()
                .filter(persona -> celular.equals(persona.getNumeroCelular()))
                .findFirst()
                .orElse(null);
    }
    public Usuario update(Usuario usuario) {
        Optional<Usuario> optUsuario = usuarioDao.findById(usuario.getId());
        if (!optUsuario.isPresent()) {
            throw  new IllegalArgumentException("No existe Usuario con id: " + usuario.getId());
        }
        Usuario usuarioDB = optUsuario.get();
        usuarioDB.setNombre(usuario.getNombre());
        usuarioDB.setCorreoElectronico(usuario.getCorreoElectronico());
        usuarioDB.setFoto(usuario.getFoto());
        usuarioDB.setIdentificacion(usuario.getIdentificacion());
        usuarioDB.setNumeroCelular(usuario.getNumeroCelular());
        usuarioDB.setNumeroTelefono(usuario.getNumeroTelefono());
        usuarioDB.setWhatsappActivo(usuario.isWhatsappActivo());
        usuarioDB.setUbicacion(usuario.getUbicacion());
        if (usuario.getFechaNacimiento() != null) {
            usuarioDB.setFechaNacimiento(usuario.getFechaNacimiento());
        }
        return usuarioDao.save(usuarioDB);
    }
    public void delete(long id) {
        Optional<Usuario> optUsuario = usuarioDao.findById(id);
        if (optUsuario.isPresent()) {
            usuarioDao.delete(optUsuario.get());
        }
    }
}
