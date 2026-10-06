package com.example.torneos.dao;

import com.example.torneos.entities.AyudanteTorneo;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AyudanteTorneoDao extends JpaRepository<AyudanteTorneo, Long> {
    List<AyudanteTorneo> findByTorneo(Torneo torneo);
    Optional<AyudanteTorneo> findByTorneoAndUsuario(Torneo torneo, Usuario usuario);
    void deleteByTorneoAndUsuario(Torneo torneo, Usuario usuario);
}