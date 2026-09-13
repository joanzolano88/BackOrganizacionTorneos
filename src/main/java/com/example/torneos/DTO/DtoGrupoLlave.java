package com.example.torneos.DTO;

import com.example.torneos.enums.FaseActual;
import lombok.Data;

@Data
public class DtoGrupoLlave {
    private long idEquipo;
    private int grupoLlave;
    private FaseActual faseTorneo;
}