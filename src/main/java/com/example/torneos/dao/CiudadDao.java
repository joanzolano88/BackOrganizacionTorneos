package com.example.torneos.dao;

import com.example.torneos.entities.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiudadDao extends JpaRepository<Ciudad, Long> {
    List<Ciudad> findByDepartamentoId(Long departamentoId);
    Ciudad findByNombreAndDepartamentoId(String nombre, Long departamentoId);
    List<Ciudad> findByDepartamentoPaisId(Long paisId);
}
