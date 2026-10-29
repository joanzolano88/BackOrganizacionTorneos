package com.example.torneos.DTO;

public record DtoGoleadorTorneo(
        Long jugadorId,
        String jugador,
        Long equipoId,
        String equipo,
        long goles) {}
