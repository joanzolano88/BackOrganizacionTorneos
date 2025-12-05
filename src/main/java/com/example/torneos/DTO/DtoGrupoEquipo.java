package com.example.torneos.DTO;

import com.example.torneos.enums.FaseActual;
import lombok.Data;

@Data
public class DtoGrupoEquipo {
    private long idEquipo;
    private FaseActual faseActual;
    private int grupo;
}
