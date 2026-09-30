package com.example.torneos.rest;

import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.ParticipacionJugadorTorneoDao;
import com.example.torneos.dao.EventoPartidoDao;
import com.example.torneos.dao.SancionJugadorTorneoDao;
import com.example.torneos.entities.Jugador;
import com.example.torneos.entities.ParticipacionJugadorTorneo;
import com.example.torneos.entities.Torneo;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jugador")
@CrossOrigin(origins = "*")
public class JugadorRest {
    @Autowired private JugadorDao jugadorDao;
    @Autowired private ParticipacionJugadorTorneoDao participacionDao;
    @Autowired private EventoPartidoDao eventoDao;
    @Autowired private SancionJugadorTorneoDao sancionDao;

    @GetMapping("/{id}/perfil")
    public Map<String, Object> perfil(@PathVariable long id, @RequestParam(required = false) Long torneoId) {
        Jugador jugador = jugadorDao.findById(id).orElseThrow(() -> new IllegalArgumentException("El jugador no existe"));
        List<ParticipacionJugadorTorneo> participaciones = participacionDao.findAll().stream()
                .filter(item -> item.getJugador().getId() == id)
                .filter(item -> torneoId == null || item.getTorneo().getId() == torneoId)
                .toList();
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("jugador", jugador);
        respuesta.put("equipos", jugador.getEquipos());
        respuesta.put("participaciones", participaciones);
        respuesta.put("participacionTorneo", participaciones.stream().findFirst().orElse(null));
        if (torneoId != null) {
            participaciones.stream().findFirst().ifPresent(participacion ->
                sancionDao.findByTorneoAndJugadorAndActivaTrue(participacion.getTorneo(), jugador)
                    .ifPresent(sancion -> respuesta.put("sancionTorneo", sancion)));
        }
        if (torneoId != null) {
            respuesta.put("eventosTorneo", eventoDao.findAll().stream()
                    .filter(evento -> evento.getJugador().getId() == id)
                    .filter(evento -> evento.getPartido().getTorneo().getId() == torneoId)
                    .toList());
        }
        return respuesta;
    }
}