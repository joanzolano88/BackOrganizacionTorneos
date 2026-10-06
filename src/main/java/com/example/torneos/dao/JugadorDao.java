package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JugadorDao extends JpaRepository<Jugador, Long> {
    Optional<Jugador> findByIdentificacion(String identificacion);
    Optional<Jugador> findByNumeroCelular(String numeroCelular);
    List<Jugador> findByEquipo(Equipo equipo);
    List<Jugador> findByEquiposContaining(Equipo equipo);
}
