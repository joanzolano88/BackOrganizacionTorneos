package com.example.torneos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DtoResultadoRegistroPlanilla {
    private long equipoId;
    private String nombreEquipo;
    private int jugadoresRegistrados;
}