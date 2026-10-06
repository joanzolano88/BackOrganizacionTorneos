package com.example.torneos.services;

import com.example.torneos.DTO.DtoLoginInfo;
import com.example.torneos.DTO.DtoUsuarioInfo;
import com.example.torneos.dao.PersonaDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.TipoUsuario;
import com.example.torneos.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {
    @Mock
    private UsuarioDao usuarioDao;

    @Mock
    private PersonaDao personaDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void iniciarSesion_aceptaLoginCuandoExisteIdentificacion() {
        DtoLoginInfo dtoLoginInfo = new DtoLoginInfo();
        dtoLoginInfo.setIdentificacion("123456");
        dtoLoginInfo.setContrasena("secret");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setIdentificacion("123456");
        usuario.setContrasena("secret");
        usuario.setTipoUsuario(TipoUsuario.JUGADOR);

        when(usuarioDao.findByIdentificacion("123456")).thenReturn(usuario);
        when(jwtTokenService.emitir(1L, "123456", TipoUsuario.JUGADOR.name())).thenReturn("token-test");

        DtoUsuarioInfo info = usuarioService.iniciarSesion(dtoLoginInfo);

        assertEquals(TipoUsuario.JUGADOR, info.getTipoUsuario());
        assertEquals("token-test", info.getToken());
    }
}
