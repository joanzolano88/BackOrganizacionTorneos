package com.example.torneos.dao;

import com.example.torneos.entities.Cancha;
import com.example.torneos.entities.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanchaDao extends JpaRepository<Cancha, Long> {
    List<Cancha> findByTorneo(Torneo torneo);
}
