package com.example.torneos.services;

import com.example.torneos.dao.PersonaDao;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.Persona;
import com.example.torneos.entities.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {
    @Mock
    private PersonaDao personaDao;

    @InjectMocks
    private PersonaService personaService;

    @Test
    void vincular_usuarioYJugadorConLaMismaIdentificacionCompartenPersona() {
        when(personaDao.findByIdentificacion("123456")).thenReturn(Optional.empty());
        when(personaDao.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setIdentificacion("123456");

        Persona personaCompartida = personaService.vincular(usuario);
        when(personaDao.findByIdentificacion("123456")).thenReturn(Optional.of(personaCompartida));

        Jugador jugador = new Jugador();
        jugador.setNombre("Ana");
        jugador.setIdentificacion("123456");

        assertSame(personaCompartida, personaService.vincular(jugador));
        assertSame(usuario.getPersona(), jugador.getPersona());
    }
}
