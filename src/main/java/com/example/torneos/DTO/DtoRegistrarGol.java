package com.example.torneos.DTO;

import lombok.Data;

@Data
public class DtoRegistrarGol {
    private Long equipoAnotadorId;
    private Long jugadorId;
    private Long asistenciaId;
    private boolean autogol;
    private boolean penalti;
    private int minuto;
}
