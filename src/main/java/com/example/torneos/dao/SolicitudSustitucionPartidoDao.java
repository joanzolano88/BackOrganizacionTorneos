package com.example.torneos.dao;

import com.example.torneos.entities.Partido;
import com.example.torneos.entities.SolicitudSustitucionPartido;
import com.example.torneos.enums.EstadoSolicitudSustitucion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudSustitucionPartidoDao extends JpaRepository<SolicitudSustitucionPartido, Long> {
    boolean existsByPartido_IdAndEquipo_IdAndEstado(
            Long partidoId, Long equipoId, EstadoSolicitudSustitucion estado);

    List<SolicitudSustitucionPartido> findByPartidoOrderByCreadaEnDesc(Partido partido);

    List<SolicitudSustitucionPartido> findByPartidoAndEquipoOrderByCreadaEnDesc(Partido partido, com.example.torneos.entities.Equipo equipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select solicitud from SolicitudSustitucionPartido solicitud where solicitud.id = :id")
    Optional<SolicitudSustitucionPartido> findByIdForUpdate(@Param("id") Long id);
}
