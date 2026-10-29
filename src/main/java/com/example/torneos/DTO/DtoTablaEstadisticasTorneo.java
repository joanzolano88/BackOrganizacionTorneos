package com.example.torneos.DTO;

import java.util.List;

public record DtoTablaEstadisticasTorneo(
        List<DtoGoleadorTorneo> goleadores,
        List<DtoVallaMenosVencida> vallasMenosVencidas) {}
