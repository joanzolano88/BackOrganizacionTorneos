package com.example.torneos.DTO;

public record DtoBusquedaJugadorEquipo(
        boolean existente,
        boolean elegible,
        Long jugadorId,
        String nombre,
        String identificacion,
        String mensaje) {}
