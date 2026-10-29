package com.example.torneos.DTO;

public record DtoVallaMenosVencida(
        Long jugadorId,
        String arquero,
        Long equipoId,
        String equipo,
        long partidosJugados,
        long golesRecibidos) {}
