package com.example.torneos.DTO;

public record DtoEstadisticasTorneo(
        DtoTablaEstadisticasTorneo faseGrupos,
        DtoTablaEstadisticasTorneo eliminatorias,
        DtoTablaEstadisticasTorneo torneoCompleto) {}
