package com.example.torneos.dao;

import com.example.torneos.DTO.DtoInvitacionEquipo;
import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.InvitacionEquipo;
import com.example.torneos.entities.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitacionEquipoDao extends JpaRepository<InvitacionEquipo, Long> {
    @Query("select new com.example.torneos.DTO.DtoInvitacionEquipo(i.id, e.id, e.nombre, j.id, j.nombre, j.cedula, i.estado, i.creadaEn) " +
            "from InvitacionEquipo i join i.equipo e join i.jugador j where j.cedula = :cedula and i.estado = :estado order by i.creadaEn desc")
    List<DtoInvitacionEquipo> listarDtoPorCedulaYEstado(@Param("cedula") String cedula, @Param("estado") String estado);

    @Query("select new com.example.torneos.DTO.DtoInvitacionEquipo(i.id, e.id, e.nombre, j.id, j.nombre, j.cedula, i.estado, i.creadaEn) " +
            "from InvitacionEquipo i join i.equipo e join i.jugador j where i.id = :id")
    Optional<DtoInvitacionEquipo> buscarDto(@Param("id") long id);

    Optional<InvitacionEquipo> findByEquipoAndJugadorAndEstado(Equipo equipo, Jugador jugador, String estado);
}