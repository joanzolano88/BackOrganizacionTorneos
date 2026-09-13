package com.example.torneos.enums;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum EstadoTorneo {
    INSCRIPCIONES,
    @JsonAlias({"INSCRIPCIONES_ACRIVO"})
    INSCRIPCIONES_ACTIVO,
    ACTIVO,
    FINALIZADO
}