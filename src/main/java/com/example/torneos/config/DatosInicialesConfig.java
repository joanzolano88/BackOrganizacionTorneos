package com.example.torneos.config;

import com.example.torneos.dao.DeporteDao;
import com.example.torneos.dao.EquipoDao;
import com.example.torneos.dao.JugadorDao;
import com.example.torneos.dao.UsuarioDao;
import com.example.torneos.entities.Deporte;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import com.example.torneos.enums.EstadoJugador;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatosInicialesConfig {
    @Bean
    CommandLineRunner cargarDeportes(DeporteDao deporteDao, JugadorDao jugadorDao, EquipoDao equipoDao, UsuarioDao usuarioDao) {
        return args -> {
            crearSiNoExiste(deporteDao, "Futbol", 30, 11, 7, null);
            crearSiNoExiste(deporteDao, "Futbol sala", 12, 5, 4, null);
        };
    }
    private void crearSiNoExiste(DeporteDao dao, String nombre, int convocados, int maxTitulares, int minTitulares, String reglas) {
        if (dao.existsByNombreIgnoreCase(nombre)) return;
        Deporte deporte = new Deporte();
        deporte.setNombre(nombre);
        deporte.setMaximoConvocados(convocados);
        deporte.setMaximoTitulares(maxTitulares);
        deporte.setMinimoTitulares(minTitulares);
        deporte.setReglas(reglas);
        dao.save(deporte);
    }
}
