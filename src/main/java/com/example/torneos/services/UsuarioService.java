package com.example.torneos.services;

import com.example.torneos.DTO.DtoUsuarioInfo;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.Persona;
import com.example.torneos.DTO.DtoLoginInfo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioDao usuarioDao;
    @Autowired
    private PersonaDao personaDao;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Value("${security.registration.allow-organizer:false}")
    private boolean permitirRegistroOrganizador;
    public Usuario save(Usuario usuario) {
        if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank() ||
                usuario.getIdentificacion() == null || usuario.getIdentificacion().isBlank() ||
                usuario.getNumeroCelular() == null || usuario.getNumeroCelular().isBlank() ||
                usuario.getTipoUsuario() == null) {
            throw new IllegalArgumentException("Nombre, identificación, celular y tipo de perfil son obligatorios");
        }
        if (usuario.getContrasena() == null || usuario.getContrasena().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        if (usuario.getFechaNacimiento() != null && usuario.getFechaNacimiento().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        if (usuario.getTipoUsuario() == com.example.torneos.enums.TipoUsuario.ORGANIZADOR && !permitirRegistroOrganizador) {
            throw new IllegalArgumentException("El registro público no permite crear cuentas de organizador");
        }
        return usuarioDao.save(usuario);
    }
    public List<Usuario> getAll() {
        return usuarioDao.findAll();
    }
    public DtoUsuarioInfo iniciarSesion(DtoLoginInfo dtoLoginInfo) {
        String identificacion = dtoLoginInfo.getIdentificacion() == null ? "" : dtoLoginInfo.getIdentificacion().trim();
        Usuario usuario = usuarioDao.findByIdentificacion(identificacion);
        if (usuario != null && usuario.getContrasena() != null && coincideContrasena(usuario.getContrasena(), dtoLoginInfo.getContrasena())) {
            DtoUsuarioInfo usuarioInfo = new DtoUsuarioInfo();
            usuarioInfo.setId(usuario.getId());
            usuarioInfo.setTipoUsuario(usuario.getTipoUsuario());
            usuarioInfo.setNombre(usuario.getNombre());
            usuarioInfo.setIdentificacion(usuario.getIdentificacion());
            usuarioInfo.setNumeroCelular(usuario.getNumeroCelular());
            usuarioInfo.setUbicacion(usuario.getUbicacion());
            usuarioInfo.setToken(jwtTokenService.emitir(usuario.getId(), usuario.getIdentificacion(), usuario.getTipoUsuario().name()));
            return usuarioInfo;
        }
        throw new IllegalArgumentException("Identificación o contraseña inválida");
    }

    private boolean coincideContrasena(String guardada, String ingresada) {
        if (ingresada == null || guardada == null) return false;
        return guardada.equals(ingresada);
    }

    private boolean esHashBcrypt(String contrasena) {
        return false;
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
    public Usuario update(Usuario usuario, long usuarioAutenticadoId) {
        if (usuario.getId() != usuarioAutenticadoId) {
            throw new IllegalArgumentException("Solo puedes modificar tu propio perfil");
        }
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
