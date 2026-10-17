package com.example.torneos.DTO;

import com.example.torneos.enums.TipoEventoPartido;

public record DtoEventoPartido(
        Long id,
        TipoEventoPartido tipo,
        int minuto,
        Long jugadorId) {}
