package com.example.torneos.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DtoInvitacionEquipo {
    private Long id;
    private Long equipoId;
    private String equipoNombre;
    private Long jugadorId;
    private String jugadorNombre;
    private String jugadorCedula;
    private String estado;
    private LocalDateTime creadaEn;

    public DtoInvitacionEquipo(Long id, Long equipoId, String equipoNombre, Long jugadorId,
                               String jugadorNombre, String jugadorCedula, String estado,
                               LocalDateTime creadaEn) {
        this.id = id;
        this.equipoId = equipoId;
        this.equipoNombre = equipoNombre;
        this.jugadorId = jugadorId;
        this.jugadorNombre = jugadorNombre;
        this.jugadorCedula = jugadorCedula;
        this.estado = estado;
        this.creadaEn = creadaEn;
    }
}