package com.example.torneos.dao;

import com.example.torneos.entities.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartamentoDao extends JpaRepository<Departamento, Long> {
    List<Departamento> findByPaisId(Long paisId);
    Departamento findByNombreAndPaisId(String nombre, Long paisId);
}
