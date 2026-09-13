package com.example.torneos.dao;

import com.example.torneos.entities.Deporte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeporteDao extends JpaRepository<Deporte, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}
