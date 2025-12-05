package com.example.torneos.dao;

import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorneoDao extends JpaRepository<Torneo, Long> {
     List<Torneo> findByEncargadoTorneo(Usuario usuario);
}
