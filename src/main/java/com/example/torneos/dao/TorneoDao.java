package com.example.torneos.dao;

import com.example.torneos.entities.Partido;
import com.example.torneos.entities.Torneo;
import com.example.torneos.entities.Usuario;
import com.example.torneos.enums.EstadoPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorneoDao extends JpaRepository<Torneo, Long> {
     List<Torneo> findByEncargadoTorneo(Usuario usuario);
     @Query("select distinct t from Torneo t left join fetch t.ciudad c left join fetch c.departamento d left join fetch t.deporte where (:departamento is null or lower(d.nombre) = lower(:departamento)) and (:usuarioId is null or t.encargadoTorneo.id = :usuarioId)")
     List<Torneo> buscarListado(@Param("departamento") String departamento, @Param("usuarioId") Long usuarioId);
}
