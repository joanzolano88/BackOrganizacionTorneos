package com.example.torneos.dao;

import com.example.torneos.entities.Reglamento;
import com.example.torneos.entities.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReglamentoDao extends JpaRepository<Reglamento, Long> {
    Reglamento findByTorneo(Torneo torneo);
}
