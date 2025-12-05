package com.example.torneos.dao;

import com.example.torneos.entities.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ImagenDao extends JpaRepository<Imagen, Long> {
}
