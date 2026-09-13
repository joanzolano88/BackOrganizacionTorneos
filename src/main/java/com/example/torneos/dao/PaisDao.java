package com.example.torneos.dao;

import com.example.torneos.entities.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaisDao extends JpaRepository<Pais, Long> {
    Pais findByNombre(String nombre);
}
