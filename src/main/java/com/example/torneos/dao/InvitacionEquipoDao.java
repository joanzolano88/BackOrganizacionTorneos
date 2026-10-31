package com.example.torneos.dao;

import com.example.torneos.entities.Equipo;
import com.example.torneos.entities.InvitacionEquipo;
import com.example.torneos.entities.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitacionEquipoDao extends JpaRepository<InvitacionEquipo, Long> {
    List<InvitacionEquipo> findByJugador_Persona_IdentificacionAndEstadoOrderByCreadaEnDesc(
            String identificacion, String estado);
    Optional<InvitacionEquipo> findByEquipoAndJugadorAndEstado(Equipo equipo, Jugador jugador, String estado);
}