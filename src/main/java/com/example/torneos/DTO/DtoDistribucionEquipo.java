package com.example.torneos.DTO;

import com.example.torneos.enums.FaseActual;
import lombok.Data;

@Data
public class DtoDistribucionEquipo {
    private long idEquipo;
    private String nombreEquipo;
    private FaseActual fase;
    private int grupo;
}