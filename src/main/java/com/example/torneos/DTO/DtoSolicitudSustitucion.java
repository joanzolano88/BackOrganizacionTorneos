package com.example.torneos.DTO;

import com.example.torneos.enums.EstadoSolicitudSustitucion;

import java.time.LocalDateTime;

public record DtoSolicitudSustitucion(
        Long id,
        Long partidoId,
        Long equipoId,
        String nombreEquipo,
        Long solicitanteId,
        Long titularId,
        Long suplenteId,
        Long jugadorId,
        Integer numeroUniforme,
        Integer minuto,
        EstadoSolicitudSustitucion estado,
        LocalDateTime creadaEn,
        Long resueltaPorId,
        LocalDateTime resueltaEn) {
}
