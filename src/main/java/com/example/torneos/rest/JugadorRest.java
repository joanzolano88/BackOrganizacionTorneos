package com.example.torneos.rest;

import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.entities.Jugador;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jugador")
@CrossOrigin(origins = "*")
public class JugadorRest {
    @Autowired private JugadorDao jugadorDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionDao;

    @GetMapping("/{id}/perfil")
    public Map<String, Object> perfil(@PathVariable long id) {
        Jugador jugador = jugadorDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        return Map.of("jugador", jugador, "equipos", jugador.getEquipos(), "participaciones", participacionDao.findAll().stream().filter(item -> item.getJugador().getId() == id).toList());
    }
}