package com.example.torneos.DTO;

public record DtoCrearSolicitudSustitucion(
        Long titularId,
        Long suplenteId,
        Long jugadorId,
        Integer numeroUniforme,
        Integer minuto) {
}
